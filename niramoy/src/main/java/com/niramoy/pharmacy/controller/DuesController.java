package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.PaymentMethod;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.SaleRepository;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.PurchaseService;
import com.niramoy.pharmacy.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/dues")
@RequiredArgsConstructor
public class DuesController {
    private final PurchaseService purchases;
    private final SaleService sales;
    private final SaleRepository saleRepository;
    private final BranchScope scope;

    @GetMapping
    public String index(Model model) {
        Long branchId = scope.current();
        BigDecimal receivable = saleRepository.totalReceivable(branchId);
        model.addAttribute("receivable", receivable == null ? BigDecimal.ZERO : receivable);
        model.addAttribute("supplierDue", purchases.totalSupplierDue());
        model.addAttribute("collectedToday", sales.collectedToday(branchId));
        model.addAttribute("suppliers", purchases.allSuppliers());
        model.addAttribute("openDues", sales.openDues(branchId));
        model.addAttribute("payments", purchases.recentPayments());
        model.addAttribute("methods", java.util.Arrays.stream(PaymentMethod.values())
                .filter(m -> m != PaymentMethod.CREDIT).toList());
        return "dues";
    }

    @PostMapping("/suppliers/{id}/pay")
    public String paySupplier(@PathVariable Long id, @Valid Forms.AmountForm form, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) { ra.addFlashAttribute("error", "সঠিক পরিমাণ দিন।"); return "redirect:/dues"; }
        try {
            purchases.paySupplier(id, form, UserPrincipal.current().getFullName());
            ra.addFlashAttribute("success", "সরবরাহকারী পেমেন্ট নথিভুক্ত হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dues";
    }

    @PostMapping("/sales/{id}/collect")
    public String collect(@PathVariable Long id, @Valid Forms.AmountForm form, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) { ra.addFlashAttribute("error", "সঠিক পরিমাণ দিন।"); return "redirect:/dues"; }
        try {
            sales.collectDue(id, form.getAmount(), form.getMethod(), UserPrincipal.current());
            ra.addFlashAttribute("success", "বকেয়া আদায় নথিভুক্ত হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dues";
    }
}
