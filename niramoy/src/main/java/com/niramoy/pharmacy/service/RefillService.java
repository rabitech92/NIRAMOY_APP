package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.*;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.CustomerSubscriptionRepository;
import com.niramoy.pharmacy.repository.MedicineRepository;
import com.niramoy.pharmacy.security.UserPrincipal;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** গ্রাহকের নিয়মিত ওষুধ সাবস্ক্রিপশন (রিফিল): তালিকা, রিমাইন্ডার, এক ক্লিকে বিল */
@Service
@RequiredArgsConstructor
public class RefillService {
    public static final List<String> UNITS = List.of("পিস", "পাতা");
    private static final LocalDate FAR = LocalDate.of(2999, 12, 31);

    private final CustomerSubscriptionRepository repo;
    private final MedicineRepository medicines;
    private final SaleService sales;

    // ---------- queries ----------
    /** filter: "" (সক্রিয় সব) | DUE (আজ/দেরি) | WEEK (৭ দিনের মধ্যে) | PAUSED | CANCELLED | ALL */
    @Transactional(readOnly = true)
    public Page<CustomerSubscription> search(Long branchId, String q, String filter, int page) {
        RefillStatus st = RefillStatus.ACTIVE;
        LocalDate until = FAR;
        switch (filter == null ? "" : filter) {
            case "DUE" -> until = LocalDate.now();
            case "WEEK" -> until = LocalDate.now().plusDays(7);
            case "PAUSED" -> st = RefillStatus.PAUSED;
            case "CANCELLED" -> st = RefillStatus.CANCELLED;
            case "ALL" -> st = null;
            default -> { }
        }
        return repo.search(branchId, st, q == null ? "" : q.trim(), until,
                PageRequest.of(Math.max(page, 0), 10, Sort.by("nextRefillDate").and(Sort.by("id"))));
    }

    @Transactional(readOnly = true)
    public CustomerSubscription get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public long dueCount(Long branchId) {
        return repo.countDue(branchId, RefillStatus.ACTIVE, LocalDate.now());
    }

    // ---------- commands ----------
    @Transactional
    public CustomerSubscription create(Forms.RefillForm f, Branch branch, UserPrincipal user) {
        List<CustomerSubscriptionItem> items = new ArrayList<>();
        for (Forms.RefillItemRow r : f.getItems()) {
            if (r == null || ((r.getMedicineName() == null || r.getMedicineName().isBlank()) && r.getQuantity() == null)) continue;
            if (r.getMedicineName() == null || r.getMedicineName().isBlank())
                throw new BusinessException("প্রতিটি সারিতে ওষুধের নাম দিন।");
            if (r.getQuantity() == null || r.getQuantity() < 1)
                throw new BusinessException("'" + r.getMedicineName().trim() + "'-এর পরিমাণ কমপক্ষে ১ দিন।");
            CustomerSubscriptionItem i = new CustomerSubscriptionItem();
            i.setMedicineName(r.getMedicineName().trim());
            i.setQuantity(r.getQuantity());
            i.setUnit(UNITS.contains(r.getUnit()) ? r.getUnit() : "পিস");
            items.add(i);
        }
        if (items.isEmpty()) throw new BusinessException("কমপক্ষে একটি ওষুধ যোগ করুন।");

        CustomerSubscription s = new CustomerSubscription();
        s.setBranch(branch);
        s.setCustomerName(f.getCustomerName().trim());
        s.setCustomerPhone(PhoneUtil.normalize(f.getCustomerPhone()));
        s.setPlanName(f.getPlanName() == null || f.getPlanName().isBlank() ? "নিয়মিত ওষুধ" : f.getPlanName().trim());
        s.setFrequencyDays(f.getFrequencyDays());
        LocalDate start = f.getStartDate() == null ? LocalDate.now() : f.getStartDate();
        s.setStartDate(start);
        s.setNextRefillDate(start);
        s.setDiscountPercent(f.getDiscountPercent().setScale(2, RoundingMode.HALF_UP));
        s.setNote(f.getNote() == null || f.getNote().isBlank() ? null : f.getNote().trim());
        s.setCreatedBy(user.getFullName());
        items.forEach(s::addItem);
        return repo.save(s);
    }

