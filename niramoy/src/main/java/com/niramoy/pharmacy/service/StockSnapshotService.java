package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.Branch;
import com.niramoy.pharmacy.entity.StockSnapshot;
import com.niramoy.pharmacy.repository.BranchRepository;
import com.niramoy.pharmacy.repository.MedicineRepository;
import com.niramoy.pharmacy.repository.StockSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * প্রতিদিন রাতে (এবং অ্যাপ চালুর সময়) প্রতিটি শাখার স্টকের মূল্যের একটি ছবি রাখে।
 * এর ভিত্তিতে "মাস/বছর শেষে স্টকে কত টাকার মাল ছিল" দেখানো হয় (এই সুবিধা চালুর দিন থেকে জমা হয়)।
 */
@Component
@RequiredArgsConstructor
public class StockSnapshotService implements ApplicationRunner {
    private final StockSnapshotRepository snapshots;
    private final BranchRepository branches;
    private final MedicineRepository medicines;

    @Override
    public void run(ApplicationArguments args) {
        takeToday();
    }

    @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Dhaka")
    public void nightly() {
        takeToday();
    }

    @Transactional
    public void takeToday() {
        LocalDate today = LocalDate.now();
        for (Branch b : branches.findAll()) {
            List<Object[]> t = medicines.stockTotals(b.getId());
            BigDecimal cost = BigDecimal.ZERO, sale = BigDecimal.ZERO;
            long units = 0;
            if (!t.isEmpty() && t.get(0)[0] != null) {
                cost = (BigDecimal) t.get(0)[0];
                sale = (BigDecimal) t.get(0)[1];
                units = ((Number) t.get(0)[2]).longValue();
            }
            StockSnapshot s = snapshots.findByBranchIdAndSnapshotDate(b.getId(), today).orElseGet(() -> {
                StockSnapshot n = new StockSnapshot();
                n.setBranch(b);
                n.setSnapshotDate(today);
                return n;
            });
            s.setCostValue(cost);
            s.setSaleValue(sale);
            s.setUnits(units);
            snapshots.save(s);
        }
    }
}
