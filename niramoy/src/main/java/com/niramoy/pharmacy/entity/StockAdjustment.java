package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustments")
@Getter @Setter @NoArgsConstructor
public class StockAdjustment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(nullable = false) private int delta;
    @Column(nullable = false) private int stockAfter;
    @Column(nullable = false, length = 255) private String reason;
    @Column(nullable = false, length = 100) private String performedBy;
    @Column(nullable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
