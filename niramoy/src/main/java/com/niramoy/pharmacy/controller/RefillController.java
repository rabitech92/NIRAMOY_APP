package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.CustomerSubscription;
import com.niramoy.pharmacy.entity.PaymentMethod;
import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.RefillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/refills")
@RequiredArgsConstructor
public class RefillController {
    private final RefillService service;
    private final BranchScope scope;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String filter,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", service.search(scope.current(), q, filter, page));
        model.addAttribute("q", q);
        model.addAttribute("filter", filter);
        return "refills/list";
    }

    @GetMapping("/new")
    public String form(Model model) {
        Forms.RefillForm f = new Forms.RefillForm();
        f.setStartDate(LocalDate.now());
        for (int i = 0; i < 3; i++) f.getItems().add(new Forms.RefillItemRow());
        model.addAttribute("form", f);
        model.addAttribute("units", RefillService.UNITS);
        return "refills/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") Forms.RefillForm form, BindingResult result,
                         RedirectAttributes ra, Model model) {
        model.addAttribute("units", RefillService.UNITS);
        if (form.getItems().isEmpty()) form.getItems().add(new Forms.RefillItemRow());
        if (result.hasErrors()) return "refills/form";
        try {
            CustomerSubscription s = service.create(form, scope.require(), UserPrincipal.current());
            ra.addFlashAttribute("success", "সাবস্ক্রিপশন তৈরি হয়েছে: " + s.getCustomerName());
            return "redirect:/refills/" + s.getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return "refills/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("s", service.get(id));
        List<PaymentMethod> methods = Arrays.stream(PaymentMethod.values())
                .filter(m -> m != PaymentMethod.CREDIT).toList();
        model.addAttribute("methods", methods);
        return "refills/detail";
    }

    @PostMapping("/{id}/bill")
    public String bill(@PathVariable Long id, @RequestParam PaymentMethod method, RedirectAttributes ra) {
        try {
            Sale sale = service.bill(id, method, UserPrincipal.current(), scope.require());
            ra.addFlashAttribute("success", "রিফিলের বিল তৈরি হয়েছে: " + sale.getInvoiceNo());
            return "redirect:/sales/" + sale.getId();
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/refills/" + id;
        }
    }

    @PostMapping("/{id}/pause")
    public String pause(@PathVariable Long id, RedirectAttributes ra) { return act(id, ra, () -> service.pause(id), "বিরতিতে রাখা হয়েছে।"); }

    @PostMapping("/{id}/resume")
    public String resume(@PathVariable Long id, RedirectAttributes ra) { return act(id, ra, () -> service.resume(id), "আবার চালু করা হয়েছে।"); }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) { return act(id, ra, () -> service.cancel(id), "সাবস্ক্রিপশন বাতিল করা হয়েছে।"); }

    @PostMapping("/{id}/next-date")
    public String nextDate(@PathVariable Long id,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           RedirectAttributes ra) {
        return act(id, ra, () -> service.changeNextDate(id, date), "পরের রিফিলের তারিখ বদলানো হয়েছে।");
    }

    private String act(Long id, RedirectAttributes ra, Runnable action, String okMessage) {
        try {
            action.run();
            ra.addFlashAttribute("success", okMessage);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/refills/" + id;
    }
}
