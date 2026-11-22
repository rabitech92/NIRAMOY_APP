package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** সফটওয়্যার সাবস্ক্রিপশন (প্ল্যান, নবায়নের অনুরোধ, যাচাই) */
@Controller
@RequestMapping("/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService service;

    @Value("${niramoy.subscription.pay-info:বিকাশ/নগদ/রকেট (পার্সোনাল): 01XXXXXXXXX — রেফারেন্সে আপনার দোকানের নাম লিখুন।}")
    private String payInfo;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("st", service.status());
        model.addAttribute("plans", service.activePlans());
        model.addAttribute("history", service.history());
        model.addAttribute("methods", SubscriptionService.PAY_METHODS);
        model.addAttribute("payInfo", payInfo);
        return "subscription/status";
    }

    @GetMapping("/expired")
    public String expired(Model model) {
        model.addAttribute("st", service.status());
        return "subscription/expired";
    }

    @PostMapping("/request")
    public String request(@Valid @ModelAttribute Forms.SubscriptionRequestForm form, BindingResult result,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/subscription";
        }
        try {
            service.request(form, UserPrincipal.current());
            ra.addFlashAttribute("success", "নবায়নের অনুরোধ পাঠানো হয়েছে। পেমেন্ট যাচাই হলে সাবস্ক্রিপশন সক্রিয় হবে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/subscription";
    }

    // ---------- শুধু সুপার অ্যাডমিন ----------
    @GetMapping("/manage")
    public String manage(Model model) {
        model.addAttribute("st", service.status());
        model.addAttribute("pending", service.pending());
        model.addAttribute("allPlans", service.allPlans());
        model.addAttribute("history", service.history());
        model.addAttribute("methods", SubscriptionService.PAY_METHODS);
        return "subscription/manage";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        return run(ra, () -> service.approve(id, UserPrincipal.current()), "সাবস্ক্রিপশন সক্রিয় করা হয়েছে।");
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(defaultValue = "") String reason, RedirectAttributes ra) {
        return run(ra, () -> service.reject(id, reason, UserPrincipal.current()), "অনুরোধ প্রত্যাখ্যান করা হয়েছে।");
    }

    @PostMapping("/grant")
    public String grant(@Valid @ModelAttribute Forms.SubscriptionRequestForm form, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/subscription/manage";
        }
        return run(ra, () -> service.grant(form, UserPrincipal.current()), "সাবস্ক্রিপশন সরাসরি সক্রিয় করা হয়েছে।");
    }

    @PostMapping("/plans/{id}")
    public String updatePlan(@PathVariable Long id, @Valid @ModelAttribute Forms.PlanForm form, BindingResult result,
                             RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "প্ল্যানের তথ্য সঠিক নয়।");
            return "redirect:/subscription/manage";
        }
        return run(ra, () -> service.updatePlan(id, form), "প্ল্যান হালনাগাদ হয়েছে।");
    }

    private String run(RedirectAttributes ra, Runnable action, String okMessage) {
        try {
            action.run();
            ra.addFlashAttribute("success", okMessage);
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/subscription/manage";
    }
}
