package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.entity.SaleStatus;
import com.niramoy.pharmacy.repository.SaleItemRepository;
import com.niramoy.pharmacy.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** গ্রাহকভিত্তিক বিক্রয় বিশ্লেষণ: জীবনকালের মোট, দৈনিক/সাপ্তাহিক/মাসিক/বার্ষিক হিসাব ও কেনাকাটার তালিকা */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerInsightService {
    private static final String[] BN_MONTHS = {"জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই",
            "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"};

    private final SaleRepository sales;
    private final SaleItemRepository itemRepo;

    public record CustomerRow(String phone, String name, long invoices, BigDecimal lifetime, LocalDateTime lastAt,
                              BigDecimal due, BigDecimal today, BigDecimal week, BigDecimal month, BigDecimal year) { }

    public record CustomerList(List<CustomerRow> rows, int page, int totalPages, long totalCustomers,
                               long allCustomers, BigDecimal allLifetime, BigDecimal allAverage) { }

    public record Bucket(String label, long invoices, BigDecimal amount) { }

    public record TopItem(String name, long pieces, BigDecimal amount, long times) { }

    public record Detail(String phone, String name, long invoices, BigDecimal lifetime, BigDecimal average,
                         BigDecimal due, LocalDateTime firstAt, LocalDateTime lastAt, BigDecimal today, BigDecimal week,
                         BigDecimal month, BigDecimal year, List<Bucket> daily, List<Bucket> weekly,
                         List<Bucket> monthly, List<Bucket> yearly, List<Sale> recent, List<TopItem> topItems) { }

    /** সপ্তাহ শুরু শনিবার থেকে */
    public static LocalDate weekStart(LocalDate d) {
        LocalDate sat = d.with(DayOfWeek.SATURDAY);
        return sat.isAfter(d) ? sat.minusWeeks(1) : sat;
    }

    // ---------- তালিকা ----------
    public CustomerList list(Long branchId, String q, String sort, int page) {
        String query = q == null ? "" : q.trim();
        PageRequest pr = PageRequest.of(Math.max(page, 0), 15);
        Page<Object[]> p = "recent".equals(sort)
                ? sales.customersByRecent(branchId, SaleStatus.COMPLETED, query, pr)
                : sales.customersByTotal(branchId, SaleStatus.COMPLETED, query, pr);

        List<String> phones = p.getContent().stream().map(r -> (String) r[0]).toList();
        LocalDate today = LocalDate.now();
        Map<String, BigDecimal> day = since(phones, today.atStartOfDay(), branchId);
        Map<String, BigDecimal> week = since(phones, weekStart(today).atStartOfDay(), branchId);
        Map<String, BigDecimal> month = since(phones, today.withDayOfMonth(1).atStartOfDay(), branchId);
        Map<String, BigDecimal> year = since(phones, LocalDate.of(today.getYear(), 1, 1).atStartOfDay(), branchId);

        List<CustomerRow> rows = new ArrayList<>();
        for (Object[] r : p.getContent()) {
            String phone = (String) r[0];
            rows.add(new CustomerRow(phone, (String) r[1], ((Number) r[2]).longValue(), nz((BigDecimal) r[3]),
                    (LocalDateTime) r[4], nz((BigDecimal) r[5]),
                    day.getOrDefault(phone, BigDecimal.ZERO), week.getOrDefault(phone, BigDecimal.ZERO),
                    month.getOrDefault(phone, BigDecimal.ZERO), year.getOrDefault(phone, BigDecimal.ZERO)));
        }
        List<Object[]> tot = sales.customerTotals(branchId, SaleStatus.COMPLETED);
        long all = 0;
        BigDecimal allSum = BigDecimal.ZERO;
        if (!tot.isEmpty() && tot.get(0)[0] != null) {
            all = ((Number) tot.get(0)[0]).longValue();
            allSum = nz((BigDecimal) tot.get(0)[1]);
        }
        BigDecimal avg = all == 0 ? BigDecimal.ZERO : allSum.divide(BigDecimal.valueOf(all), 2, RoundingMode.HALF_UP);
        return new CustomerList(rows, p.getNumber(), p.getTotalPages(), p.getTotalElements(), all, allSum, avg);
    }

    // ---------- একজন গ্রাহকের বিস্তারিত ----------
    public Detail detail(Long branchId, String phone) {
        List<Sale> all = sales.customerSales(phone, SaleStatus.COMPLETED, branchId);   // নতুন আগে
        if (all.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime weekStart = weekStart(today).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();

        BigDecimal lifetime = BigDecimal.ZERO, due = BigDecimal.ZERO, d = BigDecimal.ZERO, w = BigDecimal.ZERO,
                m = BigDecimal.ZERO, y = BigDecimal.ZERO;
        Map<LocalDate, long[]> daily = new LinkedHashMap<>();       // [ইনভয়েস] + আলাদা টাকার ম্যাপ
        Map<LocalDate, BigDecimal> dailyAmt = new HashMap<>();
        Map<LocalDate, long[]> weekly = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> weeklyAmt = new HashMap<>();
        Map<LocalDate, long[]> monthly = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> monthlyAmt = new HashMap<>();
        Map<Integer, long[]> yearly = new LinkedHashMap<>();
        Map<Integer, BigDecimal> yearlyAmt = new HashMap<>();

        LocalDate dailyFrom = today.minusDays(29);
        LocalDate weeklyFrom = weekStart(today).minusWeeks(11);
        LocalDate monthlyFrom = today.withDayOfMonth(1).minusMonths(11);

        for (Sale s : all) {                                         // নতুন থেকে পুরনো
            BigDecimal t = s.getTotal();
            LocalDateTime at = s.getCreatedAt();
            LocalDate date = at.toLocalDate();
            lifetime = lifetime.add(t);
            due = due.add(s.getDueAmount());
            if (!at.isBefore(dayStart)) d = d.add(t);
            if (!at.isBefore(weekStart)) w = w.add(t);
            if (!at.isBefore(monthStart)) m = m.add(t);
            if (!at.isBefore(yearStart)) y = y.add(t);

            if (!date.isBefore(dailyFrom)) add(daily, dailyAmt, date, t);
            LocalDate ws = weekStart(date);
            if (!ws.isBefore(weeklyFrom)) add(weekly, weeklyAmt, ws, t);
            LocalDate ms = date.withDayOfMonth(1);
            if (!ms.isBefore(monthlyFrom)) add(monthly, monthlyAmt, ms, t);
            add(yearly, yearlyAmt, date.getYear(), t);
        }

        List<Bucket> dailyList = new ArrayList<>();
        daily.forEach((k, v) -> dailyList.add(new Bucket(fmt(k), v[0], dailyAmt.get(k))));
        List<Bucket> weeklyList = new ArrayList<>();
        weekly.forEach((k, v) -> weeklyList.add(new Bucket(fmt(k) + " – " + fmt(k.plusDays(6)), v[0], weeklyAmt.get(k))));
        List<Bucket> monthlyList = new ArrayList<>();
        monthly.forEach((k, v) -> monthlyList.add(new Bucket(BN_MONTHS[k.getMonthValue() - 1] + " " + k.getYear(), v[0], monthlyAmt.get(k))));
        List<Bucket> yearlyList = new ArrayList<>();
        yearly.forEach((k, v) -> yearlyList.add(new Bucket(String.valueOf(k), v[0], yearlyAmt.get(k))));

        // সাম্প্রতিক ৩০টি ইনভয়েস আইটেমসহ (কেনাকাটার প্রিভিউ)
        List<Long> ids = all.stream().limit(30).map(Sale::getId).toList();
        List<Sale> recent = sales.withItemsByIds(ids);

        Sale newest = all.get(0);
        Sale oldest = all.get(all.size() - 1);
        BigDecimal avg = lifetime.divide(BigDecimal.valueOf(all.size()), 2, RoundingMode.HALF_UP);
        return new Detail(phone, newest.getCustomerName(), all.size(), lifetime, avg, due, oldest.getCreatedAt(),
                newest.getCreatedAt(), d, w, m, y, dailyList, weeklyList, monthlyList, yearlyList, recent,
                topItems(phone, branchId));
    }

    /** এই গ্রাহক সবচেয়ে বেশি কোন ওষুধ কেনেন (কতবার কিনেছেন, তারপর টাকা অনুযায়ী) — শীর্ষ ১০ */
    private List<TopItem> topItems(String phone, Long branchId) {
        List<TopItem> list = new ArrayList<>();
        for (Object[] r : itemRepo.customerTop(phone, SaleStatus.COMPLETED, branchId)) {
            list.add(new TopItem((String) r[0], ((Number) r[1]).longValue(), nz((BigDecimal) r[2]), ((Number) r[3]).longValue()));
        }
        list.sort(Comparator.comparingLong(TopItem::times).reversed().thenComparing(TopItem::amount, Comparator.reverseOrder()));
        return list.size() > 10 ? new ArrayList<>(list.subList(0, 10)) : list;
    }

    // ---------- helpers ----------
    private Map<String, BigDecimal> since(List<String> phones, LocalDateTime from, Long branchId) {
        Map<String, BigDecimal> map = new HashMap<>();
        if (phones.isEmpty()) return map;
        for (Object[] r : sales.sumSince(phones, SaleStatus.COMPLETED, from, branchId)) {
            map.put((String) r[0], nz((BigDecimal) r[1]));
        }
        return map;
    }

    private static <K> void add(Map<K, long[]> counts, Map<K, BigDecimal> amounts, K key, BigDecimal amount) {
        counts.computeIfAbsent(key, k -> new long[1])[0]++;
        amounts.merge(key, amount, BigDecimal::add);
    }

    private static String fmt(LocalDate d) {
        return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
