package com.niramoy.pharmacy.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** মোবাইল অ্যাপের JSON চুক্তি (entity সরাসরি পাঠানো হয় না) */
public final class ApiDtos {
    private ApiDtos() { }

    public record UserDto(Long id, String username, String fullName, String role, String roleLabel,
                          boolean global, Long branchId, String branchName) { }

    public record BranchDto(Long id, String name) { }

    public record SubscriptionDto(String level, String planName, LocalDate endDate, long daysLeft,
                                  boolean blocked, boolean warning, boolean pending, String message) { }

    public record MeDto(UserDto user, SubscriptionDto subscription, List<BranchDto> branches, BigDecimal vatPercent) { }

    public record LoginDto(String token, long expiresInSeconds, UserDto user) { }

    public record MedicineDto(Long id, String name, String genericName, String companyName, String category,
                              String barcode, String batchNo, LocalDate expiryDate, int stock, int stripSize,
                              BigDecimal price, BigDecimal stripPrice, BigDecimal costPrice, String boxNo,
                              String branchName, boolean lowStock, boolean expired, boolean expiringSoon,
                              boolean bestSeller) { }

    public record SaleItemDto(String name, String batchNo, int quantity, String unit,
                              BigDecimal unitPrice, BigDecimal lineTotal) { }

    public record SaleDto(Long id, String invoiceNo, LocalDateTime createdAt, String branchName,
                          String customerName, String customerPhone, String paymentMethod, String paymentLabel,
                          String status, String statusLabel, BigDecimal subtotal, BigDecimal discount,
                          BigDecimal vatPercent, BigDecimal vatAmount, BigDecimal total, BigDecimal dueAmount,
                          String cashier, List<SaleItemDto> items) { }

    public record RefillItemDto(String medicineName, int quantity, String unit) { }

    public record RefillDto(Long id, String customerName, String customerPhone, String planName, int frequencyDays,
                            LocalDate startDate, LocalDate nextRefillDate, long daysToNext, String status,
                            String statusLabel, BigDecimal discountPercent, int cycles, boolean due, String note,
                            String branchName, Long lastSaleId, List<RefillItemDto> items) { }

    public record PageDto<T>(List<T> items, int page, int totalPages, long totalElements) { }

    public record TrendPoint(String label, BigDecimal value) { }

    public record MixPoint(String label, int percent) { }

    public record DashboardDto(BigDecimal todaySales, BigDecimal todayProfit, BigDecimal receivable,
                               BigDecimal supplierDue, long lowStock, long expiring, long medicineCount,
                               long branchCount, long dueRefills, List<TrendPoint> trend,
                               List<MixPoint> paymentMix, List<SaleDto> recentSales) { }

    public record DuesDto(BigDecimal total, List<SaleDto> items) { }

    // ---------- খরচ ----------
    public record ExpensePaymentDto(LocalDate paidOn, String method, String methodLabel, BigDecimal amount,
                                    String referenceNo, String paidBy) { }

    public record ExpenseDto(Long id, String expenseNo, LocalDate expenseDate, String categoryName,
                             String subCategoryName, BigDecimal amount, BigDecimal paidAmount, BigDecimal dueAmount,
                             String status, String statusLabel, String paidTo, String referenceNo, String description,
                             String branchName, String createdBy, LocalDateTime createdAt, boolean voided,
                             String voidReason, String voidedBy, List<ExpensePaymentDto> payments) { }

    public record ExpenseListDto(List<ExpenseDto> items, int page, int totalPages, long totalElements,
                                 BigDecimal totalAmount, BigDecimal totalPaid) { }

    public record SliceDto(String label, BigDecimal amount, int percent) { }

    public record ExpenseDashboardDto(BigDecimal today, BigDecimal month, BigDecimal pending, BigDecimal year,
                                      String monthLabel, int yearNumber, List<SliceDto> byCategory,
                                      List<SliceDto> byPayment, List<ExpenseDto> recent) { }

    public record ExpenseCategoryDto(Long id, String name, String nameEn, String groupLabel) { }

    public record ExpenseSubDto(Long id, Long categoryId, String name) { }

    public record ExpenseMetaDto(List<ExpenseCategoryDto> categories, List<ExpenseSubDto> subCategories) { }

    // ---------- গ্রাহক বিশ্লেষণ ----------
    public record CustomerRowDto(String phone, String name, long invoices, BigDecimal lifetime, LocalDateTime lastAt,
                                 BigDecimal due, BigDecimal today, BigDecimal week, BigDecimal month, BigDecimal year) { }

    public record CustomerListDto(List<CustomerRowDto> items, int page, int totalPages, long totalElements,
                                  long allCustomers, BigDecimal allLifetime, BigDecimal allAverage) { }

    public record BucketDto(String label, long invoices, BigDecimal amount) { }

    public record TopItemDto(String name, long pieces, BigDecimal amount, long times) { }

    public record CustomerDetailDto(String phone, String name, long invoices, BigDecimal lifetime, BigDecimal average,
                                    BigDecimal due, LocalDateTime firstAt, LocalDateTime lastAt, BigDecimal today,
                                    BigDecimal week, BigDecimal month, BigDecimal year, List<BucketDto> daily,
                                    List<BucketDto> weekly, List<BucketDto> monthly, List<BucketDto> yearly,
                                    List<TopItemDto> topItems, List<SaleDto> recent) { }

    // ---------- বেশি বিক্রিত ----------
    public record SellerDto(String name, long pieces, BigDecimal revenue, long invoices, long stock, boolean marked) { }

    public record MarkDto(String name, long soldPieces, BigDecimal revenue, String periodLabel, String markedBy,
                          LocalDateTime markedAt, boolean auto) { }

    public record BestSellersDto(String periodLabel, int days, String sort, int limit, List<SellerDto> rows,
                                 List<MarkDto> marks, String message) { }
}
