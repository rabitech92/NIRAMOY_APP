package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ক্রয়ের খাতা: স্টকে মাল ঢোকার প্রতিটি ঘটনা (ক্রয় তালিকা থেকে গ্রহণ, হাতে স্টক এন্ট্রি, অথবা হাতে লেখা মোট ক্রয়)।
 * মাসিক/বার্ষিক ক্রয়ের হিসাব এখান থেকে আসে।
 */
@Entity
@Table(name = "purchase_entries", indexes = @Index(name = "idx_purchase_entry_date", columnList = "entryDate"))
@Getter @Setter @NoArgsConstructor
public class PurchaseEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false) private LocalDate entryDate;

    /** "হাতে ক্রয়ের হিসাব"-এ ফাঁকা থাকে */
    @Column(length = 150) private String medicineName;
    @Column(length = 60) private String batchNo;

    /** মোট পিস (হাতে ক্রয়ের হিসাবে ০) */
    @Column(nullable = false) private long quantity;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(length = 150) private String supplierName;
    @Column(length = 60) private String reference;
    @Column(length = 255) private String note;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PurchaseSource source;

    @Column(length = 100) private String createdBy;
    @Column(nullable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
