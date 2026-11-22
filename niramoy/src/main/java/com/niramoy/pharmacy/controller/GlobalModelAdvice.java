package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.Branch;
import com.niramoy.pharmacy.repository.BranchRepository;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.SubscriptionService;
import com.niramoy.pharmacy.service.SubscriptionStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** প্রতিটি পেজে লেআউটের জন্য প্রয়োজনীয় সাধারণ তথ্য যোগ করে। */
@ControllerAdvice(assignableTypes = {AuthController.class, DashboardController.class, InventoryController.class,
        PosController.class, SalesController.class, ReturnsController.class, PurchaseController.class,
        DuesController.class, ReportController.class, AdminController.class,
        RefillController.class, SubscriptionController.class, ExpenseController.class,
        CustomerInsightController.class, BestSellerController.class, BusinessSummaryController.class})
@RequiredArgsConstructor
public class GlobalModelAdvice {
    private final BranchRepository branches;
    private final BranchScope scope;
    private final SubscriptionService subscriptions;

    @ModelAttribute("allBranches")
    public List<Branch> allBranches() { return branches.findByActiveTrueOrderByNameAsc(); }

    @ModelAttribute("activeBranchId")
    public Long activeBranchId() { return scope.current(); }

    @ModelAttribute("me")
    public UserPrincipal me() { return UserPrincipal.current(); }

    @ModelAttribute("uri")
    public String uri(HttpServletRequest request) { return request.getRequestURI(); }

    @ModelAttribute("subStatus")
    public SubscriptionStatus subStatus() { return subscriptions.status(); }
}
