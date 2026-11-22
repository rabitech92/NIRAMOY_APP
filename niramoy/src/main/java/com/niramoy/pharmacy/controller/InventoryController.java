package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.Medicine;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final MedicineService service;
    private final BranchScope scope;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "ALL") String filter,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        Page<Medicine> result = service.search(scope.current(), q, filter, page);
        model.addAttribute("page", result);
        model.addAttribute("q", q);
        model.addAttribute("filter", filter);
        return "inventory/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("form", new Forms.MedicineForm());
        return "inventory/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") Forms.MedicineForm form, BindingResult result,
                       RedirectAttributes ra, Model model) {
        if (result.hasErrors()) return "inventory/form";
        try {
            Medicine m = service.create(form, scope.require());
            ra.addFlashAttribute("success", "ওষুধ যোগ হয়েছে: " + m.getName());
            return "redirect:/inventory/" + m.getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "inventory/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Medicine m = service.get(id);
        model.addAttribute("m", m);
        model.addAttribute("adjustments", service.recentAdjustments(id));
        if (!model.containsAttribute("adjustForm")) model.addAttribute("adjustForm", new Forms.StockAdjustForm());
        return "inventory/detail";
    }

    @PostMapping("/{id}/adjust")
    public String adjust(@PathVariable Long id, @Valid @ModelAttribute("adjustForm") Forms.StockAdjustForm form,
                         BindingResult result, RedirectAttributes ra, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("m", service.get(id));
            model.addAttribute("adjustments", service.recentAdjustments(id));
            return "inventory/detail";
        }
        try {
            service.adjust(id, form.getDelta(), form.getReason(), UserPrincipal.current().getFullName());
            ra.addFlashAttribute("success", "স্টক সমন্বয় সম্পন্ন হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory/" + id;
    }
}
