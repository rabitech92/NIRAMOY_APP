package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.api.ApiDtos.*;
import com.niramoy.pharmacy.entity.PaymentMethod;
import com.niramoy.pharmacy.entity.Sale;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.BestSellerService;
import com.niramoy.pharmacy.service.CheckoutRequest;
import com.niramoy.pharmacy.service.MedicineService;
import com.niramoy.pharmacy.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SalesApiController {
    private final MedicineService medicines;
    private final SaleService sales;
    private final BranchScope scope;
    private final BestSellerService marks;

    // ---------- POS ----------
    @GetMapping("/pos/search")
    public List<MedicineDto> posSearch(@RequestParam(defaultValue = "") String q) {
        Long branchId = scope.require().getId();
        return medicines.posSearch(branchId, q).stream().map(m -> ApiMapper.medicine(m, false, marks.isMarked(m.getName()))).toList();
    }

    @PostMapping("/pos/checkout")
    public SaleDto checkout(@RequestBody CheckoutRequest request) {
        Sale sale = sales.checkout(request, UserPrincipal.current(), scope.require());
        return ApiMapper.sale(sales.getWithItems(sale.getId()), true);
    }

    // ---------- ইনভয়েস ----------
    @GetMapping("/sales")
    public PageDto<SaleDto> list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String status,
                                 @RequestParam(defaultValue = "0") int page) {
        return ApiMapper.page(sales.search(scope.current(), q, status, page), s -> ApiMapper.sale(s, false));
    }

    @GetMapping("/sales/{id}")
    public SaleDto detail(@PathVariable Long id) {
        Sale s = sales.getWithItems(id);
        assertBranch(s);
        return ApiMapper.sale(s, true);
    }

    // ---------- বকেয়া ----------
    @GetMapping("/dues")
    public DuesDto dues() {
        List<Sale> list = sales.openDues(scope.current());
        BigDecimal total = list.stream().map(Sale::getDueAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DuesDto(total, list.stream().map(s -> ApiMapper.sale(s, false)).toList());
    }

    public record CollectRequest(BigDecimal amount, PaymentMethod method) { }

    @PostMapping("/dues/{id}/collect")
    public SaleDto collect(@PathVariable Long id, @RequestBody CollectRequest req) {
        assertBranch(sales.getWithItems(id));
        sales.collectDue(id, req.amount(), req.method(), UserPrincipal.current());
        return ApiMapper.sale(sales.getWithItems(id), true);
    }

    private void assertBranch(Sale s) {
        Long b = scope.current();
        if (b != null && !b.equals(s.getBranch().getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
