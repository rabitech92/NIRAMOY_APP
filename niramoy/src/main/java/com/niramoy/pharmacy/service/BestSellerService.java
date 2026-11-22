package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.MedicineMark;
import com.niramoy.pharmacy.entity.SaleStatus;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.MedicineMarkRepository;
import com.niramoy.pharmacy.repository.MedicineRepository;
import com.niramoy.pharmacy.repository.SaleItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * কোন ওষুধ বেশি বিক্রি হয় তার র‍্যাংকিং ও "বেশি বিক্রিত" চিহ্ন।
 * চিহ্ন ওষুধের নাম ধরে রাখা হয় (সব ব্যাচ/শাখায় প্রযোজ্য)। বিন Thymeleaf-এ ${@marks.isMarked(name)} হিসেবে ব্যবহৃত।
 */
@Service("marks")
@RequiredArgsConstructor
public class BestSellerService {
    private static final long CACHE_MS = 30_000;

    private final SaleItemRepository items;
    private final MedicineRepository medicines;
    private final MedicineMarkRepository markRepo;

    private volatile Set<String> cache;
    private volatile long cacheAt;

    public record SellerRow(String name, long pieces, BigDecimal revenue, long invoices, long stock, boolean marked) { }

    public static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    // ---------- চিহ্ন ----------
    public boolean isMarked(String name) {
        return keys().contains(key(name));
    }

    private Set<String> keys() {
        Set<String> c = cache;
        if (c != null && System.currentTimeMillis() - cacheAt < CACHE_MS) return c;
        Set<String> fresh = markRepo.findAll().stream().map(MedicineMark::getNameKey).collect(Collectors.toSet());
        cache = fresh;
        cacheAt = System.currentTimeMillis();
        return fresh;
    }

    public void invalidate() { cache = null; }

    @Transactional(readOnly = true)
    public List<MedicineMark> allMarks() { return markRepo.findAllByOrderByMarkedAtDesc(); }

    // ---------- র‍্যাংকিং ----------
    /** sort: qty (পিস, ডিফল্ট) | revenue (টাকা) | orders (ইনভয়েস সংখ্যা) */
    @Transactional(readOnly = true)
    public List<SellerRow> ranking(Long branchId, LocalDate from, LocalDate to, String sort, int limit) {
        List<Object[]> rows = items.topSelling(branchId, SaleStatus.COMPLETED, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<String, Long> stock = new HashMap<>();
        for (Object[] r : medicines.stockByName(branchId)) stock.put(key((String) r[0]), ((Number) r[1]).longValue());
        Set<String> marked = keys();

        List<SellerRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            String name = (String) r[0];
            list.add(new SellerRow(name, ((Number) r[1]).longValue(), (BigDecimal) r[2], ((Number) r[3]).longValue(),
                    stock.getOrDefault(key(name), 0L), marked.contains(key(name))));
        }
        Comparator<SellerRow> cmp = switch (sort == null ? "" : sort) {
            case "revenue" -> Comparator.comparing(SellerRow::revenue).reversed();
            case "orders" -> Comparator.comparingLong(SellerRow::invoices).reversed();
            default -> Comparator.comparingLong(SellerRow::pieces).reversed();
        };
        return list.stream().sorted(cmp.thenComparing(SellerRow::name)).limit(Math.max(1, Math.min(limit, 200))).toList();
    }

    // ---------- চিহ্নিত করা / তোলা ----------
    @Transactional
    public void mark(Long branchId, String name, LocalDate from, LocalDate to, String periodLabel, String by) {
        if (name == null || name.isBlank()) throw new BusinessException("ওষুধের নাম পাওয়া যায়নি।");
        String k = key(name);
        if (markRepo.findByNameKey(k).isPresent()) return;
        SellerRow stats = ranking(branchId, from, to, "qty", 200).stream()
                .filter(r -> key(r.name()).equals(k)).findFirst().orElse(null);
        save(name.trim(), stats, periodLabel, false, by);
        invalidate();
    }

    /** র‍্যাংকিংয়ের শীর্ষ [limit] টি ওষুধ একসাথে চিহ্নিত করে; নতুন কয়টি চিহ্নিত হলো তা ফেরত দেয় */
    @Transactional
    public int markTop(Long branchId, LocalDate from, LocalDate to, String sort, int limit, String periodLabel, String by) {
        int n = 0;
        for (SellerRow r : ranking(branchId, from, to, sort, limit)) {
            if (r.marked()) continue;
            save(r.name(), r, periodLabel, true, by);
            n++;
        }
        invalidate();
        return n;
    }

    @Transactional
    public void unmark(String name) {
        markRepo.deleteByNameKey(key(name));
        invalidate();
    }

    private void save(String name, SellerRow stats, String periodLabel, boolean auto, String by) {
        MedicineMark m = new MedicineMark();
        m.setNameKey(key(name));
        m.setDisplayName(name.length() > 150 ? name.substring(0, 150) : name);
        m.setAutoMarked(auto);
        m.setSoldPieces(stats == null ? 0 : stats.pieces());
        m.setRevenue(stats == null ? BigDecimal.ZERO : stats.revenue());
        m.setPeriodLabel(periodLabel);
        m.setMarkedBy(by);
        markRepo.save(m);
    }
}
