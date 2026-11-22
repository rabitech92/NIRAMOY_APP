package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.security.BranchScope;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final BranchScope scope;

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/access-denied")
    public String denied() { return "access-denied"; }

    /** ওপরের বারের শাখা-সুইচার (শুধু SUPER_ADMIN / OWNER-এর জন্য কার্যকর) */
    @PostMapping("/branch/switch")
    public String switchBranch(@RequestParam(required = false) Long branchId,
                               @RequestParam(defaultValue = "/") String redirect, HttpSession session) {
        scope.switchTo(branchId, session);
        // ওপেন-রিডাইরেক্ট প্রতিরোধ: শুধু অভ্যন্তরীণ পাথ
        boolean safe = redirect.startsWith("/") && !redirect.startsWith("//") && !redirect.contains("\\");
        return "redirect:" + (safe ? redirect : "/");
    }
}
