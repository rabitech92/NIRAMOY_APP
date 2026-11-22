package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.api.ApiDtos.*;
import com.niramoy.pharmacy.security.BranchScope;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.BestSellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** মোবাইল অ্যাপের "বেশি বিক্রিত ওষুধ" API: র‍্যাংকিং ও চিহ্নিত করা */
@RestController
@RequestMapping("/api/v1/best-sellers")
@RequiredArgsConstructor
public class BestSellerApiController {
    private static final List<Integer> DAY_OPTIONS = List.of(7, 30, 90, 365, 0);
    private static final List<String> SORTS = List.of("qty", "revenue", "orders");

    private final BestSellerService service;
    private final BranchScope scope;

    public record MarkRequest(String name, Integer days, String sort, Integer limit) { }

    @GetMapping
    public BestSellersDto get(@RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "qty") String sort,
                              @RequestParam(defaultValue = "20") int limit) {
        return build(days, sort, limit, null);
    }

    @PostMapping("/mark")
    public BestSellersDto mark(@RequestBody MarkRequest r) {
        int d = day(r.days());
        LocalDate[] range = range(d);
        service.mark(scope.current(), r.name(), range[0], range[1], label(d), UserPrincipal.current().getFullName());
        return build(d, r.sort(), lim(r.limit()), "'" + r.name() + "' চিহ্নিত হয়েছে।");
    }

    @PostMapping("/mark-top")
    public BestSellersDto markTop(@RequestBody MarkRequest r) {
        int d = day(r.days());
        LocalDate[] range = range(d);
        int n = service.markTop(scope.current(), range[0], range[1], sort(r.sort()), lim(r.limit()), label(d),
                UserPrincipal.current().getFullName());
        return build(d, r.sort(), lim(r.limit()), n + "টি ওষুধ নতুন করে চিহ্নিত হয়েছে।");
    }

    @PostMapping("/unmark")
    public BestSellersDto unmark(@RequestBody MarkRequest r) {
        service.unmark(r.name());
        return build(day(r.days()), r.sort(), lim(r.limit()), "'" + r.name() + "'-এর চিহ্ন তুলে নেওয়া হয়েছে।");
    }

    private BestSellersDto build(Integer daysIn, String sortIn, Integer limitIn, String message) {
        int d = day(daysIn);
        String s = sort(sortIn);
        int l = lim(limitIn);
        LocalDate[] r = range(d);
        return new BestSellersDto(label(d), d, s, l,
                service.ranking(scope.current(), r[0], r[1], s, l).stream()
                        .map(x -> new SellerDto(x.name(), x.pieces(), x.revenue(), x.invoices(), x.stock(), x.marked())).toList(),
                service.allMarks().stream()
                        .map(m -> new MarkDto(m.getDisplayName(), m.getSoldPieces(), m.getRevenue(), m.getPeriodLabel(),
                                m.getMarkedBy(), m.getMarkedAt(), m.isAutoMarked())).toList(),
                message);
    }

    private static int day(Integer d) { return d != null && DAY_OPTIONS.contains(d) ? d : 30; }
    private static String sort(String s) { return s != null && SORTS.contains(s) ? s : "qty"; }
    private static int lim(Integer l) { return l == null ? 20 : Math.max(5, Math.min(l, 100)); }

    private static LocalDate[] range(int days) {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{days == 0 ? LocalDate.of(2000, 1, 1) : today.minusDays(days - 1L), today};
    }

    private static String label(int days) { return days == 0 ? "সব সময়" : "গত " + days + " দিন"; }
}
