package com.niramoy.pharmacy.api;

import com.niramoy.pharmacy.api.ApiDtos.*;
import com.niramoy.pharmacy.entity.*;
import com.niramoy.pharmacy.security.UserPrincipal;
import com.niramoy.pharmacy.service.SubscriptionStatus;
import org.springframework.data.domain.Page;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** entity → DTO রূপান্তর */
final class ApiMapper {
    private ApiMapper() { }

    static boolean hasRole(UserPrincipal u, Role... roles) {
        return u != null && Arrays.asList(roles).contains(u.getRole());
    }

    /** ক্রয়মূল্য ও লাভ শুধু ম্যানেজমেন্ট দেখতে পাবেন */
    static boolean canSeeMoney(UserPrincipal u) {
        return hasRole(u, Role.SUPER_ADMIN, Role.OWNER, Role.BRANCH_MANAGER);
    }

    static UserDto user(UserPrincipal u) {
        return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getRole().name(), u.getRole().getLabel(),
                u.isGlobal(), u.getBranchId(), u.getBranchName());
    }

    static BranchDto branch(Branch b) { return new BranchDto(b.getId(), b.getName()); }

    static SubscriptionDto subscription(SubscriptionStatus s) {
        return new SubscriptionDto(s.getLevel().name(), s.getPlanName(), s.getEndDate(), s.getDaysLeft(),
                s.isBlocked(), s.isWarning(), s.isPending(), s.getMessage());
    }

    static MedicineDto medicine(Medicine m, boolean showCost, boolean bestSeller) {
        return new MedicineDto(m.getId(), m.getName(), m.getGenericName(), m.getCompanyName(), m.getCategory(),
                m.getBarcode(), m.getBatchNo(), m.getExpiryDate(), m.getStock(), Math.max(m.getStripSize(), 1),
                m.getSalePrice(), m.getEffectiveStripPrice(), showCost ? m.getCostPrice() : null, m.getBoxNo(),
                m.getBranch().getName(), m.isLowStock(), m.isExpired(), m.isExpiringSoon(), bestSeller);
    }

    static SaleDto sale(Sale s, boolean withItems) {
        List<SaleItemDto> items = withItems
                ? s.getItems().stream().map(i -> new SaleItemDto(i.getMedicineName(), i.getBatchNo(), i.getQuantity(),
                        i.getUnitLabel() == null ? "পিস" : i.getUnitLabel(), i.getUnitPrice(), i.getLineTotal())).toList()
                : List.of();
        return new SaleDto(s.getId(), s.getInvoiceNo(), s.getCreatedAt(), s.getBranch().getName(),
                s.getCustomerName(), s.getCustomerPhone(), s.getPaymentMethod().name(), s.getPaymentMethod().getLabel(),
                s.getStatus().name(), s.getStatus().getLabel(), s.getSubtotal(), s.getDiscount(), s.getVatPercent(),
                s.getVatAmount(), s.getTotal(), s.getDueAmount(),
                s.getCashier() == null ? null : s.getCashier().getFullName(), items);
    }

    static RefillDto refill(CustomerSubscription r) {
        List<RefillItemDto> items = r.getItems().stream()
                .map(i -> new RefillItemDto(i.getMedicineName(), i.getQuantity(), i.getUnit())).toList();
        return new RefillDto(r.getId(), r.getCustomerName(), r.getCustomerPhone(), r.getPlanName(), r.getFrequencyDays(),
                r.getStartDate(), r.getNextRefillDate(), r.getDaysToNext(), r.getStatus().name(), r.getStatus().getLabel(),
                r.getDiscountPercent(), r.getCycles(), r.isDue(), r.getNote(), r.getBranch().getName(),
                r.getLastSaleId(), items);
    }

    static <E, D> PageDto<D> page(Page<E> p, Function<E, D> f) {
        return new PageDto<>(p.getContent().stream().map(f).toList(), p.getNumber(), p.getTotalPages(), p.getTotalElements());
    }

    static ExpenseDto expense(Expense e, boolean withPayments) {
        List<ExpensePaymentDto> pays = withPayments
                ? e.getPayments().stream().map(p -> new ExpensePaymentDto(p.getPaidOn(), p.getMethod().name(),
                        p.getMethod().getLabel(), p.getAmount(), p.getReferenceNo(), p.getPaidBy())).toList()
                : List.of();
        return new ExpenseDto(e.getId(), e.getExpenseNo(), e.getExpenseDate(), e.getCategory().getName(),
                e.getSubCategory() == null ? null : e.getSubCategory().getName(), e.getAmount(), e.getPaidAmount(),
                e.getDueAmount(), e.getStatus().name(), e.getStatus().getLabel(), e.getPaidTo(), e.getReferenceNo(),
                e.getDescription(), e.getBranch().getName(), e.getCreatedBy(), e.getCreatedAt(), e.isVoided(),
                e.getVoidReason(), e.getVoidedBy(), pays);
    }
}
