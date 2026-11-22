package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.PaymentMethod;
import com.niramoy.pharmacy.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosController {
    private final SettingService settings;

    @GetMapping
    public String pos(Model model) {
        model.addAttribute("vat", settings.vat());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "pos";
    }
}
