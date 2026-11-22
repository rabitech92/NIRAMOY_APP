package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sale_returns")
@Getter @Setter @NoArgsConstructor
public class SaleReturn {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false) @JoinColumn(name = "sale_id", unique = true)
    private Sale sale;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal refundAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethod refundMethod;
    @Column(nullable = false, length = 255) private String reason;
    @Column(nullable = false) private boolean stockRestored;
    @Column(nullable = false, length = 100) private String processedBy;
    @Column(nullable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
