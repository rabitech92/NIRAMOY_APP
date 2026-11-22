package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.*;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.*;
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
import java.util.UUID;

/** খরচ ব্যবস্থাপনা: ক্যাটাগরি, খরচ এন্ট্রি, পেমেন্ট ট্র্যাকিং ও ড্যাশবোর্ড (শাখাভিত্তিক) */
@Service
@RequiredArgsConstructor
public class ExpenseService {
    public static final List<PaymentMethod> PAY_METHODS = List.of(
            PaymentMethod.CASH, PaymentMethod.BANK, PaymentMethod.BKASH, PaymentMethod.NAGAD,
            PaymentMethod.ROCKET, PaymentMethod.CARD);
    private static final String[] BN_MONTHS = {"জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই",
            "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"};

    private final ExpenseRepository expenses;
    private final ExpensePaymentRepository payments;
    private final ExpenseCategoryRepository categories;
    private final ExpenseSubCategoryRepository subCategories;

    public record Slice(String label, BigDecimal amount, int percent) { }

    public record Dashboard(BigDecimal today, BigDecimal month, BigDecimal pending, BigDecimal year,
                            String monthLabel, int yearNumber, List<Slice> byCategory, List<Slice> byPayment,
                            List<Expense> recent) { }

    public record ListResult(Page<Expense> page, BigDecimal totalAmount, BigDecimal totalPaid) { }

    // ---------- প্রথমবার ডিফল্ট ক্যাটাগরি ----------
    @Transactional
    public void ensureDefaults() {
        if (categories.count() > 0) return;
        List<ExpenseCategory> list = new ArrayList<>();
        ExpenseGroup g = ExpenseGroup.SHOP;
        list.add(new ExpenseCategory(g, "দোকান ভাড়া", "Shop Rent"));
        list.add(new ExpenseCategory(g, "বিদ্যুৎ বিল", "Electricity Bill"));
        list.add(new ExpenseCategory(g, "পানির বিল", "Water Bill"));
        list.add(new ExpenseCategory(g, "ইন্টারনেট বিল", "Internet Bill"));
        list.add(new ExpenseCategory(g, "গ্যাস বিল", "Gas Bill"));
        list.add(new ExpenseCategory(g, "দোকান পরিষ্কার", "Cleaning"));
        list.add(new ExpenseCategory(g, "দোকান মেরামত", "Maintenance"));
        list.add(new ExpenseCategory(g, "নিরাপত্তা খরচ", "Security"));
        g = ExpenseGroup.STAFF;
        list.add(new ExpenseCategory(g, "কর্মচারীর বেতন", "Employee Salary"));
        list.add(new ExpenseCategory(g, "অতিরিক্ত কাজের টাকা", "Overtime"));
        list.add(new ExpenseCategory(g, "বোনাস", "Employee Bonus"));
        list.add(new ExpenseCategory(g, "যাতায়াত বা অন্যান্য ভাতা", "Employee Allowance"));
        list.add(new ExpenseCategory(g, "কর্মচারীদের খাবার", "Staff Food"));
        list.add(new ExpenseCategory(g, "কর্মচারীদের চিকিৎসা", "Staff Medical"));
        g = ExpenseGroup.OPERATING;
        list.add(new ExpenseCategory(g, "বাজার", "Bazar"));
        list.add(new ExpenseCategory(g, "পরিবহন", "Transport"));
        list.add(new ExpenseCategory(g, "ডেলিভারি", "Delivery Charge"));
        list.add(new ExpenseCategory(g, "চা-নাস্তা", "Tea & Snacks"));
        list.add(new ExpenseCategory(g, "প্যাকেট ও ব্যাগ", "Packaging"));
        list.add(new ExpenseCategory(g, "কাগজ, কলম, প্রিন্ট", "Stationery"));
        list.add(new ExpenseCategory(g, "মোবাইল বিল", "Mobile Bill"));
        list.add(new ExpenseCategory(g, "কুরিয়ার", "Courier"));
        g = ExpenseGroup.BUSINESS;
        list.add(new ExpenseCategory(g, "ট্রেড লাইসেন্স", "Trade License"));
        list.add(new ExpenseCategory(g, "ড্রাগ লাইসেন্স", "Drug License"));
        list.add(new ExpenseCategory(g, "লাইসেন্স নবায়ন", "License Renewal"));
        list.add(new ExpenseCategory(g, "ব্যাংক চার্জ", "Bank Charge"));
        list.add(new ExpenseCategory(g, "পেমেন্ট চার্জ", "Payment Gateway Charge"));
        list.add(new ExpenseCategory(g, "বিজ্ঞাপন", "Marketing"));
        list.add(new ExpenseCategory(g, "সফটওয়্যার খরচ", "Software Subscription"));
        list.add(new ExpenseCategory(g, "হিসাবরক্ষণ", "Accounting Fee"));
        list.add(new ExpenseCategory(g, "অন্যান্য", "Others"));
        g = ExpenseGroup.EMERGENCY;
        list.add(new ExpenseCategory(g, "জরুরি মেরামত", "Emergency Repair"));
        list.add(new ExpenseCategory(g, "যন্ত্রপাতি ক্রয়/মেরামত", "Equipment Purchase/Repair"));
        list.add(new ExpenseCategory(g, "চুরি বা ক্ষতি", "Theft or Damage Loss"));
        list.add(new ExpenseCategory(g, "অপ্রত্যাশিত খরচ", "Unexpected Expenses"));
        list.add(new ExpenseCategory(g, "বিবিধ খরচ", "Miscellaneous Expenses"));
        categories.saveAll(list);
    }

