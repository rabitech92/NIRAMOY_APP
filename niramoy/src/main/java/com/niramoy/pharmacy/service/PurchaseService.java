package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.*;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.*;
import com.niramoy.pharmacy.controller.Forms;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    public static final List<String> UNITS = List.of("পিস", "পাতা", "বক্স", "বোতল", "ভায়াল");

    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository orders;
    private final PurchaseReturnRepository purchaseReturns;
    private final SupplierPaymentRepository payments;
    private final MedicineRepository medicines;
    private final PurchaseEntryRepository entries;

    // ---------- queries ----------
    @Transactional(readOnly = true)
    public Page<PurchaseOrder> orders(Long branchId, String q, String status, int page) {
        OrderStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = OrderStatus.valueOf(status); } catch (IllegalArgumentException ignored) { }
        }
        return orders.search(branchId, st, norm(q), PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Transactional(readOnly = true)
    public Page<Supplier> suppliers(String q, String filter, int page) {
        String f = "DUE".equals(filter) || "PAID".equals(filter) ? filter : "ALL";
        return suppliers.search(norm(q), f, PageRequest.of(Math.max(page, 0), 10, Sort.by("name")));
    }

    @Transactional(readOnly = true)
    public Page<PurchaseReturn> returns(Long branchId, String q, int page) {
        return purchaseReturns.search(branchId, norm(q), PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "id")));
    }

    @Transactional(readOnly = true)
    public List<Supplier> allSuppliers() { return suppliers.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public PurchaseOrder getOrder(Long id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public PurchaseOrderItem getItem(PurchaseOrder o, Long itemId) {
        return o.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public BigDecimal totalSupplierDue() {
        BigDecimal v = suppliers.totalDue();
        return v == null ? BigDecimal.ZERO : v;
    }

    @Transactional(readOnly = true)
    public List<SupplierPayment> recentPayments() { return payments.findTop10ByOrderByIdDesc(); }

    // ---------- suppliers ----------
    @Transactional
    public Supplier createSupplier(Forms.SupplierForm f) {
        return suppliers.save(new Supplier(f.getName().trim(), f.getPhone().trim(), BigDecimal.ZERO));
    }

    // ---------- order (ক্রয় তালিকা) ----------
    @Transactional
    public PurchaseOrder createOrder(Forms.OrderForm f, Branch branch) {
        Supplier s = suppliers.findById(f.getSupplierId())
                .orElseThrow(() -> new BusinessException("সরবরাহকারী পাওয়া যায়নি।"));
        List<PurchaseOrderItem> items = new ArrayList<>();
        for (Forms.ItemRow r : f.getItems()) {
            if (isEmptyRow(r)) continue;
            items.add(toItem(r));
        }
        if (items.isEmpty()) throw new BusinessException("কমপক্ষে একটি ওষুধ যোগ করুন (ব্র্যান্ড নাম ও পরিমাণ আবশ্যক)।");

        PurchaseOrder o = new PurchaseOrder();
        o.setOrderNo("TMP-" + UUID.randomUUID());
        o.setSupplier(s);
        o.setBranch(branch);
        o.setMpoName(blankToNull(f.getMpoName()));
        o.setAmount(BigDecimal.ZERO);
        items.forEach(o::addItem);
        o.setItemsSummary(summary(items));
        orders.saveAndFlush(o);
        o.setOrderNo("PO-" + (1000 + o.getId()));
        return o;
    }

    @Transactional
    public void addItem(Long orderId, Forms.ItemRow row) {
        PurchaseOrder o = getOrder(orderId);
        if (o.getStatus() != OrderStatus.PENDING) throw new BusinessException("সম্পন্ন আদেশে আইটেম যোগ করা যায় না।");
        if (isEmptyRow(row)) throw new BusinessException("ব্র্যান্ড নাম ও পরিমাণ দিন।");
        // পুরনো ধরনের (আইটেমবিহীন) আদেশে প্রথম আইটেম যোগ করলে পুরনো পরিমাণ শূন্য করা হয়
        if (o.getItems().isEmpty()) o.setAmount(BigDecimal.ZERO);
        o.addItem(toItem(row));
        o.setItemsSummary(summary(o.getItems()));
    }

    /** এমপিও যেসব আইটেমের অর্ডার নিয়েছেন সেগুলো "অর্ডার সম্পন্ন" চিহ্নিত করে; বাকিগুলো "অর্ডার হয়নি" থাকে */
    @Transactional
    public int markOrdered(Long orderId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) throw new BusinessException("অন্তত একটি আইটেম নির্বাচন করুন।");
        PurchaseOrder o = getOrder(orderId);
        int n = 0;
        for (PurchaseOrderItem i : o.getItems()) {
            if (itemIds.contains(i.getId()) && i.getStatus() == ItemStatus.NOT_ORDERED) {
                i.setStatus(ItemStatus.ORDERED);
                i.setOrderedAt(LocalDateTime.now());
                n++;
            }
        }
        if (n == 0) throw new BusinessException("নির্বাচিত আইটেমগুলো আগেই অর্ডার সম্পন্ন বা গ্রহণ করা হয়েছে।");
        return n;
    }

    @Transactional
    public void unmarkOrdered(Long orderId, Long itemId) {
        PurchaseOrderItem i = getItem(getOrder(orderId), itemId);
        if (i.getStatus() != ItemStatus.ORDERED) throw new BusinessException("শুধু 'অর্ডার সম্পন্ন' আইটেমের অর্ডার বাতিল করা যায়।");
        i.setStatus(ItemStatus.NOT_ORDERED);
        i.setOrderedAt(null);
    }

    @Transactional
    public void removeItem(Long orderId, Long itemId) {
        PurchaseOrder o = getOrder(orderId);
        PurchaseOrderItem i = getItem(o, itemId);
        if (i.getStatus() == ItemStatus.RECEIVED) throw new BusinessException("গ্রহণ করা আইটেম মোছা যায় না।");
        if (o.getItems().size() <= 1) throw new BusinessException("আদেশে কমপক্ষে একটি আইটেম থাকতে হবে।");
        o.getItems().remove(i);
        o.setItemsSummary(summary(o.getItems()));
        refreshStatus(o);
    }

    /**
     * সরবরাহকারীর পণ্য এলে ফার্মাসিস্ট আইটেমটি গ্রহণ করেন: স্টকে ব্যাচ যোগ হয়,
     * সরবরাহকারীর বকেয়া (ক্রয়মূল্য × পিস) বাড়ে, আইটেম "পণ্য গ্রহণ সম্পন্ন" হয়।
     */
    @Transactional
    public void receiveItem(Long orderId, Long itemId, Forms.ReceiveForm f) {
        PurchaseOrder o = getOrder(orderId);
        PurchaseOrderItem item = getItem(o, itemId);
        if (item.getStatus() != ItemStatus.ORDERED)
            throw new BusinessException("শুধু 'অর্ডার সম্পন্ন' আইটেমের পণ্য গ্রহণ করা যায়।");
        if (f.getSalePrice().compareTo(f.getCostPrice()) < 0)
            throw new BusinessException("বিক্রয়মূল্য ক্রয়মূল্যের চেয়ে কম হতে পারবে না।");

        long total = (long) f.getQuantityStrips() * f.getStripSize() + f.getQuantityPieces();
        if (total <= 0) throw new BusinessException("প্রাপ্ত পাতা বা পিস কমপক্ষে একটি দিন।");
        if (total > 10_000_000L) throw new BusinessException("পরিমাণ অতিরিক্ত বেশি।");

        Long branchId = o.getBranch().getId();
        var existing = medicines.findByBranchIdAndBarcodeAndBatchNo(branchId, f.getBarcode().trim(), f.getBatchNo().trim());
        if (existing.isPresent()) {
            medicines.incrementStock(existing.get().getId(), (int) total);   // persistence context পরিষ্কার হয়
        } else {
            Medicine m = new Medicine();
            m.setName(f.getName().trim());
            m.setGenericName(blankToNull(f.getGenericName()));
            m.setCompanyName(blankToNull(f.getCompanyName()));
            m.setCategory(blankToNull(f.getCategory()));
            m.setBarcode(f.getBarcode().trim());
            m.setBatchNo(f.getBatchNo().trim());
            m.setExpiryDate(f.getExpiryDate());
            m.setCostPrice(f.getCostPrice());
            m.setSalePrice(f.getSalePrice());
            m.setStock((int) total);
            m.setReorderLevel(f.getReorderLevel());
            m.setStripSize(f.getStripSize());
            m.setStripPrice(f.getStripPrice());
            m.setBoxNo(f.getBoxNo().trim());
            m.setBranch(o.getBranch());
            medicines.save(m);
        }

        // incrementStock-এর পর context পরিষ্কার হতে পারে, তাই আবার লোড করা হলো
        PurchaseOrder managed = getOrder(orderId);
        PurchaseOrderItem mi = getItem(managed, itemId);
        BigDecimal cost = f.getCostPrice().multiply(BigDecimal.valueOf(total));
        Supplier s = suppliers.findById(managed.getSupplier().getId()).orElseThrow();
        s.setDueAmount(s.getDueAmount().add(cost));
        managed.setAmount(managed.getAmount().add(cost));
        mi.setStatus(ItemStatus.RECEIVED);
        mi.setReceivedAt(LocalDateTime.now());
        refreshStatus(managed);

        // ক্রয়ের খাতায় এন্ট্রি (মাসিক/বার্ষিক ক্রয়ের হিসাবের জন্য)
        PurchaseEntry pe = new PurchaseEntry();
        pe.setBranch(managed.getBranch());
        pe.setEntryDate(LocalDate.now());
        pe.setMedicineName(f.getName().trim());
        pe.setBatchNo(f.getBatchNo().trim());
        pe.setQuantity(total);
        pe.setUnitCost(f.getCostPrice());
        pe.setTotalCost(cost);
        pe.setSupplierName(managed.getSupplier().getName());
        pe.setReference(managed.getOrderNo());
        pe.setSource(PurchaseSource.ORDER);
        pe.setCreatedBy(com.niramoy.pharmacy.security.UserPrincipal.current() == null ? null
                : com.niramoy.pharmacy.security.UserPrincipal.current().getFullName());
        entries.save(pe);
    }

    @Transactional
    public void returnToSupplier(Long orderId, Forms.PurchaseReturnForm f) {
        PurchaseOrder o = getOrder(orderId);
        if (o.getAmount().signum() <= 0) throw new BusinessException("এই আদেশের কোনো পণ্য এখনো গ্রহণ করা হয়নি।");
        if (f.getAmount().compareTo(o.getAmount()) > 0) throw new BusinessException("ফেরতের মূল্য গ্রহণকৃত পণ্যের মূল্যের বেশি হতে পারবে না।");
        PurchaseReturn r = new PurchaseReturn();
        r.setOrder(o); r.setAmount(f.getAmount()); r.setReason(f.getReason().trim());
        purchaseReturns.save(r);
        Supplier s = o.getSupplier();
        s.setDueAmount(s.getDueAmount().subtract(f.getAmount()).max(BigDecimal.ZERO));
    }

    @Transactional
    public void paySupplier(Long supplierId, Forms.AmountForm f, String paidBy) {
        Supplier s = suppliers.findById(supplierId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (f.getMethod() == PaymentMethod.CREDIT) throw new BusinessException("পেমেন্ট পদ্ধতি বকেয়া হতে পারে না।");
        if (f.getAmount().compareTo(s.getDueAmount()) > 0) throw new BusinessException("পেমেন্ট বকেয়ার বেশি হতে পারবে না।");
        s.setDueAmount(s.getDueAmount().subtract(f.getAmount()));
        SupplierPayment p = new SupplierPayment();
        p.setSupplier(s); p.setAmount(f.getAmount()); p.setMethod(f.getMethod()); p.setPaidBy(paidBy);
        payments.save(p);
    }

    // ---------- helpers ----------
    private void refreshStatus(PurchaseOrder o) {
        boolean all = !o.getItems().isEmpty() && o.getItems().stream().allMatch(i -> i.getStatus() == ItemStatus.RECEIVED);
        if (all && o.getStatus() != OrderStatus.RECEIVED) {
            o.setStatus(OrderStatus.RECEIVED);
            o.setReceivedAt(LocalDateTime.now());
        }
    }

    private static boolean isEmptyRow(Forms.ItemRow r) {
        return r == null || (isBlank(r.getBrandName()) && isBlank(r.getGenericName())
                && isBlank(r.getCompanyName()) && r.getQuantity() == null);
    }

    private static PurchaseOrderItem toItem(Forms.ItemRow r) {
        if (isBlank(r.getBrandName())) throw new BusinessException("প্রতিটি সারিতে ব্র্যান্ড নাম দিন।");
        if (r.getQuantity() == null || r.getQuantity() < 1)
            throw new BusinessException("'" + r.getBrandName().trim() + "'-এর পরিমাণ কমপক্ষে ১ দিন।");
        if (r.getQuantity() > 1_000_000) throw new BusinessException("পরিমাণ অতিরিক্ত বেশি।");
        if (r.getBrandName().trim().length() > 150 || (r.getGenericName() != null && r.getGenericName().trim().length() > 150)
                || (r.getCompanyName() != null && r.getCompanyName().trim().length() > 150))
            throw new BusinessException("নাম সর্বোচ্চ ১৫০ অক্ষরের হতে পারবে।");
        PurchaseOrderItem i = new PurchaseOrderItem();
        i.setBrandName(r.getBrandName().trim());
        i.setGenericName(blankToNull(r.getGenericName()));
        i.setCompanyName(blankToNull(r.getCompanyName()));
        i.setQuantity(r.getQuantity());
        i.setUnit(UNITS.contains(r.getUnit()) ? r.getUnit() : "পিস");
        i.setPriority(r.getPriority() == null ? Priority.NORMAL : r.getPriority());
        return i;
    }

    private static String summary(List<PurchaseOrderItem> items) {
        String s = String.join(", ", items.stream().limit(4).map(PurchaseOrderItem::getBrandName).toList());
        if (items.size() > 4) s += " ... (মোট " + items.size() + "টি)";
        return s.length() > 255 ? s.substring(0, 252) + "..." : s;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static String norm(String q) { return q == null ? "" : q.trim(); }
}
