package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.api.ApiDtos.*;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.service.CustomerInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** মোবাইল অ্যাপের গ্রাহক বিশ্লেষণ API (শুধু SUPER_ADMIN / OWNER / BRANCH_MANAGER) */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerApiController {
    private final CustomerInsightService service;
    private final BranchScope scope;

    @GetMapping
    public CustomerListDto list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "total") String sort,
                                @RequestParam(defaultValue = "0") int page) {
        CustomerInsightService.CustomerList d = service.list(scope.current(), q, "recent".equals(sort) ? "recent" : "total", page);
        return new CustomerListDto(
                d.rows().stream().map(r -> new CustomerRowDto(r.phone(), r.name(), r.invoices(), r.lifetime(), r.lastAt(),
                        r.due(), r.today(), r.week(), r.month(), r.year())).toList(),
                d.page(), d.totalPages(), d.totalCustomers(), d.allCustomers(), d.allLifetime(), d.allAverage());
    }

    @GetMapping("/{phone}")
    public CustomerDetailDto detail(@PathVariable String phone) {
        if (!phone.matches("\\d{6,15}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        CustomerInsightService.Detail c = service.detail(scope.current(), phone);
        return new CustomerDetailDto(c.phone(), c.name(), c.invoices(), c.lifetime(), c.average(), c.due(), c.firstAt(),
                c.lastAt(), c.today(), c.week(), c.month(), c.year(),
                c.daily().stream().map(b -> new BucketDto(b.label(), b.invoices(), b.amount())).toList(),
                c.weekly().stream().map(b -> new BucketDto(b.label(), b.invoices(), b.amount())).toList(),
                c.monthly().stream().map(b -> new BucketDto(b.label(), b.invoices(), b.amount())).toList(),
                c.yearly().stream().map(b -> new BucketDto(b.label(), b.invoices(), b.amount())).toList(),
                c.topItems().stream().map(t -> new TopItemDto(t.name(), t.pieces(), t.amount(), t.times())).toList(),
                c.recent().stream().map(s -> ApiMapper.sale(s, true)).toList());
    }
}
