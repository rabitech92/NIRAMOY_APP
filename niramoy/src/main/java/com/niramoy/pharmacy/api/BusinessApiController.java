package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.service.BusinessSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** মোবাইল অ্যাপের ক্রয়-বিক্রয় ও স্টক হিসাব (শুধু SUPER_ADMIN / OWNER / BRANCH_MANAGER) */
@RestController
@RequestMapping("/api/v1/business")
@RequiredArgsConstructor
public class BusinessApiController {
    private final BusinessSummaryService service;
    private final BranchScope scope;

    @GetMapping
    public BusinessSummaryService.Summary summary() {
        return service.build(scope.current());
    }
}
