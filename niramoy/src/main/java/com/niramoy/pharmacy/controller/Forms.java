package com.niramoy.pharmacy.controller;

import com.niramoy.pharmacy.entity.ExpenseGroup;
import com.niramoy.pharmacy.entity.ExpenseStatus;
import com.niramoy.pharmacy.entity.PaymentMethod;
import com.niramoy.pharmacy.entity.Priority;
import com.niramoy.pharmacy.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** ফর্ম-বাইন্ডিং ও ভ্যালিডেশনের ক্লাসগুলো (এন্টিটি সরাসরি ফর্মে ব্যবহার করা হয় না)। */
public final class Forms {
    private Forms() { }

    @Data
    public static class MedicineForm {
        @NotBlank(message = "ওষুধের নাম দিন") @Size(max = 150, message = "সর্বোচ্চ ১৫০ অক্ষর")
        private String name;
        @Size(max = 80, message = "সর্বোচ্চ ৮০ অক্ষর")
        private String category;
        @Size(max = 150, message = "সর্বোচ্চ ১৫০ অক্ষর")
        private String genericName;
        @Size(max = 150, message = "সর্বোচ্চ ১৫০ অক্ষর")
        private String companyName;
        @NotBlank(message = "বারকোড দিন") @Size(max = 60, message = "সর্বোচ্চ ৬০ অক্ষর")
        private String barcode;
        @NotBlank(message = "ব্যাচ নম্বর দিন") @Size(max = 60, message = "সর্বোচ্চ ৬০ অক্ষর")
        private String batchNo;
        @NotNull(message = "মেয়াদ দিন") @Future(message = "মেয়াদ ভবিষ্যতের তারিখ হতে হবে")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate expiryDate;
        @NotNull(message = "ক্রয়মূল্য দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal costPrice;
        @NotNull(message = "বিক্রয়মূল্য দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal salePrice;
        @NotBlank(message = "বক্স নম্বর দিন") @Size(max = 60, message = "সর্বোচ্চ ৬০ অক্ষর")
        private String boxNo;
        @NotNull(message = "পাতা সংখ্যা দিন (না থাকলে ০)") @Min(value = 0, message = "ঋণাত্মক হতে পারবে না")
        private Integer stockStrips = 0;
        @NotNull(message = "পিস সংখ্যা দিন (না থাকলে ০)") @Min(value = 0, message = "ঋণাত্মক হতে পারবে না")
        private Integer stockPieces = 0;
        @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal stripPrice;
        @NotNull(message = "পুনঃঅর্ডার সীমা দিন") @Min(value = 0, message = "ঋণাত্মক হতে পারবে না")
        private Integer reorderLevel;
        @NotNull(message = "এক পাতায় পিস দিন") @Min(value = 1, message = "কমপক্ষে ১")
        private Integer stripSize = 10;
    }

    @Data
    public static class StockAdjustForm {
        @NotNull(message = "পরিবর্তনের পরিমাণ দিন")
        private Integer delta;
        @NotBlank(message = "কারণ লিখুন") @Size(max = 255)
        private String reason;
    }

    @Data
    public static class SupplierForm {
        @NotBlank(message = "নাম দিন") @Size(max = 150)
        private String name;
        @NotBlank(message = "ফোন নম্বর দিন") @Size(max = 30)
        @Pattern(regexp = "^[0-9০-৯+\\-\\s]{6,30}$", message = "সঠিক ফোন নম্বর দিন")
        private String phone;
    }

    /** ক্রয় তালিকার একটি সারি (ফার্মেসিতে যে ওষুধ নেই) */
    @Data
    public static class ItemRow {
        private String brandName;
        private String genericName;
        private String companyName;
        private Integer quantity;
        private String unit = "পিস";
        private Priority priority = Priority.NORMAL;
    }

    @Data
    public static class OrderForm {
        @NotNull(message = "সরবরাহকারী নির্বাচন করুন")
        private Long supplierId;
        @Size(max = 100, message = "সর্বোচ্চ ১০০ অক্ষর")
        private String mpoName;
        private List<ItemRow> items = new ArrayList<>();
    }

    @Data
    public static class ReceiveForm {
        @NotBlank(message = "ওষুধের নাম দিন") @Size(max = 150)
        private String name;
        @Size(max = 80)
        private String category;
        @Size(max = 150)
        private String genericName;
        @Size(max = 150)
        private String companyName;
        @NotBlank(message = "বারকোড দিন") @Size(max = 60)
        private String barcode;
        @NotBlank(message = "ব্যাচ নম্বর দিন") @Size(max = 60)
        private String batchNo;
        @NotNull(message = "মেয়াদ দিন") @Future(message = "মেয়াদ ভবিষ্যতের তারিখ হতে হবে")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate expiryDate;
        @NotNull(message = "ক্রয়মূল্য দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal costPrice;
        @NotNull(message = "বিক্রয়মূল্য দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal salePrice;
        @NotBlank(message = "বক্স নম্বর দিন") @Size(max = 60)
        private String boxNo;
        @NotNull(message = "এক পাতায় পিস দিন") @Min(value = 1, message = "কমপক্ষে ১")
        private Integer stripSize = 10;
        @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal stripPrice;
        @NotNull(message = "পাতা সংখ্যা দিন (না থাকলে ০)") @Min(0)
        private Integer quantityStrips = 0;
        @NotNull(message = "পিস সংখ্যা দিন (না থাকলে ০)") @Min(0)
        private Integer quantityPieces = 0;
        @NotNull(message = "পুনঃঅর্ডার সীমা দিন") @Min(0)
        private Integer reorderLevel = 20;
    }

    /** সরবরাহকারী পেমেন্ট ও গ্রাহকের বকেয়া আদায়ে ব্যবহৃত */
    @Data
    public static class AmountForm {
        @NotNull(message = "পরিমাণ দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal amount;
        @NotNull(message = "পদ্ধতি নির্বাচন করুন")
        private PaymentMethod method = PaymentMethod.CASH;
    }

    @Data
    public static class PurchaseReturnForm {
        @NotNull(message = "পরিমাণ দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal amount;
        @NotBlank(message = "কারণ লিখুন") @Size(max = 255)
        private String reason;
    }

    @Data
    public static class ReturnForm {
        @NotBlank(message = "ফেরতের কারণ লিখুন") @Size(max = 255)
        private String reason;
        @NotNull(message = "রিফান্ড পদ্ধতি নির্বাচন করুন")
        private PaymentMethod method = PaymentMethod.CASH;
        private boolean restoreStock = true;
    }

    @Data
    public static class UserForm {
        @NotBlank(message = "ইউজারনেম দিন")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$", message = "৩–৩০টি ইংরেজি অক্ষর/সংখ্যা (. _ - চলবে)")
        private String username;
        @NotBlank(message = "পূর্ণ নাম দিন") @Size(max = 100)
        private String fullName;
        @NotBlank(message = "পাসওয়ার্ড দিন") @Size(min = 8, max = 72, message = "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে")
        private String password;
        @NotNull(message = "ভূমিকা নির্বাচন করুন")
        private Role role = Role.CASHIER;
        private Long branchId;
    }

    @Data
    public static class BranchForm {
        @NotBlank(message = "শাখার নাম দিন") @Size(max = 100)
        private String name;
        @Size(max = 255) private String address;
        @Size(max = 30) private String phone;
        @Size(max = 100) private String managerName;
    }

    @Data
    public static class SettingsForm {
        @NotNull(message = "VAT দিন") @DecimalMin(value = "0", message = "০–১০০ এর মধ্যে দিন")
        @DecimalMax(value = "100", message = "০–১০০ এর মধ্যে দিন")
        private BigDecimal vatPercent;
        @NotBlank(message = "প্রিফিক্স দিন")
        @Pattern(regexp = "^[A-Za-z0-9-]{2,10}$", message = "২–১০টি ইংরেজি অক্ষর, সংখ্যা বা হাইফেন")
        private String invoicePrefix;
    }

    // ---------- গ্রাহকের নিয়মিত ওষুধ সাবস্ক্রিপশন ----------
    @Data
    public static class RefillItemRow {
        private String medicineName;
        private Integer quantity;
        private String unit = "পিস";
    }

    @Data
    public static class RefillForm {
        @NotBlank(message = "গ্রাহকের নাম দিন") @Size(max = 100, message = "সর্বোচ্চ ১০০ অক্ষর")
        private String customerName;
        @NotBlank(message = "মোবাইল নম্বর দিন") @Size(max = 20, message = "সঠিক মোবাইল নম্বর দিন")
        private String customerPhone;
        @Size(max = 100, message = "সর্বোচ্চ ১০০ অক্ষর")
        private String planName;
        @NotNull(message = "কত দিন পরপর রিফিল তা দিন") @Min(value = 1, message = "কমপক্ষে ১ দিন") @Max(value = 365, message = "সর্বোচ্চ ৩৬৫ দিন")
        private Integer frequencyDays = 30;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;
        @NotNull(message = "ছাড়ের হার দিন (না থাকলে ০)") @DecimalMin(value = "0", message = "০–৫০ এর মধ্যে দিন")
        @DecimalMax(value = "50", message = "সর্বোচ্চ ৫০%")
        private BigDecimal discountPercent = BigDecimal.ZERO;
        @Size(max = 255, message = "সর্বোচ্চ ২৫৫ অক্ষর")
        private String note;
        private List<RefillItemRow> items = new ArrayList<>();
    }

    // ---------- সফটওয়্যার সাবস্ক্রিপশন ----------
    @Data
    public static class SubscriptionRequestForm {
        @NotNull(message = "প্ল্যান নির্বাচন করুন")
        private Long planId;
        @NotNull(message = "পেমেন্ট পদ্ধতি নির্বাচন করুন")
        private PaymentMethod method = PaymentMethod.BKASH;
        @Size(max = 60, message = "সর্বোচ্চ ৬০ অক্ষর")
        private String trxId;
        @Size(max = 30, message = "সর্বোচ্চ ৩০ অক্ষর")
        private String senderNumber;
        @Size(max = 255, message = "সর্বোচ্চ ২৫৫ অক্ষর")
        private String note;
    }

    @Data
    public static class PlanForm {
        @NotNull @DecimalMin("0") private BigDecimal price;
        @NotNull @Min(1) private Integer durationDays;
        @NotNull @Min(1) private Integer maxBranches;
        @NotNull @Min(1) private Integer maxUsers;
        private boolean active;
    }

    // ---------- খরচ ব্যবস্থাপনা ----------
    @Data
    public static class ExpenseForm {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate expenseDate;
        @NotNull(message = "ক্যাটাগরি নির্বাচন করুন")
        private Long categoryId;
        private Long subCategoryId;
        @NotNull(message = "টাকার পরিমাণ দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal amount;
        @NotNull(message = "অবস্থা নির্বাচন করুন")
        private ExpenseStatus status = ExpenseStatus.PAID;
        /** শুধু "আংশিক" অবস্থায় প্রযোজ্য */
        private BigDecimal paidAmount;
        private PaymentMethod paymentMethod = PaymentMethod.CASH;
        @Size(max = 100, message = "সর্বোচ্চ ১০০ অক্ষর") private String paidTo;
        @Size(max = 60, message = "সর্বোচ্চ ৬০ অক্ষর") private String referenceNo;
        @Size(max = 255, message = "সর্বোচ্চ ২৫৫ অক্ষর") private String description;
    }

    @Data
    public static class CategoryForm {
        @NotBlank(message = "ক্যাটাগরির নাম দিন") @Size(max = 100) private String name;
        @Size(max = 100) private String nameEn;
        @NotNull(message = "গ্রুপ নির্বাচন করুন") private ExpenseGroup groupType = ExpenseGroup.SHOP;
    }

    @Data
    public static class SubCategoryForm {
        @NotNull(message = "ক্যাটাগরি নির্বাচন করুন") private Long categoryId;
        @NotBlank(message = "উপ-ক্যাটাগরির নাম দিন") @Size(max = 100) private String name;
    }

    @Data
    public static class ExpensePayForm {
        @NotNull(message = "পরিমাণ দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal amount;
        @NotNull(message = "পদ্ধতি নির্বাচন করুন") private PaymentMethod method = PaymentMethod.CASH;
        @Size(max = 60) private String referenceNo;
    }

    // ---------- ক্রয়ের হাতে হিসাব ----------
    @Data
    public static class ManualPurchaseForm {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate date;
        @NotNull(message = "ক্রয়ের টাকার পরিমাণ দিন") @DecimalMin(value = "0.01", message = "০-এর বেশি হতে হবে")
        private BigDecimal amount;
        @Size(max = 150, message = "সর্বোচ্চ ১৫০ অক্ষর") private String supplier;
        @Size(max = 255, message = "সর্বোচ্চ ২৫৫ অক্ষর") private String note;
    }
}
