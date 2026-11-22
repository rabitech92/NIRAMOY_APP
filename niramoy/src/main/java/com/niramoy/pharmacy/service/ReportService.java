package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.Medicine;
import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.ExpenseRepository;
import com.niramoy.pharmacy.repository.MedicineRepository;
import com.niramoy.pharmacy.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private static final long MAX_DAYS = 366;

    private final SaleRepository sales;
    private final MedicineRepository medicines;
    private final ExpenseRepository expenseRepo;

    public record BranchRow(String name, BigDecimal sales, BigDecimal profit) { }

    public record ReportView(LocalDate from, LocalDate to, BigDecimal totalSales, BigDecimal profit,
                             BigDecimal discount, BigDecimal vat, int invoices, BigDecimal expiredStockValue,
                             long expiring, long lowStock, BigDecimal receivable,
                             List<String> labels, List<BigDecimal> values, List<BranchRow> branchRows,
                             BigDecimal expenses, BigDecimal netProfit) { }

    public ReportView build(LocalDate from, LocalDate to, Long branchId) {
        if (from.isAfter(to)) throw new BusinessException("শুরুর তারিখ শেষ তারিখের আগে হতে হবে।");
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) throw new BusinessException("সর্বোচ্চ ৩৬৬ দিনের রিপোর্ট নেওয়া যায়।");

        List<Sale> list = sales.findCompletedBetween(branchId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        BigDecimal total = BigDecimal.ZERO, profit = BigDecimal.ZERO, discount = BigDecimal.ZERO, vat = BigDecimal.ZERO;
        Map<LocalDate, BigDecimal> perDay = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) perDay.put(d, BigDecimal.ZERO);
        Map<String, BigDecimal[]> perBranch = new TreeMap<>();   // [sales, profit]

        for (Sale s : list) {
            total = total.add(s.getTotal());
            profit = profit.add(s.getProfit());
            discount = discount.add(s.getDiscount());
            vat = vat.add(s.getVatAmount());
            perDay.merge(s.getCreatedAt().toLocalDate(), s.getTotal(), BigDecimal::add);
            BigDecimal[] row = perBranch.computeIfAbsent(s.getBranch().getName(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            row[0] = row[0].add(s.getTotal());
            row[1] = row[1].add(s.getProfit());
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        perDay.forEach((d, v) -> { labels.add(d.getDayOfMonth() + "/" + d.getMonthValue()); values.add(v); });

        List<BranchRow> rows = new ArrayList<>();
        perBranch.forEach((n, v) -> rows.add(new BranchRow(n, v[0], v[1])));

        BigDecimal exp = expenseRepo.sumAmount(branchId, from, to);
        if (exp == null) exp = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        BigDecimal expired = medicines.expiredStockValue(branchId, today);
        BigDecimal receivable = sales.totalReceivable(branchId);
        return new ReportView(from, to, total, profit, discount, vat, list.size(),
                expired == null ? BigDecimal.ZERO : expired,
                medicines.countExpiringBetween(branchId, today, today.plusDays(Medicine.EXPIRY_WARNING_DAYS)),
                medicines.countLowStock(branchId),
                receivable == null ? BigDecimal.ZERO : receivable,
                labels, values, rows, exp, profit.subtract(exp));
    }
}
