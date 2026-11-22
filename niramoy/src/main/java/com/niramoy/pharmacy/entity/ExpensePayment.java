package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** একটি খরচের একটি পেমেন্ট (বকেয়া/আংশিক খরচে একাধিক পেমেন্ট হতে পারে) */
@Entity
@Table(name = "expense_payments", indexes = @Index(name = "idx_exp_pay_date", columnList = "paidOn"))
@Getter @Setter @NoArgsConstructor
public class ExpensePayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentMethod method;
    @Column(nullable = false) private LocalDate paidOn;
    @Column(length = 60) private String referenceNo;
    @Column(length = 100) private String paidBy;
    @Column(nullable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
