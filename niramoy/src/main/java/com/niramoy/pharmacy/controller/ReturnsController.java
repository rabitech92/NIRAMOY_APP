package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.PaymentMethod;
import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/returns")
@RequiredArgsConstructor
public class ReturnsController {
    private final SaleService service;
    private final BranchScope scope;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "COMPLETED") String filter,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", service.search(scope.current(), q, filter, page));
        model.addAttribute("recentReturns", service.recentReturns(scope.current()));
        model.addAttribute("q", q);
        model.addAttribute("filter", filter);
        return "returns/list";
    }

    @GetMapping("/{saleId}")
    public String form(@PathVariable Long saleId, Model model) {
        prepare(saleId, model);
        model.addAttribute("form", new Forms.ReturnForm());
        return "returns/form";
    }

    @PostMapping("/{saleId}")
    public String submit(@PathVariable Long saleId, @Valid @ModelAttribute("form") Forms.ReturnForm form,
                         BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) { prepare(saleId, model); return "returns/form"; }
        try {
            Sale s = service.processReturn(saleId, form, UserPrincipal.current());
            ra.addFlashAttribute("success", s.getInvoiceNo() + " ইনভয়েসের ফেরত সম্পন্ন হয়েছে।");
            return "redirect:/returns";
        } catch (BusinessException e) {
            prepare(saleId, model);
            model.addAttribute("error", e.getMessage());
            return "returns/form";
        }
    }

    private void prepare(Long saleId, Model model) {
        model.addAttribute("sale", service.getWithItems(saleId));
        model.addAttribute("paymentMethods", PaymentMethod.values());
    }
}
