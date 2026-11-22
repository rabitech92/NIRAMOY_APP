package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.service.DashboardService;
import com.niramoy.pharmacy.service.RefillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboard;
    private final BranchScope scope;
    private final RefillService refills;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("d", dashboard.build(scope.current()));
        model.addAttribute("dueRefills", refills.dueCount(scope.current()));
        return "dashboard";
    }
}
