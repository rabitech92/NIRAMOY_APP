package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.Branch;
import com.niramoy.pharmacy.entity.Medicine;
import com.niramoy.pharmacy.entity.PurchaseEntry;
import com.niramoy.pharmacy.entity.PurchaseSource;
import com.niramoy.pharmacy.entity.StockAdjustment;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.MedicineRepository;
import com.niramoy.pharmacy.repository.PurchaseEntryRepository;
import com.niramoy.pharmacy.repository.StockAdjustmentRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicineService {
    public static final int PAGE_SIZE = 10;
    private final MedicineRepository medicines;
    private final StockAdjustmentRepository adjustments;
    private final PurchaseEntryRepository entries;

    public Page<Medicine> search(Long branchId, String q, String filter, int page) {
        LocalDate today = LocalDate.now();
        String f = filter == null || filter.isBlank() ? "ALL" : filter;
        return medicines.search(branchId, q == null ? "" : q.trim(), f, today,
                today.plusDays(Medicine.EXPIRY_WARNING_DAYS),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("expiryDate").and(Sort.by("name"))));
    }

    public Medicine get(Long id) {
        return medicines.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<StockAdjustment> recentAdjustments(Long medicineId) {
        return adjustments.findTop10ByMedicineIdOrderByIdDesc(medicineId);
    }

    public List<Medicine> posSearch(Long branchId, String q) {
        return medicines.searchForSale(branchId, q == null ? "" : q.trim(), LocalDate.now(), PageRequest.of(0, 20));
    }

    @Transactional
    public Medicine create(Forms.MedicineForm f, Branch branch) {
        medicines.findByBranchIdAndBarcodeAndBatchNo(branch.getId(), f.getBarcode().trim(), f.getBatchNo().trim())
                .ifPresent(m -> { throw new BusinessException("এই শাখায় একই বারকোড ও ব্যাচের ওষুধ আগেই আছে।"); });
        if (f.getSalePrice().compareTo(f.getCostPrice()) < 0)
            throw new BusinessException("বিক্রয়মূল্য ক্রয়মূল্যের চেয়ে কম হতে পারবে না।");
        if (f.getStripPrice() != null
                && f.getStripPrice().compareTo(f.getCostPrice().multiply(BigDecimal.valueOf(f.getStripSize()))) < 0)
            throw new BusinessException("পাতার বিক্রয়মূল্য পাতার ক্রয়মূল্যের (পিস ক্রয়মূল্য × পিস সংখ্যা) চেয়ে কম হতে পারবে না।");
        Medicine m = new Medicine();
        m.setName(f.getName().trim());
        m.setCategory(f.getCategory() == null ? null : f.getCategory().trim());
        m.setBarcode(f.getBarcode().trim());
        m.setBatchNo(f.getBatchNo().trim());
        m.setExpiryDate(f.getExpiryDate());
        m.setCostPrice(f.getCostPrice());
        m.setSalePrice(f.getSalePrice());
        long total = (long) f.getStockStrips() * f.getStripSize() + f.getStockPieces();
        if (total > 10_000_000L) throw new BusinessException("স্টক পরিমাণ অতিরিক্ত বেশি।");
        m.setStock((int) total);
        m.setBoxNo(f.getBoxNo().trim());
        m.setGenericName(blankToNull(f.getGenericName()));
        m.setCompanyName(blankToNull(f.getCompanyName()));
        m.setStripPrice(f.getStripPrice());
        m.setReorderLevel(f.getReorderLevel());
        m.setStripSize(f.getStripSize());
        m.setBranch(branch);
        Medicine saved = medicines.save(m);

        // হাতে নতুন ওষুধ/ব্যাচ যোগ হলে সেটিও ক্রয়ের খাতায় ওঠে
        if (saved.getStock() > 0) {
            PurchaseEntry pe = new PurchaseEntry();
            pe.setBranch(branch);
            pe.setEntryDate(LocalDate.now());
            pe.setMedicineName(saved.getName());
            pe.setBatchNo(saved.getBatchNo());
            pe.setQuantity(saved.getStock());
            pe.setUnitCost(saved.getCostPrice());
            pe.setTotalCost(saved.getCostPrice().multiply(BigDecimal.valueOf(saved.getStock())));
            pe.setSource(PurchaseSource.MANUAL_STOCK);
            pe.setCreatedBy(com.niramoy.pharmacy.security.UserPrincipal.current() == null ? null
                    : com.niramoy.pharmacy.security.UserPrincipal.current().getFullName());
            entries.save(pe);
        }
        return saved;
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    @Transactional
    public void adjust(Long id, int delta, String reason, String performedBy) {
        if (delta == 0) throw new BusinessException("পরিবর্তনের পরিমাণ শূন্য হতে পারবে না।");
        Medicine m = get(id);
        int after = m.getStock() + delta;
        if (after < 0) throw new BusinessException("স্টক ঋণাত্মক হতে পারবে না।");
        m.setStock(after);
        StockAdjustment a = new StockAdjustment();
        a.setMedicine(m); a.setDelta(delta); a.setStockAfter(after);
        a.setReason(reason.trim()); a.setPerformedBy(performedBy);
        adjustments.save(a);
    }
}
