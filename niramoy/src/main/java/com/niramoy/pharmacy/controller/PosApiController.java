package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.Medicine;
import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.CheckoutRequest;
import com.niramoy.pharmacy.service.BestSellerService;
import com.niramoy.pharmacy.service.MedicineService;
import com.niramoy.pharmacy.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** POS-এর JSON এন্ডপয়েন্ট (CSRF টোকেন হেডারে পাঠাতে হয়)। */
@RestController
@RequestMapping("/pos/api")
@RequiredArgsConstructor
public class PosApiController {
    private final MedicineService medicines;
    private final SaleService sales;
    private final BranchScope scope;
    private final BestSellerService marks;

    public record MedicineView(Long id, String name, String barcode, String batchNo, String expiryDate,
                               BigDecimal price, BigDecimal stripPrice, int stock, int stripSize, String boxNo,
                               String genericName, String companyName, boolean bestSeller) { }

    @GetMapping("/search")
    public List<MedicineView> search(@RequestParam(defaultValue = "") String q) {
        Long branchId = scope.require().getId();
        return medicines.posSearch(branchId, q).stream().map(this::view).toList();
    }

    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody CheckoutRequest request) {
        Sale sale = sales.checkout(request, UserPrincipal.current(), scope.require());
        return Map.of("saleId", sale.getId(), "invoiceNo", sale.getInvoiceNo(), "total", sale.getTotal());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handle(BusinessException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    private MedicineView view(Medicine m) {
        return new MedicineView(m.getId(), m.getName(), m.getBarcode(), m.getBatchNo(),
                m.getExpiryDate().toString(), m.getSalePrice(), m.getEffectiveStripPrice(), m.getStock(), m.getStripSize(), m.getBoxNo(),
                m.getGenericName(), m.getCompanyName(), marks.isMarked(m.getName()));
    }
}
