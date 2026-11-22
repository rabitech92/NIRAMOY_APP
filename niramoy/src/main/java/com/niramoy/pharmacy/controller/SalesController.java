package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.SaleStatus;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {
    private final SaleService service;
    private final BranchScope scope;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String filter,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", service.search(scope.current(), q, filter, page));
        model.addAttribute("q", q);
        model.addAttribute("filter", filter);
        model.addAttribute("statuses", SaleStatus.values());
        return "sales/list";
    }

    @GetMapping("/{id}")
    public String invoice(@PathVariable Long id, Model model) {
        model.addAttribute("sale", service.getWithItems(id));
        return "sales/invoice";
    }
}
