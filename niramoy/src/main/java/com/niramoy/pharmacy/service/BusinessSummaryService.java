package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.PurchaseEntry;
import com.niramoy.pharmacy.entity.PurchaseSource;
import com.niramoy.pharmacy.entity.SaleStatus;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.*;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.controller.Forms;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * ক্রয়-বিক্রয় ও স্টকের হিসাব: স্টকে এখন কত টাকার মাল, মাসে/বছরে কত ক্রয়, কত বিক্রয়, কত লাভ।
 * ক্রয়ের হিসাব "ক্রয়ের খাতা" (PurchaseEntry) থেকে, যা এই সুবিধা চালুর পর থেকে জমে।
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessSummaryService {
    private static final String[] BN_MONTHS = {"জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই",
            "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"};

    private final MedicineRepository medicines;
    private final SaleRepository sales;
    private final SaleItemRepository saleItems;
    private final PurchaseEntryRepository purchases;
    private final PurchaseReturnRepository purchaseReturns;
    private final StockSnapshotRepository snapshots;

    public record Stock(BigDecimal costValue, BigDecimal saleValue, BigDecimal potentialProfit, long units, long batches,
                        BigDecimal expiredCost, BigDecimal expiringCost) { }

    /** একটি মাস বা বছরের সারি; stockEnd = সেই মাস/বছর শেষে স্টকের ক্রয়মূল্য (ছবি না থাকলে null) */
    public record Row(String label, BigDecimal purchases, BigDecimal returns, BigDecimal netPurchases, BigDecimal sales,
                      BigDecimal profit, long invoices, BigDecimal stockEnd) { }

    public record ValueRow(String name, BigDecimal cost, BigDecimal sale, long units) { }

    public record Summary(Stock stock, BigDecimal salesToday, BigDecimal salesWeek, BigDecimal salesMonth,
                          BigDecimal salesYear, BigDecimal salesTotal, BigDecimal purchaseMonth, BigDecimal purchaseYear,
                          BigDecimal purchaseTotal, List<Row> months, List<Row> years, List<ValueRow> byBranch,
                          List<ValueRow> byCategory, List<ValueRow> topStock, List<String> chartLabels,
                          List<BigDecimal> chartPurchases, List<BigDecimal> chartSales) { }

    public Summary build(Long branchId) {
        LocalDate today = LocalDate.now();

        // ---------- স্টক ----------
        List<Object[]> t = medicines.stockTotals(branchId);
        BigDecimal cost = BigDecimal.ZERO, sale = BigDecimal.ZERO;
        long units = 0, batches = 0;
        if (!t.isEmpty() && t.get(0)[0] != null) {
            cost = (BigDecimal) t.get(0)[0];
            sale = (BigDecimal) t.get(0)[1];
            units = ((Number) t.get(0)[2]).longValue();
            batches = ((Number) t.get(0)[3]).longValue();
        }
        Stock stock = new Stock(cost, sale, sale.subtract(cost), units, batches,
                nz(medicines.expiredStockValue(branchId, today)),
                nz(medicines.stockCostBetween(branchId, today, today.plusDays(90))));

        // ---------- মাসভিত্তিক ডেটা (কী = বছর*100 + মাস) ----------
        Map<Integer, BigDecimal> pur = toMap(purchases.monthlyPurchases(branchId));
        Map<Integer, BigDecimal> ret = toMap(purchaseReturns.monthlyReturns(branchId));
        Map<Integer, BigDecimal> margin = toMap(saleItems.monthlyMargin(branchId, SaleStatus.COMPLETED));
        Map<Integer, BigDecimal> sal = new HashMap<>();
        Map<Integer, BigDecimal> disc = new HashMap<>();
        Map<Integer, Long> inv = new HashMap<>();
        for (Object[] r : sales.monthlySales(branchId, SaleStatus.COMPLETED)) {
            int k = key(r[0], r[1]);
            inv.put(k, ((Number) r[2]).longValue());
            sal.put(k, nz((BigDecimal) r[3]));
            disc.put(k, nz((BigDecimal) r[4]));
        }
        TreeMap<Integer, BigDecimal> stockEnd = new TreeMap<>();      // মাসের শেষ ছবি (তারিখ ক্রমে, শেষটি টিকে থাকে)
        for (Object[] r : snapshots.byDate(branchId)) {
            LocalDate d = (LocalDate) r[0];
            stockEnd.put(d.getYear() * 100 + d.getMonthValue(), nz((BigDecimal) r[1]));
        }

        // ---------- শেষ ১২ মাস ----------
        YearMonth cur = YearMonth.now();
        List<Row> months = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> chartP = new ArrayList<>();
        List<BigDecimal> chartS = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth ym = cur.minusMonths(i);
            int k = ym.getYear() * 100 + ym.getMonthValue();
            BigDecimal end = i == 0 ? cost : stockEnd.get(k);
            Row row = row(BN_MONTHS[ym.getMonthValue() - 1] + " " + ym.getYear(), pur.get(k), ret.get(k), sal.get(k),
                    margin.get(k), disc.get(k), inv.get(k), end);
            months.add(row);
            labels.add(0, BN_MONTHS[ym.getMonthValue() - 1].substring(0, Math.min(3, BN_MONTHS[ym.getMonthValue() - 1].length()))
                    + " " + (ym.getYear() % 100));
            chartP.add(0, row.netPurchases());
            chartS.add(0, row.sales());
        }

        // ---------- বছরভিত্তিক ----------
        TreeSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        for (Map<Integer, ?> m : List.of(pur, ret, sal, margin, stockEnd)) for (Integer k : m.keySet()) years.add(k / 100);
        years.add(cur.getYear());
        List<Row> yearRows = new ArrayList<>();
        for (int y : years) {
            BigDecimal p = sumYear(pur, y), r = sumYear(ret, y), s = sumYear(sal, y), mg = sumYear(margin, y), d = sumYear(disc, y);
            long invoices = 0;
            for (Map.Entry<Integer, Long> e : inv.entrySet()) if (e.getKey() / 100 == y) invoices += e.getValue();
            BigDecimal end;
            if (y == cur.getYear()) end = cost;
            else {
                Integer lastKey = stockEnd.floorKey(y * 100 + 12);
                end = (lastKey != null && lastKey / 100 == y) ? stockEnd.get(lastKey) : null;
            }
            yearRows.add(new Row(String.valueOf(y), p, r, p.subtract(r), s, mg.subtract(d), invoices, end));
        }

        // ---------- আজ/সপ্তাহ/মাস/বছরের বিক্রয় ----------
        BigDecimal salesToday = nz(sales.sumTotalBetween(branchId, SaleStatus.COMPLETED, today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        BigDecimal salesWeek = nz(sales.sumTotalBetween(branchId, SaleStatus.COMPLETED,
                CustomerInsightService.weekStart(today).atStartOfDay(), today.plusDays(1).atStartOfDay()));
        int ck = cur.getYear() * 100 + cur.getMonthValue();
        BigDecimal salesMonth = sal.getOrDefault(ck, BigDecimal.ZERO);
        BigDecimal salesYear = sumYear(sal, cur.getYear());
        BigDecimal salesTotal = sal.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purMonth = pur.getOrDefault(ck, BigDecimal.ZERO).subtract(ret.getOrDefault(ck, BigDecimal.ZERO));
        BigDecimal purYear = sumYear(pur, cur.getYear()).subtract(sumYear(ret, cur.getYear()));
        BigDecimal purTotal = pur.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(ret.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        return new Summary(stock, salesToday, salesWeek, salesMonth, salesYear, salesTotal, purMonth, purYear, purTotal,
                months, yearRows, valueRows(medicines.stockByBranch(branchId)), valueRows(medicines.stockByCategory(branchId)),
                valueRows(medicines.topStockValue(branchId, PageRequest.of(0, 10))), labels, chartP, chartS);
    }

    /** পুরনো/বাইরের ক্রয় (যা সিস্টেমে ছিল না) হাতে যোগ করা — যাতে মাস ও বছরের ক্রয়ের হিসাব পূর্ণ হয় */
    @Transactional
    public void addManualPurchase(Forms.ManualPurchaseForm f, com.niramoy.pharmacy.entity.Branch branch, UserPrincipal user) {
        LocalDate date = f.getDate() == null ? LocalDate.now() : f.getDate();
        if (date.isAfter(LocalDate.now())) throw new BusinessException("ভবিষ্যতের তারিখে ক্রয় যোগ করা যায় না।");
        PurchaseEntry p = new PurchaseEntry();
        p.setBranch(branch);
        p.setEntryDate(date);
        p.setQuantity(0);
        p.setTotalCost(f.getAmount());
        p.setSupplierName(blank(f.getSupplier()));
        p.setNote(blank(f.getNote()));
        p.setSource(PurchaseSource.MANUAL_TOTAL);
        p.setCreatedBy(user.getFullName());
        purchases.save(p);
    }

    // ---------- helpers ----------
    private static Row row(String label, BigDecimal p, BigDecimal r, BigDecimal s, BigDecimal margin, BigDecimal disc,
                           Long invoices, BigDecimal stockEnd) {
        BigDecimal pp = nz(p), rr = nz(r);
        return new Row(label, pp, rr, pp.subtract(rr), nz(s), nz(margin).subtract(nz(disc)), invoices == null ? 0 : invoices, stockEnd);
    }

    private static List<ValueRow> valueRows(List<Object[]> rows) {
        List<ValueRow> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new ValueRow(String.valueOf(r[0]), nz((BigDecimal) r[1]), nz((BigDecimal) r[2]),
                    r[3] == null ? 0 : ((Number) r[3]).longValue()));
        }
        return out;
    }

    private static Map<Integer, BigDecimal> toMap(List<Object[]> rows) {
        Map<Integer, BigDecimal> m = new HashMap<>();
        for (Object[] r : rows) m.merge(key(r[0], r[1]), nz((BigDecimal) r[2]), BigDecimal::add);
        return m;
    }

    private static int key(Object year, Object month) {
        return ((Number) year).intValue() * 100 + ((Number) month).intValue();
    }

    private static BigDecimal sumYear(Map<Integer, BigDecimal> m, int year) {
        BigDecimal s = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> e : m.entrySet()) if (e.getKey() / 100 == year) s = s.add(e.getValue());
        return s;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String blank(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