    // ---------- ক্যাটাগরি ----------
    @Transactional(readOnly = true)
    public List<ExpenseCategory> allCategories() { return categories.findAllByOrderByGroupTypeAscNameAsc(); }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> activeCategories() { return categories.findByActiveTrueOrderByGroupTypeAscNameAsc(); }

    @Transactional(readOnly = true)
    public List<ExpenseSubCategory> allSubCategories() { return subCategories.findAllByOrderByCategoryNameAscNameAsc(); }

    @Transactional(readOnly = true)
    public List<ExpenseSubCategory> activeSubCategories() { return subCategories.findByActiveTrueOrderByNameAsc(); }

    @Transactional
    public void createCategory(Forms.CategoryForm f) {
        String name = f.getName().trim();
        if (categories.existsByNameIgnoreCase(name)) throw new BusinessException("এই নামে ক্যাটাগরি আগেই আছে।");
        ExpenseCategory c = new ExpenseCategory(f.getGroupType(), name, blankToNull(f.getNameEn()));
        categories.save(c);
    }

    @Transactional
    public void toggleCategory(Long id) {
        ExpenseCategory c = categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setActive(!c.isActive());
    }

    @Transactional
    public void createSubCategory(Forms.SubCategoryForm f) {
        ExpenseCategory c = categories.findById(f.getCategoryId()).orElseThrow(() -> new BusinessException("ক্যাটাগরি পাওয়া যায়নি।"));
        String name = f.getName().trim();
        if (subCategories.existsByCategoryIdAndNameIgnoreCase(c.getId(), name))
            throw new BusinessException("এই ক্যাটাগরিতে একই নামের উপ-ক্যাটাগরি আছে।");
        ExpenseSubCategory s = new ExpenseSubCategory();
        s.setCategory(c);
        s.setName(name);
        subCategories.save(s);
    }

    @Transactional
    public void toggleSubCategory(Long id) {
        ExpenseSubCategory s = subCategories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setActive(!s.isActive());
    }

