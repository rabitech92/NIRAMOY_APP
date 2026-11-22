package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.service.CustomerInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** গ্রাহক বিশ্লেষণ: কে জীবনকালে কত টাকার ওষুধ কিনেছেন + দৈনিক/সাপ্তাহিক/বার্ষিক হিসাব */
@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerInsightController {
    private final CustomerInsightService service;
    private final BranchScope scope;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "total") String sort,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        String s = "recent".equals(sort) ? "recent" : "total";
        model.addAttribute("data", service.list(scope.current(), q, s, page));
        model.addAttribute("q", q);
        model.addAttribute("sort", s);
        return "customers/list";
    }

    @GetMapping("/{phone}")
    public String detail(@PathVariable String phone, Model model) {
        if (!phone.matches("\\d{6,15}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        model.addAttribute("c", service.detail(scope.current(), phone));
        return "customers/detail";
    }
}
