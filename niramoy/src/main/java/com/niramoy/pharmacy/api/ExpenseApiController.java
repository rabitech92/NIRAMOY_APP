package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.api.ApiDtos.*;
import com.niramoy.pharmacy.entity.Expense;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.ExpenseService;
import com.niramoy.pharmacy.controller.Forms;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** মোবাইল অ্যাপের খরচ ব্যবস্থাপনা API (শুধু SUPER_ADMIN / OWNER / BRANCH_MANAGER) */
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseApiController {
    private final ExpenseService service;
    private final BranchScope scope;

    @GetMapping("/meta")
    public ExpenseMetaDto meta() {
        return new ExpenseMetaDto(
                service.activeCategories().stream()
                        .map(c -> new ExpenseCategoryDto(c.getId(), c.getName(), c.getNameEn(), c.getGroupType().getLabel())).toList(),
                service.activeSubCategories().stream()
                        .map(s -> new ExpenseSubDto(s.getId(), s.getCategory().getId(), s.getName())).toList());
    }

    @GetMapping("/dashboard")
    public ExpenseDashboardDto dashboard() {
        ExpenseService.Dashboard d = service.dashboard(scope.current());
        return new ExpenseDashboardDto(d.today(), d.month(), d.pending(), d.year(), d.monthLabel(), d.yearNumber(),
                d.byCategory().stream().map(s -> new SliceDto(s.label(), s.amount(), s.percent())).toList(),
                d.byPayment().stream().map(s -> new SliceDto(s.label(), s.amount(), s.percent())).toList(),
                d.recent().stream().map(e -> ApiMapper.expense(e, false)).toList());
    }

    @GetMapping
    public ExpenseListDto list(@RequestParam(defaultValue = "") String q,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(defaultValue = "") String status,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(defaultValue = "false") boolean voided,
                               @RequestParam(defaultValue = "0") int page) {
        LocalDate today = LocalDate.now();
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        if (start.isAfter(end)) { LocalDate t = start; start = end; end = t; }
        ExpenseService.ListResult r = service.search(scope.current(), q, categoryId, status, start, end, voided, page);
        return new ExpenseListDto(r.page().getContent().stream().map(e -> ApiMapper.expense(e, false)).toList(),
                r.page().getNumber(), r.page().getTotalPages(), r.page().getTotalElements(),
                r.totalAmount(), r.totalPaid());
    }

    @GetMapping("/{id}")
    public ExpenseDto detail(@PathVariable Long id) {
        return ApiMapper.expense(service.get(id, scope.current()), true);
    }

    @PostMapping
    public ExpenseDto create(@Valid @RequestBody Forms.ExpenseForm form) {
        Expense e = service.create(form, scope.require(), UserPrincipal.current());
        return ApiMapper.expense(service.get(e.getId(), scope.current()), true);
    }

    @PostMapping("/{id}/pay")
    public ExpenseDto pay(@PathVariable Long id, @Valid @RequestBody Forms.ExpensePayForm form) {
        service.pay(id, form, scope.current(), UserPrincipal.current());
        return ApiMapper.expense(service.get(id, scope.current()), true);
    }

    public record VoidRequest(String reason) { }

    @PostMapping("/{id}/void")
    public ExpenseDto voidExpense(@PathVariable Long id, @RequestBody VoidRequest req) {
        service.voidExpense(id, req.reason(), scope.current(), UserPrincipal.current());
        return ApiMapper.expense(service.get(id, scope.current()), true);
    }
}