    // ---------- খরচ ----------
    @Transactional
    public Expense create(Forms.ExpenseForm f, Branch branch, UserPrincipal user) {
        ExpenseCategory c = categories.findById(f.getCategoryId()).orElseThrow(() -> new BusinessException("ক্যাটাগরি পাওয়া যায়নি।"));
        if (!c.isActive()) throw new BusinessException("এই ক্যাটাগরি এখন বন্ধ আছে।");
        LocalDate date = f.getExpenseDate() == null ? LocalDate.now() : f.getExpenseDate();
        checkDate(date);

        Expense e = new Expense();
        e.setExpenseNo("TMP-" + UUID.randomUUID());
        e.setExpenseDate(date);
        e.setCategory(c);
        e.setSubCategory(resolveSub(c, f.getSubCategoryId()));
        e.setAmount(f.getAmount().setScale(2, RoundingMode.HALF_UP));
        e.setPaidTo(blankToNull(f.getPaidTo()));
        e.setReferenceNo(blankToNull(f.getReferenceNo()));
        e.setDescription(blankToNull(f.getDescription()));
        e.setBranch(branch);
        e.setCreatedBy(user.getFullName());

        BigDecimal paid;
        switch (f.getStatus()) {
            case PAID -> paid = e.getAmount();
            case PARTIAL -> {
                paid = f.getPaidAmount() == null ? BigDecimal.ZERO : f.getPaidAmount().setScale(2, RoundingMode.HALF_UP);
                if (paid.signum() <= 0 || paid.compareTo(e.getAmount()) >= 0)
                    throw new BusinessException("আংশিক পরিশোধের অঙ্ক ০-এর বেশি ও মোট খরচের কম হতে হবে।");
            }
            default -> paid = BigDecimal.ZERO;
        }
        if (paid.signum() > 0) addPayment(e, paid, f.getPaymentMethod(), date, e.getReferenceNo(), user.getFullName());
        e.recompute();

        expenses.saveAndFlush(e);
        e.setExpenseNo(String.format("EXP-%06d", e.getId()));
        return e;
    }

    /** অর্থসংক্রান্ত নয় এমন তথ্য ও মোট খরচ সম্পাদনা। পেমেন্ট বদলাতে "বাকি পরিশোধ" ব্যবহার করুন। */
    @Transactional
    public Expense update(Long id, Forms.ExpenseForm f, Long scopeBranchId, UserPrincipal user) {
        Expense e = get(id, scopeBranchId);
        if (e.isVoided()) throw new BusinessException("বাতিল খরচ সম্পাদনা করা যায় না।");
        ExpenseCategory c = categories.findById(f.getCategoryId()).orElseThrow(() -> new BusinessException("ক্যাটাগরি পাওয়া যায়নি।"));
        if (!c.isActive() && !c.getId().equals(e.getCategory().getId())) throw new BusinessException("এই ক্যাটাগরি এখন বন্ধ আছে।");
        LocalDate date = f.getExpenseDate() == null ? e.getExpenseDate() : f.getExpenseDate();
        checkDate(date);
        BigDecimal amount = f.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(e.getPaidAmount()) < 0)
            throw new BusinessException("মোট খরচ ইতোমধ্যে পরিশোধিত অঙ্কের (" + e.getPaidAmount() + ") কম হতে পারবে না।");

