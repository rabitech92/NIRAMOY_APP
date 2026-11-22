package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.Role;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.repository.BranchRepository;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.BranchService;
import com.niramoy.pharmacy.service.SettingService;
import com.niramoy.pharmacy.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService users;
    private final BranchService branchService;
    private final BranchRepository branches;
    private final SettingService settings;
    private final BranchScope scope;

    // ---------- users ----------
    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String filter,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        usersModel(q, filter, page, model);
        if (!model.containsAttribute("form")) model.addAttribute("form", new Forms.UserForm());
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("form") Forms.UserForm form, BindingResult result,
                             RedirectAttributes ra, Model model) {
        if (result.hasErrors()) { usersModel("", "", 0, model); return "admin/users"; }
        try {
            users.create(form, UserPrincipal.current());
            ra.addFlashAttribute("success", "ব্যবহারকারী তৈরি হয়েছে: " + form.getUsername());
            return "redirect:/admin/users";
        } catch (BusinessException e) {
            usersModel("", "", 0, model);
            model.addAttribute("error", e.getMessage());
            return "admin/users";
        }
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            users.toggleActive(id, UserPrincipal.current());
            ra.addFlashAttribute("success", "ব্যবহারকারীর অবস্থা বদলানো হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void usersModel(String q, String filter, int page, Model model) {
        model.addAttribute("page", users.search(scope.current(), filter, q, page));
        model.addAttribute("q", q);
        model.addAttribute("filter", filter);
        model.addAttribute("roles", Role.values());
        model.addAttribute("branchList", branches.findByActiveTrueOrderByNameAsc());
    }

    // ---------- branches ----------
    @GetMapping("/branches")
    public String branches(Model model) {
        model.addAttribute("list", branchService.all());
        if (!model.containsAttribute("form")) model.addAttribute("form", new Forms.BranchForm());
        return "admin/branches";
    }

    @PostMapping("/branches")
    public String createBranch(@Valid @ModelAttribute("form") Forms.BranchForm form, BindingResult result,
                               RedirectAttributes ra, Model model) {
        model.addAttribute("list", branchService.all());
        if (result.hasErrors()) return "admin/branches";
        try {
            branchService.create(form);
            ra.addFlashAttribute("success", "শাখা যোগ হয়েছে।");
            return "redirect:/admin/branches";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/branches";
        }
    }

    @PostMapping("/branches/{id}/toggle")
    public String toggleBranch(@PathVariable Long id, RedirectAttributes ra) {
        try {
            branchService.toggle(id);
            ra.addFlashAttribute("success", "শাখার অবস্থা বদলানো হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/branches";
    }

    // ---------- settings ----------
    @GetMapping("/settings")
    public String settingsPage(Model model) {
        if (!model.containsAttribute("form")) {
            Forms.SettingsForm f = new Forms.SettingsForm();
            f.setVatPercent(settings.vat());
            f.setInvoicePrefix(settings.invoicePrefix());
            model.addAttribute("form", f);
        }
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String saveSettings(@Valid @ModelAttribute("form") Forms.SettingsForm form, BindingResult result,
                               RedirectAttributes ra) {
        if (result.hasErrors()) return "admin/settings";
        settings.update(form.getVatPercent(), form.getInvoicePrefix());
        ra.addFlashAttribute("success", "সেটিংস সংরক্ষিত হয়েছে।");
        return "redirect:/admin/settings";
    }
}