    /**
     * রিফিলের বিল: সাবস্ক্রিপশনের ওষুধগুলো এই শাখার স্টক থেকে (আগে মেয়াদ শেষ হবে এমন ব্যাচ) খুঁজে
     * সাধারণ বিক্রয় হিসেবে বিল তৈরি করে, ছাড় প্রয়োগ করে এবং পরের রিফিলের তারিখ এগিয়ে দেয়।
     */
    @Transactional
    public Sale bill(Long id, PaymentMethod method, UserPrincipal user, Branch branch) {
        CustomerSubscription s = get(id);
        if (s.getStatus() != RefillStatus.ACTIVE) throw new BusinessException("শুধু সক্রিয় সাবস্ক্রিপশনের বিল করা যায়।");
        if (!s.getBranch().getId().equals(branch.getId())) throw new BusinessException("এই সাবস্ক্রিপশন অন্য শাখার।");
        if (method == null || method == PaymentMethod.CREDIT) throw new BusinessException("পেমেন্ট পদ্ধতি নির্বাচন করুন।");

        List<CheckoutRequest.Line> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CustomerSubscriptionItem i : s.getItems()) {
            Medicine m = resolve(branch.getId(), i);
            boolean strip = "পাতা".equals(i.getUnit());
            int strips = strip ? i.getQuantity() : 0;
            int pieces = strip ? 0 : i.getQuantity();
            lines.add(new CheckoutRequest.Line(m.getId(), strips, pieces));
            subtotal = subtotal
                    .add(m.getEffectiveStripPrice().multiply(BigDecimal.valueOf(strips)))
                    .add(m.getSalePrice().multiply(BigDecimal.valueOf(pieces)));
        }
        BigDecimal discount = subtotal.multiply(s.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        CheckoutRequest req = new CheckoutRequest(lines, discount, method, null, s.getCustomerName(), s.getCustomerPhone());
        Sale sale = sales.checkout(req, user, branch);

        // checkout-এর পর persistence context পরিষ্কার হয়, তাই আবার লোড করে হালনাগাদ
        CustomerSubscription managed = get(id);
        LocalDate base = managed.getNextRefillDate().isBefore(LocalDate.now()) ? LocalDate.now() : managed.getNextRefillDate();
        managed.setNextRefillDate(base.plusDays(managed.getFrequencyDays()));
        managed.setCycles(managed.getCycles() + 1);
        managed.setLastBilledAt(LocalDateTime.now());
        managed.setLastSaleId(sale.getId());
        return sale;
    }

    @Transactional
    public void pause(Long id) { setStatus(id, RefillStatus.ACTIVE, RefillStatus.PAUSED, "শুধু সক্রিয় সাবস্ক্রিপশন বিরতিতে রাখা যায়।"); }

    @Transactional
    public void resume(Long id) { setStatus(id, RefillStatus.PAUSED, RefillStatus.ACTIVE, "শুধু বিরতিতে থাকা সাবস্ক্রিপশন চালু করা যায়।"); }

    @Transactional
    public void cancel(Long id) {
        CustomerSubscription s = get(id);
        if (s.getStatus() == RefillStatus.CANCELLED) throw new BusinessException("সাবস্ক্রিপশনটি আগেই বাতিল।");
        s.setStatus(RefillStatus.CANCELLED);
    }

    @Transactional
    public void changeNextDate(Long id, LocalDate date) {
        if (date == null) throw new BusinessException("তারিখ দিন।");
        CustomerSubscription s = get(id);
        if (s.getStatus() == RefillStatus.CANCELLED) throw new BusinessException("বাতিল সাবস্ক্রিপশনের তারিখ বদলানো যায় না।");
        s.setNextRefillDate(date);
    }

    // ---------- helpers ----------
    private void setStatus(Long id, RefillStatus from, RefillStatus to, String error) {
        CustomerSubscription s = get(id);
        if (s.getStatus() != from) throw new BusinessException(error);
        s.setStatus(to);
    }

    /** নামে মিলিয়ে (হুবহু নামকে অগ্রাধিকার) পর্যাপ্ত স্টক আছে এমন প্রথম ব্যাচ (FEFO) বেছে নেয় */
    private Medicine resolve(Long branchId, CustomerSubscriptionItem item) {
        String name = item.getMedicineName().trim();
        List<Medicine> found = medicines.searchForSale(branchId, name, LocalDate.now(), PageRequest.of(0, 30));
        for (Medicine m : found) {
            if (m.getName().equalsIgnoreCase(name) && m.getStock() >= need(m, item)) return m;
        }
        for (Medicine m : found) {
            if (m.getStock() >= need(m, item)) return m;
        }
        throw new BusinessException("'" + name + "'-এর পর্যাপ্ত স্টক পাওয়া যায়নি; ইনভেন্টরি দেখুন বা POS থেকে বিক্রয় করুন।");
    }

    private static long need(Medicine m, CustomerSubscriptionItem i) {
        return "পাতা".equals(i.getUnit()) ? (long) i.getQuantity() * Math.max(m.getStripSize(), 1) : i.getQuantity();
    }
}
