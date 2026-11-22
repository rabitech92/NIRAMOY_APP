package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.BusinessSummaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** ক্রয়-বিক্রয় ও স্টকের হিসাব: স্টকে কত টাকার মাল, মাসে/বছরে কত ক্রয়-বিক্রয় ও লাভ */
@Controller
@RequestMapping("/business")
@RequiredArgsConstructor
public class BusinessSummaryController {
    private final BusinessSummaryService service;
    private final BranchScope scope;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("s", service.build(scope.current()));
        if (!model.containsAttribute("purchaseForm")) {
            Forms.ManualPurchaseForm f = new Forms.ManualPurchaseForm();
            f.setDate(LocalDate.now());
            model.addAttribute("purchaseForm", f);
        }
        return "business/index";
    }

    @PostMapping("/purchases")
    public String addPurchase(@Valid @ModelAttribute("purchaseForm") Forms.ManualPurchaseForm form, BindingResult result,
                              RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/business";
        }
        try {
            service.addManualPurchase(form, scope.require(), UserPrincipal.current());
            ra.addFlashAttribute("success", "ক্রয়ের হিসাব যোগ হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business";
    }
}
