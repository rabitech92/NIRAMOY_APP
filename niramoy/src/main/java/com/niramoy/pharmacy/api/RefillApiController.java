package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.api.ApiDtos.*;
import com.niramoy.pharmacy.entity.CustomerSubscription;
import com.niramoy.pharmacy.entity.PaymentMethod;
import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.RefillService;
import com.niramoy.pharmacy.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/refills")
@RequiredArgsConstructor
public class RefillApiController {
    private final RefillService refills;
    private final SaleService sales;
    private final BranchScope scope;

    @GetMapping
    public PageDto<RefillDto> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String filter,
                                   @RequestParam(defaultValue = "0") int page) {
        return ApiMapper.page(refills.search(scope.current(), q, filter, page), ApiMapper::refill);
    }

    @GetMapping("/{id}")
    public RefillDto detail(@PathVariable Long id) {
        return ApiMapper.refill(load(id));
    }

    public record BillRequest(PaymentMethod method) { }

    @PostMapping("/{id}/bill")
    public SaleDto bill(@PathVariable Long id, @RequestBody BillRequest req) {
        load(id);
        Sale sale = refills.bill(id, req.method(), UserPrincipal.current(), scope.require());
        return ApiMapper.sale(sales.getWithItems(sale.getId()), true);
    }

    private CustomerSubscription load(Long id) {
        CustomerSubscription s = refills.get(id);
        Long b = scope.current();
        if (b != null && !b.equals(s.getBranch().getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return s;
    }
}
