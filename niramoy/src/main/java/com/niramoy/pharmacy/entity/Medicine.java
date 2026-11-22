package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** প্রতিটি রো = একটি ব্যাচ (FEFO অনুযায়ী মেয়াদ দিয়ে সাজানো হয়) */
@Entity
@Table(name = "medicines",
        indexes = {@Index(name = "idx_med_barcode", columnList = "barcode"),
                   @Index(name = "idx_med_expiry", columnList = "expiryDate")},
        uniqueConstraints = @UniqueConstraint(name = "uk_med_branch_barcode_batch",
                columnNames = {"branch_id", "barcode", "batchNo"}))
@Getter @Setter @NoArgsConstructor
public class Medicine {
    public static final int EXPIRY_WARNING_DAYS = 90;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 80)
    private String category;

    @Column(length = 150)
    private String genericName;

    @Column(length = 150)
    private String companyName;

    @Column(nullable = false, length = 60)
    private String barcode;

    @Column(nullable = false, length = 60)
    private String batchNo;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private int reorderLevel;

    /** এক পাতায় কত পিস (দাম ও স্টক পিস হিসাবে রাখা হয়) */
    @Column(nullable = false, columnDefinition = "int not null default 10")
    private int stripSize = 10;

    /** কোন বক্স/তাকে রাখা আছে (যেমন A-12) */
    @Column(length = 60)
    private String boxNo;

    /** এক পাতার বিক্রয়মূল্য; খালি থাকলে পিস দাম × এক পাতায় পিস */
    @Column(precision = 12, scale = 2)
    private BigDecimal stripPrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    public BigDecimal getEffectiveStripPrice() {
        if (stripPrice != null && stripPrice.signum() > 0) return stripPrice;
        return salePrice.multiply(BigDecimal.valueOf(Math.max(stripSize, 1)));
    }
    public BigDecimal getStripCost() { return costPrice.multiply(BigDecimal.valueOf(Math.max(stripSize, 1))); }

    public boolean isLowStock() { return stock <= reorderLevel; }
    public boolean isExpired() { return expiryDate.isBefore(LocalDate.now()); }
    public boolean isExpiringSoon() {
        return !isExpired() && !expiryDate.isAfter(LocalDate.now().plusDays(EXPIRY_WARNING_DAYS));
    }
}
