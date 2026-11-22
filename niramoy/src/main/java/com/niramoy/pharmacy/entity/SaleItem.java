package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** ইনভয়েসের মুহূর্তের নাম/দামের স্ন্যাপশট রাখা হয়, তাই পরে ওষুধ বদলালেও ইনভয়েস ঠিক থাকে */
@Entity
@Table(name = "sale_items")
@Getter @Setter @NoArgsConstructor
public class SaleItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "sale_id")
    private Sale sale;

    @Column(nullable = false) private Long medicineId;
    @Column(nullable = false, length = 150) private String medicineName;
    @Column(nullable = false, length = 60) private String batchNo;
    @Column(nullable = false) private int quantity;
    /** এক ইউনিটে কত পিস (পিস বিক্রয়ে ১, পাতা বিক্রয়ে এক পাতায় পিস) */
    @Column(nullable = false, columnDefinition = "int not null default 1") private int packSize = 1;
    @Column(length = 20) private String unitLabel = "পিস";
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal costPrice;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal lineTotal;
}
