package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.Expense;
import com.niramoy.pharmacy.entity.ExpenseGroup;
import com.niramoy.pharmacy.entity.ExpenseStatus;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService service;
    private final BranchScope scope;

    // ---------- ড্যাশবোর্ড ----------
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("d", service.dashboard(scope.current()));
        return "expenses/dashboard";
    }

    // ---------- তালিকা ও রিপোর্ট ----------
    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(defaultValue = "") String status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "false") boolean voided,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        LocalDate today = LocalDate.now();
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        if (start.isAfter(end)) { LocalDate t = start; start = end; end = t; }

        model.addAttribute("result", service.search(scope.current(), q, categoryId, status, start, end, voided, page));
        model.addAttribute("categories", service.allCategories());
        model.addAttribute("statuses", ExpenseStatus.values());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("voided", voided);
        // দ্রুত তারিখ বাছাই: আজ / এই সপ্তাহ / এই মাস / এই বছর
        model.addAttribute("today", today);
        model.addAttribute("weekStart", today.with(DayOfWeek.SATURDAY).isAfter(today)
                ? today.with(DayOfWeek.SATURDAY).minusWeeks(1) : today.with(DayOfWeek.SATURDAY));
        model.addAttribute("monthStart", today.withDayOfMonth(1));
        model.addAttribute("yearStart", LocalDate.of(today.getYear(), 1, 1));
        return "expenses/list";
    }

    // ---------- নতুন / সম্পাদনা ----------
    @GetMapping("/new")
    public String form(Model model) {
        Forms.ExpenseForm f = new Forms.ExpenseForm();
        f.setExpenseDate(LocalDate.now());
        model.addAttribute("form", f);
        formModel(model, false, null);
        return "expenses/form";
    }

    @PostMapping("/save")
    public String create(@Valid @ModelAttribute("form") Forms.ExpenseForm form, BindingResult result,
                         RedirectAttributes ra, Model model) {
        formModel(model, false, null);
        if (result.hasErrors()) return "expenses/form";
        try {
            Expense e = service.create(form, scope.require(), UserPrincipal.current());
            ra.addFlashAttribute("success", "খরচ নথিভুক্ত হয়েছে: " + e.getExpenseNo());
            return "redirect:/expenses/" + e.getId();
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "expenses/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("e", service.get(id, scope.current()));
        if (!model.containsAttribute("payForm")) model.addAttribute("payForm", new Forms.ExpensePayForm());
        model.addAttribute("payMethods", ExpenseService.PAY_METHODS);
        return "expenses/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Expense e = service.get(id, scope.current());
        Forms.ExpenseForm f = new Forms.ExpenseForm();
        f.setExpenseDate(e.getExpenseDate());
        f.setCategoryId(e.getCategory().getId());
        f.setSubCategoryId(e.getSubCategory() == null ? null : e.getSubCategory().getId());
        f.setAmount(e.getAmount());
        f.setPaidTo(e.getPaidTo());
        f.setReferenceNo(e.getReferenceNo());
        f.setDescription(e.getDescription());
        model.addAttribute("form", f);
        model.addAttribute("e", e);
        formModel(model, true, id);
        return "expenses/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") Forms.ExpenseForm form,
                         BindingResult result, RedirectAttributes ra, Model model) {
        formModel(model, true, id);
        model.addAttribute("e", service.get(id, scope.current()));
        if (result.hasErrors()) return "expenses/form";
        try {
            service.update(id, form, scope.current(), UserPrincipal.current());
            ra.addFlashAttribute("success", "খরচ হালনাগাদ হয়েছে।");
            return "redirect:/expenses/" + id;
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "expenses/form";
        }
    }

    // ---------- পেমেন্ট ও বাতিল ----------
    @PostMapping("/{id}/pay")
    public String pay(@PathVariable Long id, @Valid @ModelAttribute("payForm") Forms.ExpensePayForm form,
                      BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "সঠিক পরিমাণ ও পদ্ধতি দিন।");
            return "redirect:/expenses/" + id;
        }
        try {
            service.pay(id, form, scope.current(), UserPrincipal.current());
            ra.addFlashAttribute("success", "পেমেন্ট নথিভুক্ত হয়েছে।");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/expenses/" + id;
    }

    @PostMapping("/{id}/void")
    public String voidExpense(@PathVariable Long id, @RequestParam(defaultValue = "") String reason, RedirectAttributes ra) {
        try {
            service.voidExpense(id, reason, scope.current(), UserPrincipal.current());
            ra.addFlashAttribute("success", "খরচ বাতিল করা হয়েছে (হিসাব থেকে বাদ)।");
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/expenses/" + id;
    }

    // ---------- ক্যাটাগরি (শুধু সুপার অ্যাডমিন/মালিক) ----------
    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("categories", service.allCategories());
        model.addAttribute("subCategories", service.allSubCategories());
        model.addAttribute("groups", ExpenseGroup.values());
        if (!model.containsAttribute("catForm")) model.addAttribute("catForm", new Forms.CategoryForm());
        if (!model.containsAttribute("subForm")) model.addAttribute("subForm", new Forms.SubCategoryForm());
        return "expenses/categories";
    }

    @PostMapping("/categories")
    public String createCategory(@Valid @ModelAttribute("catForm") Forms.CategoryForm form, BindingResult result,
                                 RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/expenses/categories";
        }
        return run(ra, () -> service.createCategory(form), "ক্যাটাগরি যোগ হয়েছে।");
    }

    @PostMapping("/categories/{id}/toggle")
    public String toggleCategory(@PathVariable Long id, RedirectAttributes ra) {
        return run(ra, () -> service.toggleCategory(id), "ক্যাটাগরির অবস্থা বদলানো হয়েছে।");
    }

    @PostMapping("/categories/sub")
    public String createSub(@Valid @ModelAttribute("subForm") Forms.SubCategoryForm form, BindingResult result,
                            RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/expenses/categories";
        }
        return run(ra, () -> service.createSubCategory(form), "উপ-ক্যাটাগরি যোগ হয়েছে।");
    }

    @PostMapping("/categories/sub/{id}/toggle")
    public String toggleSub(@PathVariable Long id, RedirectAttributes ra) {
        return run(ra, () -> service.toggleSubCategory(id), "উপ-ক্যাটাগরির অবস্থা বদলানো হয়েছে।");
    }

    private String run(RedirectAttributes ra, Runnable action, String ok) {
        try {
            action.run();
            ra.addFlashAttribute("success", ok);
        } catch (BusinessException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/expenses/categories";
    }

    private void formModel(Model model, boolean edit, Long id) {
        model.addAttribute("categories", service.activeCategories());
        model.addAttribute("groups", ExpenseGroup.values());
        model.addAttribute("subCategories", service.activeSubCategories());
        model.addAttribute("payMethods", ExpenseService.PAY_METHODS);
        model.addAttribute("statuses", ExpenseStatus.values());
        model.addAttribute("edit", edit);
        model.addAttribute("expenseId", id);
    }
}