        e.setExpenseDate(date);
        e.setCategory(c);
        e.setSubCategory(resolveSub(c, f.getSubCategoryId()));
        e.setAmount(amount);
        e.setPaidTo(blankToNull(f.getPaidTo()));
        e.setReferenceNo(blankToNull(f.getReferenceNo()));
        e.setDescription(blankToNull(f.getDescription()));
        e.setUpdatedBy(user.getFullName());
        e.setUpdatedAt(LocalDateTime.now());
        e.recompute();
        return e;
    }

    @Transactional
    public void pay(Long id, Forms.ExpensePayForm f, Long scopeBranchId, UserPrincipal user) {
        Expense e = get(id, scopeBranchId);
        if (e.isVoided()) throw new BusinessException("বাতিল খরচে পেমেন্ট করা যায় না।");
        BigDecimal amount = f.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (e.getDueAmount().signum() <= 0) throw new BusinessException("এই খরচের কোনো বকেয়া নেই।");
        if (amount.compareTo(e.getDueAmount()) > 0) throw new BusinessException("পেমেন্ট বকেয়ার (" + e.getDueAmount() + ") বেশি হতে পারবে না।");
        addPayment(e, amount, f.getMethod(), LocalDate.now(), blankToNull(f.getReferenceNo()), user.getFullName());
        e.setUpdatedBy(user.getFullName());
        e.setUpdatedAt(LocalDateTime.now());
        e.recompute();
    }

    @Transactional
    public void voidExpense(Long id, String reason, Long scopeBranchId, UserPrincipal user) {
        Expense e = get(id, scopeBranchId);
        if (e.isVoided()) throw new BusinessException("খরচটি আগেই বাতিল করা হয়েছে।");
        if (reason == null || reason.isBlank()) throw new BusinessException("বাতিলের কারণ লিখুন।");
        e.setVoided(true);
        e.setVoidReason(reason.trim());
        e.setVoidedBy(user.getFullName());
        e.setVoidedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Expense get(Long id, Long scopeBranchId) {
        Expense e = expenses.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (scopeBranchId != null && !scopeBranchId.equals(e.getBranch().getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return e;
    }

    // ---------- তালিকা ও ড্যাশবোর্ড ----------
    @Transactional(readOnly = true)
    public ListResult search(Long branchId, String q, Long categoryId, String status, LocalDate from, LocalDate to,
                             boolean includeVoided, int page) {
        ExpenseStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = ExpenseStatus.valueOf(status); } catch (IllegalArgumentException ignored) { }
        }
        String query = q == null ? "" : q.trim();
        Page<Expense> p = expenses.search(branchId, from, to, categoryId, st, includeVoided, query,
                PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "expenseDate").and(Sort.by(Sort.Direction.DESC, "id"))));
        List<Object[]> t = expenses.totals(branchId, from, to, categoryId, st, query);
        BigDecimal amount = BigDecimal.ZERO, paid = BigDecimal.ZERO;
        if (!t.isEmpty() && t.get(0)[0] != null) {
            amount = (BigDecimal) t.get(0)[0];
            paid = (BigDecimal) t.get(0)[1];
        }
        return new ListResult(p, amount, paid);
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(today.getYear(), 12, 31);

        List<Slice> byCategory = topSlices(expenses.sumByCategory(branchId, monthStart, monthEnd), 5, false);
        List<Slice> byPayment = topSlices(payments.sumByMethod(branchId, monthStart, monthEnd), 10, true);

        return new Dashboard(
                nz(expenses.sumAmount(branchId, today, today)),
                nz(expenses.sumAmount(branchId, monthStart, monthEnd)),
                nz(expenses.sumPending(branchId)),
                nz(expenses.sumAmount(branchId, yearStart, yearEnd)),
                BN_MONTHS[today.getMonthValue() - 1] + " " + today.getYear(), today.getYear(),
                byCategory, byPayment, expenses.recent(branchId, PageRequest.of(0, 8)));
    }

    /** [নাম বা PaymentMethod, মোট] সারি থেকে শতাংশসহ স্লাইস; limit-এর পরের সব "অন্যান্য"-তে */
    private List<Slice> topSlices(List<Object[]> rows, int limit, boolean paymentMethod) {
        BigDecimal total = rows.stream().map(r -> (BigDecimal) r[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Slice> out = new ArrayList<>();
        BigDecimal others = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            BigDecimal amount = (BigDecimal) rows.get(i)[1];
            if (i < limit) {
                String label = paymentMethod ? ((PaymentMethod) rows.get(i)[0]).getLabel() : String.valueOf(rows.get(i)[0]);
                out.add(new Slice(label, amount, percent(amount, total)));
            } else {
                others = others.add(amount);
            }
        }
        if (others.signum() > 0) out.add(new Slice("অন্যান্য", others, percent(others, total)));
        return out;
    }

    // ---------- helpers ----------
    private void addPayment(Expense e, BigDecimal amount, PaymentMethod method, LocalDate paidOn, String ref, String by) {
        if (method == null || !PAY_METHODS.contains(method)) throw new BusinessException("পেমেন্ট পদ্ধতি নির্বাচন করুন।");
        ExpensePayment p = new ExpensePayment();
        p.setAmount(amount);
        p.setMethod(method);
        p.setPaidOn(paidOn);
        p.setReferenceNo(ref);
        p.setPaidBy(by);
        e.addPayment(p);
        e.setPaidAmount(e.getPaidAmount().add(amount));
    }

    private ExpenseSubCategory resolveSub(ExpenseCategory c, Long subId) {
        if (subId == null) return null;
        ExpenseSubCategory s = subCategories.findById(subId).orElseThrow(() -> new BusinessException("উপ-ক্যাটাগরি পাওয়া যায়নি।"));
        if (!s.getCategory().getId().equals(c.getId())) throw new BusinessException("উপ-ক্যাটাগরি নির্বাচিত ক্যাটাগরির নয়।");
        return s;
    }

    private static void checkDate(LocalDate d) {
        if (d.isAfter(LocalDate.now())) throw new BusinessException("ভবিষ্যতের তারিখে খরচ এন্ট্রি করা যায় না।");
        if (d.isBefore(LocalDate.now().minusYears(2))) throw new BusinessException("তারিখ অতিরিক্ত পুরনো।");
    }

    private static int percent(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? 0 : part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
