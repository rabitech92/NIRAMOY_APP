package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.Medicine;
import com.niramoy.pharmacy.entity.PaymentMethod;
import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.repository.BranchRepository;
import com.niramoy.pharmacy.repository.MedicineRepository;
import com.niramoy.pharmacy.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private static final String[] BN_DAYS = {"সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি", "রবি"};

    private final SaleRepository sales;
    private final MedicineRepository medicines;
    private final BranchRepository branches;
    private final PurchaseService purchases;

    public record MixRow(String label, int percent) { }

    public record DashboardView(BigDecimal todaySales, BigDecimal todayProfit, BigDecimal receivable,
                                BigDecimal supplierDue, long lowStock, long expiring, long medicineCount,
                                long branchCount, List<String> trendLabels, List<BigDecimal> trendValues,
                                List<MixRow> paymentMix, List<Sale> recentSales) { }

    public DashboardView build(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);
        List<Sale> week = sales.findCompletedBetween(branchId, from.atStartOfDay(), today.plusDays(1).atStartOfDay());

        // দিনভিত্তিক বিক্রয় (শূন্য দিনসহ)
        Map<LocalDate, BigDecimal> perDay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) perDay.put(from.plusDays(i), BigDecimal.ZERO);
        BigDecimal todaySales = BigDecimal.ZERO, todayProfit = BigDecimal.ZERO;
        Map<PaymentMethod, BigDecimal> byMethod = new EnumMap<>(PaymentMethod.class);
        BigDecimal weekTotal = BigDecimal.ZERO;
        for (Sale s : week) {
            LocalDate d = s.getCreatedAt().toLocalDate();
            perDay.merge(d, s.getTotal(), BigDecimal::add);
            byMethod.merge(s.getPaymentMethod(), s.getTotal(), BigDecimal::add);
            weekTotal = weekTotal.add(s.getTotal());
            if (d.equals(today)) { todaySales = todaySales.add(s.getTotal()); todayProfit = todayProfit.add(s.getProfit()); }
        }
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        perDay.forEach((d, v) -> { labels.add(BN_DAYS[d.getDayOfWeek().getValue() - 1]); values.add(v); });

        List<MixRow> mix = new ArrayList<>();
        for (Map.Entry<PaymentMethod, BigDecimal> e : byMethod.entrySet()) {
            int pct = weekTotal.signum() == 0 ? 0
                    : e.getValue().multiply(BigDecimal.valueOf(100)).divide(weekTotal, 0, RoundingMode.HALF_UP).intValue();
            mix.add(new MixRow(e.getKey().getLabel(), pct));
        }
        mix.sort(Comparator.comparingInt(MixRow::percent).reversed());

        BigDecimal receivable = sales.totalReceivable(branchId);
        List<Sale> recent = week.stream().limit(5).toList();

        return new DashboardView(todaySales, todayProfit,
                receivable == null ? BigDecimal.ZERO : receivable,
                purchases.totalSupplierDue(),
                medicines.countLowStock(branchId),
                medicines.countExpiringBetween(branchId, today, today.plusDays(Medicine.EXPIRY_WARNING_DAYS)),
                medicines.countAll(branchId),
                branches.countByActiveTrue(),
                labels, values, mix, recent);
    }
}
