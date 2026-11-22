package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** প্রতিদিনের স্টক মূল্যের ছবি (শাখাভিত্তিক) — মাস/বছর শেষে স্টকের মূল্য দেখানোর জন্য */
@Entity
@Table(name = "stock_snapshots",
        uniqueConstraints = @UniqueConstraint(name = "uk_snapshot_branch_day", columnNames = {"branch_id", "snapshotDate"}))
@Getter @Setter @NoArgsConstructor
public class StockSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false) private LocalDate snapshotDate;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal costValue = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal saleValue = BigDecimal.ZERO;
    @Column(nullable = false) private long units;
}
