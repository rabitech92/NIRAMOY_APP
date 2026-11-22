package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.*;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.*;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.controller.Forms;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * সফটওয়্যার সাবস্ক্রিপশন: ফ্রি ট্রায়াল → মালিক প্ল্যান বেছে অনুরোধ পাঠান (পেমেন্টের তথ্যসহ)
 * → সুপার অ্যাডমিন যাচাই করে সক্রিয় করেন। মেয়াদ + গ্রেস পিরিয়ড শেষ হলে সফটওয়্যার ব্লক হয়।
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    public static final int GRACE_DAYS = 3;
    public static final int WARN_DAYS = 7;
    private static final long CACHE_MS = 30_000;
    public static final List<PaymentMethod> PAY_METHODS =
            List.of(PaymentMethod.BKASH, PaymentMethod.NAGAD, PaymentMethod.ROCKET, PaymentMethod.BANK, PaymentMethod.CASH);

    private final SubscriptionPlanRepository plans;
    private final AppSubscriptionRepository subs;
    private final AppUserRepository users;
    private final BranchRepository branches;

    private volatile SubscriptionStatus cache;
    private volatile long cacheAt;

    // ---------- প্রথমবার চালুর সময় ডিফল্ট প্ল্যান ও ১৪ দিনের ট্রায়াল ----------
    @Transactional
    public void ensureDefaults() {
        if (plans.count() == 0) {
            plans.saveAll(List.of(
                    new SubscriptionPlan("TRIAL", "ফ্রি ট্রায়াল (১৪ দিন)", "0", 14, 3, 10),
                    new SubscriptionPlan("BASIC", "বেসিক (১ মাস)", "999", 30, 1, 3),
                    new SubscriptionPlan("STANDARD", "স্ট্যান্ডার্ড (৬ মাস)", "4999", 180, 3, 10),
                    new SubscriptionPlan("PREMIUM", "প্রিমিয়াম (১ বছর)", "8999", 365, 10, 50)));
        }
        if (subs.count() == 0) {
            SubscriptionPlan trial = plans.findByCode("TRIAL").orElseThrow();
            AppSubscription s = new AppSubscription();
            s.setPlan(trial);
            s.setState(SubscriptionState.ACTIVE);
            s.setStartDate(LocalDate.now());
            s.setEndDate(LocalDate.now().plusDays(trial.getDurationDays() - 1L));
            s.setAmount(BigDecimal.ZERO);
            s.setRequestedBy("system");
            s.setDecidedBy("system");
            s.setDecidedAt(LocalDateTime.now());
            s.setNote("স্বয়ংক্রিয় ফ্রি ট্রায়াল");
            subs.save(s);
        }
        invalidate();
    }

    // ---------- অবস্থা ----------
    public SubscriptionStatus status() {
        SubscriptionStatus c = cache;
        LocalDate today = LocalDate.now();
        if (c != null && System.currentTimeMillis() - cacheAt < CACHE_MS && today.equals(c.getComputedOn())) return c;

        List<AppSubscription> current = subs.findCurrent(SubscriptionState.ACTIVE, today, PageRequest.of(0, 1));
        LocalDate lastEnd = subs.latestEnd(SubscriptionState.ACTIVE);
        boolean pending = subs.existsByState(SubscriptionState.PENDING);

        SubscriptionStatus st;
        if (!current.isEmpty() && lastEnd != null) {
            st = new SubscriptionStatus(SubscriptionStatus.Level.ACTIVE, current.get(0).getPlan().getName(), lastEnd,
                    ChronoUnit.DAYS.between(today, lastEnd), pending, today);
        } else if (lastEnd == null) {
            st = new SubscriptionStatus(SubscriptionStatus.Level.NONE, "—", null, 0, pending, today);
        } else if (!today.isAfter(lastEnd.plusDays(GRACE_DAYS))) {
            st = new SubscriptionStatus(SubscriptionStatus.Level.GRACE, "—", lastEnd,
                    ChronoUnit.DAYS.between(today, lastEnd), pending, today);
        } else {
            st = new SubscriptionStatus(SubscriptionStatus.Level.EXPIRED, "—", lastEnd,
                    ChronoUnit.DAYS.between(today, lastEnd), pending, today);
        }
        cache = st;
        cacheAt = System.currentTimeMillis();
        return st;
    }

    public void invalidate() { cache = null; }

    // ---------- তালিকা ----------
    @Transactional(readOnly = true)
    public List<SubscriptionPlan> activePlans() { return plans.findByActiveTrueOrderByPriceAsc(); }

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> allPlans() { return plans.findAllByOrderByPriceAsc(); }

    @Transactional(readOnly = true)
    public List<AppSubscription> history() { return subs.findTop50ByOrderByIdDesc(); }

    @Transactional(readOnly = true)
    public List<AppSubscription> pending() { return subs.findByStateOrderByIdAsc(SubscriptionState.PENDING); }

    // ---------- মালিকের অনুরোধ ----------
    @Transactional
    public AppSubscription request(Forms.SubscriptionRequestForm f, UserPrincipal user) {
        SubscriptionPlan plan = paidPlan(f.getPlanId());
        validatePayment(f);
        if (subs.existsByState(SubscriptionState.PENDING))
            throw new BusinessException("আগের একটি অনুরোধ এখনো যাচাই হয়নি। সেটি সম্পন্ন হলে নতুন অনুরোধ করুন।");
        AppSubscription s = newSub(plan, f);
        s.setState(SubscriptionState.PENDING);
        s.setRequestedBy(user.getFullName());
        subs.save(s);
        invalidate();
        return s;
    }

    // ---------- সুপার অ্যাডমিনের যাচাই ----------
    @Transactional
    public void approve(Long id, UserPrincipal admin) {
        AppSubscription s = find(id);
        if (s.getState() != SubscriptionState.PENDING) throw new BusinessException("অনুরোধটি আগেই নিষ্পত্তি হয়েছে।");
        activate(s, admin.getFullName());
        invalidate();
    }

    @Transactional
    public void reject(Long id, String reason, UserPrincipal admin) {
        AppSubscription s = find(id);
        if (s.getState() != SubscriptionState.PENDING) throw new BusinessException("অনুরোধটি আগেই নিষ্পত্তি হয়েছে।");
        s.setState(SubscriptionState.REJECTED);
        s.setDecidedBy(admin.getFullName());
        s.setDecidedAt(LocalDateTime.now());
        s.setNote(reason == null || reason.isBlank() ? "প্রত্যাখ্যাত" : reason.trim());
        invalidate();
    }

    /** পেমেন্ট সরাসরি পেয়ে গেলে সুপার অ্যাডমিন সঙ্গে সঙ্গে সক্রিয় করতে পারেন */
    @Transactional
    public void grant(Forms.SubscriptionRequestForm f, UserPrincipal admin) {
        SubscriptionPlan plan = paidPlan(f.getPlanId());
        AppSubscription s = newSub(plan, f);
        s.setRequestedBy(admin.getFullName());
        activate(s, admin.getFullName());
        subs.save(s);
        invalidate();
    }

    @Transactional
    public void updatePlan(Long id, Forms.PlanForm f) {
        SubscriptionPlan p = plans.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.setPrice(f.getPrice());
        p.setDurationDays(f.getDurationDays());
        p.setMaxBranches(f.getMaxBranches());
        p.setMaxUsers(f.getMaxUsers());
        p.setActive(f.isActive());
    }

    // ---------- প্ল্যানের সীমা ----------
    public void checkBranchLimit() {
        SubscriptionPlan p = currentPlan();
        if (p != null && branches.countByActiveTrue() >= p.getMaxBranches())
            throw new BusinessException("আপনার প্ল্যানে সর্বোচ্চ " + p.getMaxBranches() + "টি শাখা চলে। আরও শাখা যোগ করতে প্ল্যান আপগ্রেড করুন।");
    }

    public void checkUserLimit() {
        SubscriptionPlan p = currentPlan();
        if (p != null && users.countByActiveTrue() >= p.getMaxUsers())
            throw new BusinessException("আপনার প্ল্যানে সর্বোচ্চ " + p.getMaxUsers() + " জন ব্যবহারকারী চলে। প্ল্যান আপগ্রেড করুন।");
    }

    // ---------- helpers ----------
    private SubscriptionPlan currentPlan() {
        List<AppSubscription> cur = subs.findCurrent(SubscriptionState.ACTIVE, LocalDate.now(), PageRequest.of(0, 1));
        return cur.isEmpty() ? null : cur.get(0).getPlan();
    }

    private void activate(AppSubscription s, String by) {
        LocalDate today = LocalDate.now();
        LocalDate last = subs.latestEnd(SubscriptionState.ACTIVE);
        // আগের মেয়াদ বাকি থাকলে তার পর থেকে শুরু হয়, নইলে আজ থেকে
        LocalDate start = (last != null && !last.isBefore(today)) ? last.plusDays(1) : today;
        s.setStartDate(start);
        s.setEndDate(start.plusDays(s.getPlan().getDurationDays() - 1L));
        s.setState(SubscriptionState.ACTIVE);
        s.setDecidedBy(by);
        s.setDecidedAt(LocalDateTime.now());
    }

    private AppSubscription newSub(SubscriptionPlan plan, Forms.SubscriptionRequestForm f) {
        validatePayment(f);
        AppSubscription s = new AppSubscription();
        s.setPlan(plan);
        s.setAmount(plan.getPrice());
        s.setPaymentMethod(f.getMethod());
        s.setTrxId(blankToNull(f.getTrxId()));
        s.setSenderNumber(blankToNull(f.getSenderNumber()));
        s.setNote(blankToNull(f.getNote()));
        return s;
    }

    private void validatePayment(Forms.SubscriptionRequestForm f) {
        if (!PAY_METHODS.contains(f.getMethod())) throw new BusinessException("এই পেমেন্ট পদ্ধতি সমর্থিত নয়।");
        if (f.getMethod() != PaymentMethod.CASH && (f.getTrxId() == null || f.getTrxId().isBlank()))
            throw new BusinessException("ট্রানজেকশন আইডি (TrxID) আবশ্যক।");
    }

    private SubscriptionPlan paidPlan(Long id) {
        SubscriptionPlan p = plans.findById(id).orElseThrow(() -> new BusinessException("প্ল্যান পাওয়া যায়নি।"));
        if (!p.isActive()) throw new BusinessException("এই প্ল্যানটি এখন চালু নেই।");
        if (p.isFree()) throw new BusinessException("ফ্রি ট্রায়াল প্ল্যান নবায়নযোগ্য নয়।");
        return p;
    }

    private AppSubscription find(Long id) {
        return subs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
