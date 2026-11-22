package com.niramoy.pharmacy.config;

import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.SubscriptionService;
import com.niramoy.pharmacy.service.SubscriptionStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** সাবস্ক্রিপশনের মেয়াদ (গ্রেস সহ) শেষ হলে লগইন করা ব্যবহারকারীকে "মেয়াদ শেষ" পেজে পাঠায় */
@Component
@RequiredArgsConstructor
public class SubscriptionInterceptor implements HandlerInterceptor {
    private final SubscriptionService subscriptions;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (UserPrincipal.current() == null) return true;       // লগইনের আগে Security নিজে সামলায়
        SubscriptionStatus st = subscriptions.status();
        if (!st.isBlocked()) return true;

        String ctx = request.getContextPath();
        String uri = request.getRequestURI();
        if (uri.startsWith(ctx + "/pos/api/") || uri.startsWith(ctx + "/api/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"সাবস্ক্রিপশনের মেয়াদ শেষ। নবায়ন করুন।\",\"code\":\"SUBSCRIPTION_EXPIRED\"}");
            return false;
        }
        response.sendRedirect(ctx + "/subscription/expired");
        return false;
    }
}
