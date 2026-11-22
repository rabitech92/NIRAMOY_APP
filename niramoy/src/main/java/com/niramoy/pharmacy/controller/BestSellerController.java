package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.exception.BusinessException;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.BestSellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/** কোন ওষুধ বেশি বিক্রি হয় তার তালিকা এবং "বেশি বিক্রিত" চিহ্নিত করে রাখা */
@Controller
@RequestMapping("/best-sellers")
@RequiredArgsConstructor
public class BestSellerController {
    private static final List<Integer> DAY_OPTIONS = List.of(7, 30, 90, 365, 0);
    private static final List<String> SORTS = List.of("qty", "revenue", "orders");

    private final BestSellerService service;
    private final BranchScope scope;

    @GetMapping
    public String index(@RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "qty") String sort,
                        @RequestParam(defaultValue = "20") int limit, Model model) {
        int d = day(days);
        String s = sort(sort);
        int l = Math.max(5, Math.min(limit, 100));
        LocalDate[] r = range(d);
        model.addAttribute("rows", service.ranking(scope.current(), r[0], r[1], s, l));
        model.addAttribute("marks", service.allMarks());
        model.addAttribute("days", d);
        model.addAttribute("sort", s);
        model.addAttribute("limit", l);
        model.addAttribute("periodLabel", label(d));
        model.addAttribute("dayOptions", DAY_OPTIONS);
        return "best-sellers/index";
    }

    @PostMapping("/mark")
    public String mark(@RequestParam String name, @RequestParam(defaultValue = "30") int days,
                       @RequestParam(defaultValue = "qty") String sort, @RequestParam(defaultValue = "20") int limit,
                       RedirectAttributes ra) {
        int d = day(days);
        LocalDate[] r = range(d);
        try {
            service.mark(scope.current(), name, r[0], r[1], label(d), UserPrincipal.current().getFullName());
            ra.addFlashAttribute("success", "'" + name + "' বেশি বিক্রিত হিসেবে চিহ্নিত হয়েছে।");
        } catch (BusinessException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return back(d, sort, limit);
    }

    @PostMapping("/mark-top")
    public String markTop(@RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "qty") String sort,
                          @RequestParam(defaultValue = "20") int limit, RedirectAttributes ra) {
        int d = day(days);
        LocalDate[] r = range(d);
        int n = service.markTop(scope.current(), r[0], r[1], sort(sort), Math.max(1, Math.min(limit, 100)), label(d),
                UserPrincipal.current().getFullName());
        ra.addFlashAttribute("success", n + "টি ওষুধ নতুন করে 'বেশি বিক্রিত' চিহ্নিত হয়েছে।");
        return back(d, sort, limit);
    }

    @PostMapping("/unmark")
    public String unmark(@RequestParam String name, @RequestParam(defaultValue = "30") int days,
                         @RequestParam(defaultValue = "qty") String sort, @RequestParam(defaultValue = "20") int limit,
                         RedirectAttributes ra) {
        service.unmark(name);
        ra.addFlashAttribute("success", "'" + name + "'-এর চিহ্ন তুলে নেওয়া হয়েছে।");
        return back(day(days), sort, limit);
    }

    private static int day(int days) { return DAY_OPTIONS.contains(days) ? days : 30; }
    private static String sort(String s) { return SORTS.contains(s) ? s : "qty"; }

    private static LocalDate[] range(int days) {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{days == 0 ? LocalDate.of(2000, 1, 1) : today.minusDays(days - 1L), today};
    }

    private static String label(int days) { return days == 0 ? "সব সময়" : "গত " + days + " দিন"; }

    private static String back(int days, String sort, int limit) {
        return "redirect:/best-sellers?days=" + days + "&sort=" + sort(sort) + "&limit=" + Math.max(5, Math.min(limit, 100));
    }
}
