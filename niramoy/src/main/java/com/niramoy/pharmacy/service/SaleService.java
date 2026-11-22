package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.*;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.*;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.controller.Forms;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SaleService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SaleRepository sales;
    private final SaleReturnRepository saleReturns;
    private final DueCollectionRepository collections;
    private final MedicineRepository medicines;
    private final AppUserRepository users;
    private final SettingService settings;

    // ---------- queries ----------
    @Transactional(readOnly = true)
    public Page<Sale> search(Long branchId, String q, String status, int page) {
        SaleStatus st = parseStatus(status);
        Pageable p = PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "id"));
        return sales.search(branchId, st, q == null ? "" : q.trim(), p);
    }

    @Transactional(readOnly = true)
    public Sale getWithItems(Long id) {
        return sales.findWithItemsById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<SaleReturn> recentReturns(Long branchId) {
        return saleReturns.recent(branchId, PageRequest.of(0, 8));
    }

    @Transactional(readOnly = true)
    public List<Sale> openDues(Long branchId) { return sales.findOpenDues(branchId); }

    // ---------- checkout ----------
    @Transactional
    public Sale checkout(CheckoutRequest req, UserPrincipal user, Branch branch) {
        if (req.items() == null || req.items().isEmpty()) throw new BusinessException("কার্টে কোনো ওষুধ নেই।");
        if (req.paymentMethod() == null) throw new BusinessException("পেমেন্ট পদ্ধতি নির্বাচন করুন।");

        // medicineId -> [পাতা, পিস]
        Map<Long, int[]> wanted = new LinkedHashMap<>();
        for (CheckoutRequest.Line l : req.items()) {
            if (l.medicineId() == null || l.strips() < 0 || l.pieces() < 0 || l.strips() + l.pieces() == 0)
                throw new BusinessException("অবৈধ পরিমাণ।");
            int[] q = wanted.computeIfAbsent(l.medicineId(), k -> new int[2]);
            q[0] += l.strips();
            q[1] += l.pieces();
        }

        Sale sale = new Sale();
        sale.setBranch(branch);
        sale.setCashier(users.getReferenceById(user.getId()));
        sale.setPaymentMethod(req.paymentMethod());
        sale.setInvoiceNo("TMP-" + UUID.randomUUID());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, int[]> e : wanted.entrySet()) {
            Medicine m = medicines.findById(e.getKey())
                    .orElseThrow(() -> new BusinessException("ওষুধ পাওয়া যায়নি।"));
            if (!m.getBranch().getId().equals(branch.getId()))
                throw new BusinessException("'" + m.getName() + "' এই শাখার স্টকে নেই।");
            if (m.isExpired())
                throw new BusinessException("'" + m.getName() + "' (" + m.getBatchNo() + ") মেয়াদোত্তীর্ণ — বিক্রয় করা যাবে না।");

            int pack = Math.max(m.getStripSize(), 1);
            int strips = e.getValue()[0], pieces = e.getValue()[1];
            long totalPieces = (long) strips * pack + pieces;
            if (totalPieces > m.getStock() || medicines.decrementStock(m.getId(), (int) totalPieces) == 0)
                throw new BusinessException("'" + m.getName() + "'-এর পর্যাপ্ত স্টক নেই।");

            if (pieces > 0) {
                SaleItem item = newItem(m, pieces, 1, "পিস", m.getSalePrice(), m.getCostPrice());
                sale.addItem(item);
                subtotal = subtotal.add(item.getLineTotal());
            }
            if (strips > 0) {
                SaleItem item = newItem(m, strips, pack, "পাতা", m.getEffectiveStripPrice(), m.getStripCost());
                sale.addItem(item);
                subtotal = subtotal.add(item.getLineTotal());
            }
        }

        BigDecimal discount = req.discount() == null ? BigDecimal.ZERO : req.discount();
        if (discount.signum() < 0 || discount.compareTo(subtotal) > 0)
            throw new BusinessException("ছাড় ০ থেকে উপমোটের মধ্যে হতে হবে।");

        BigDecimal vatPercent = settings.vat();
        BigDecimal net = subtotal.subtract(discount);
        BigDecimal vatAmount = net.multiply(vatPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal total = net.add(vatAmount).setScale(2, RoundingMode.HALF_UP);

        sale.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        sale.setDiscount(discount.setScale(2, RoundingMode.HALF_UP));
        sale.setVatPercent(vatPercent);
        sale.setVatAmount(vatAmount);
        sale.setTotal(total);

        // আংশিক পরিশোধ: paidAmount না দিলে পূর্ণ পরিশোধ; CREDIT হলে কিছুই পরিশোধ হয়নি
        BigDecimal paid;
        if (req.paymentMethod() == PaymentMethod.CREDIT) paid = BigDecimal.ZERO;
        else if (req.paidAmount() == null) paid = total;
        else paid = req.paidAmount().setScale(2, RoundingMode.HALF_UP);
        if (paid.signum() < 0 || paid.compareTo(total) > 0)
            throw new BusinessException("পরিশোধের পরিমাণ ০ থেকে মোট বিলের মধ্যে হতে হবে।");
        BigDecimal due = total.subtract(paid);

        // প্রতিটি বিলে গ্রাহকের নাম ও মোবাইল নম্বর আবশ্যক
        if (req.customerName() == null || req.customerName().isBlank())
            throw new BusinessException("গ্রাহকের নাম আবশ্যক।");
        String phone = normalizePhone(req.customerPhone());

        if (due.signum() > 0) {
            sale.setDueAmount(due);
            if (paid.signum() == 0) sale.setPaymentMethod(PaymentMethod.CREDIT);
        }
        sale.setCustomerName(req.customerName().trim());
        sale.setCustomerPhone(phone);

        sales.saveAndFlush(sale);
        sale.setInvoiceNo(String.format("%s-%06d", settings.invoicePrefix(), sale.getId()));
        return sale;
    }

    // ---------- return ----------
    @Transactional
    public Sale processReturn(Long saleId, Forms.ReturnForm f, UserPrincipal user) {
        Sale sale = sales.findWithItemsById(saleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (sale.getStatus() != SaleStatus.COMPLETED)
            throw new BusinessException("এই ইনভয়েস আগেই ফেরত দেওয়া হয়েছে।");

        if (f.isRestoreStock()) {
            for (SaleItem i : sale.getItems()) medicines.incrementStock(i.getMedicineId(), i.getQuantity() * i.getPackSize());
        }
        // ক্রেডিট বিক্রয়ে যতটুকু আদায় হয়েছে ততটুকুই ফেরতযোগ্য
        BigDecimal refund = sale.getTotal().subtract(sale.getDueAmount());

        // clearAutomatically-এর কারণে sale ডিটাচ হতে পারে, তাই আবার লোড করা হলো
        Sale managed = sales.findById(saleId).orElseThrow();
        managed.setStatus(SaleStatus.RETURNED);
        managed.setDueAmount(BigDecimal.ZERO);

        SaleReturn r = new SaleReturn();
        r.setSale(managed);
        r.setRefundAmount(refund);
        r.setRefundMethod(f.getMethod());
        r.setReason(f.getReason().trim());
        r.setStockRestored(f.isRestoreStock());
        r.setProcessedBy(user.getFullName());
        saleReturns.save(r);
        return managed;
    }

    // ---------- customer due collection ----------
    @Transactional
    public void collectDue(Long saleId, BigDecimal amount, PaymentMethod method, UserPrincipal user) {
        Sale sale = sales.findById(saleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (amount == null || amount.signum() <= 0) throw new BusinessException("পরিমাণ শূন্যের বেশি দিন।");
        if (method == PaymentMethod.CREDIT) throw new BusinessException("আদায়ের পদ্ধতি বকেয়া হতে পারে না।");
        if (amount.compareTo(sale.getDueAmount()) > 0) throw new BusinessException("আদায় বকেয়ার বেশি হতে পারবে না।");
        sale.setDueAmount(sale.getDueAmount().subtract(amount));
        DueCollection c = new DueCollection();
        c.setSale(sale); c.setAmount(amount); c.setMethod(method); c.setCollectedBy(user.getFullName());
        collections.save(c);
    }

    @Transactional(readOnly = true)
    public BigDecimal collectedToday(Long branchId) {
        LocalDate d = LocalDate.now();
        BigDecimal v = collections.sumBetween(branchId, d.atStartOfDay(), d.plusDays(1).atStartOfDay());
        return v == null ? BigDecimal.ZERO : v;
    }

    private static final Pattern BD_MOBILE = Pattern.compile("^01[3-9][0-9]{8}$");

    /** বাংলা অঙ্ক, +88 / 88 কোড ও ফাঁকা বাদ দিয়ে 01XXXXXXXXX আকারে আনে */
    private static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) throw new BusinessException("গ্রাহকের মোবাইল নম্বর আবশ্যক।");
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (c >= '০' && c <= '৯') sb.append((char) ('0' + (c - '০')));
            else if (!Character.isWhitespace(c) && c != '-' && c != '(' && c != ')') sb.append(c);
        }
        String d = sb.toString();
        if (d.startsWith("+88")) d = d.substring(3);
        else if (d.startsWith("88") && d.length() == 13) d = d.substring(2);
        if (!BD_MOBILE.matcher(d).matches())
            throw new BusinessException("সঠিক মোবাইল নম্বর দিন (যেমন: 01712345678)।");
        return d;
    }

    private static SaleItem newItem(Medicine m, int qty, int pack, String label, BigDecimal unitPrice, BigDecimal unitCost) {
        SaleItem item = new SaleItem();
        item.setMedicineId(m.getId());
        item.setMedicineName(m.getName());
        item.setBatchNo(m.getBatchNo());
        item.setQuantity(qty);
        item.setPackSize(pack);
        item.setUnitLabel(label);
        item.setUnitPrice(unitPrice);
        item.setCostPrice(unitCost);
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(qty)));
        return item;
    }

    private static SaleStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return SaleStatus.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
