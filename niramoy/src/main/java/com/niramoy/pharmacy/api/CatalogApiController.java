package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.api.ApiDtos.*;
import com.niramoy.pharmacy.entity.Medicine;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.BestSellerService;
import com.niramoy.pharmacy.service.DashboardService;
import com.niramoy.pharmacy.service.MedicineService;
import com.niramoy.pharmacy.service.RefillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogApiController {
    private final DashboardService dashboard;
    private final RefillService refills;
    private final MedicineService medicines;
    private final BranchScope scope;
    private final BestSellerService marks;

    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        UserPrincipal u = UserPrincipal.current();
        Long branchId = scope.current();
        DashboardService.DashboardView d = dashboard.build(branchId);
        boolean money = ApiMapper.canSeeMoney(u);
        List<TrendPoint> trend = new java.util.ArrayList<>();
        for (int i = 0; i < d.trendLabels().size(); i++) trend.add(new TrendPoint(d.trendLabels().get(i), d.trendValues().get(i)));
        return new DashboardDto(d.todaySales(), money ? d.todayProfit() : null, money ? d.receivable() : null,
                money ? d.supplierDue() : null, d.lowStock(), d.expiring(), d.medicineCount(), d.branchCount(),
                refills.dueCount(branchId), trend,
                d.paymentMix().stream().map(m -> new MixPoint(m.label(), m.percent())).toList(),
                d.recentSales().stream().map(s -> ApiMapper.sale(s, false)).toList());
    }

    @GetMapping("/medicines")
    public PageDto<MedicineDto> medicines(@RequestParam(defaultValue = "") String q,
                                          @RequestParam(defaultValue = "ALL") String filter,
                                          @RequestParam(defaultValue = "0") int page) {
        boolean cost = ApiMapper.canSeeMoney(UserPrincipal.current()) || ApiMapper.hasRole(UserPrincipal.current(),
                com.niramoy.pharmacy.entity.Role.INVENTORY_MANAGER);
        return ApiMapper.page(medicines.search(scope.current(), q, filter, page), m -> ApiMapper.medicine(m, cost, marks.isMarked(m.getName())));
    }

    @GetMapping("/medicines/{id}")
    public MedicineDto medicine(@PathVariable Long id) {
        Medicine m = medicines.get(id);
        Long b = scope.current();
        if (b != null && !b.equals(m.getBranch().getId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        boolean cost = ApiMapper.canSeeMoney(UserPrincipal.current()) || ApiMapper.hasRole(UserPrincipal.current(),
                com.niramoy.pharmacy.entity.Role.INVENTORY_MANAGER);
        return ApiMapper.medicine(m, cost, marks.isMarked(m.getName()));
    }
}
