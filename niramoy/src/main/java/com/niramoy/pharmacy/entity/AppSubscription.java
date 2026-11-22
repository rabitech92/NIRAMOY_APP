package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** দোকানের সফটওয়্যার সাবস্ক্রিপশনের একটি রেকর্ড (অনুরোধ → যাচাই → সক্রিয়) */
@Entity
@Table(name = "app_subscriptions")
@Getter @Setter @NoArgsConstructor
public class AppSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(optional = false) @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private SubscriptionState state = SubscriptionState.PENDING;

    /** যাচাই (approve) হওয়ার পর নির্ধারিত হয় */
    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING) @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Column(length = 60) private String trxId;
    @Column(length = 30) private String senderNumber;
    @Column(length = 100) private String requestedBy;
    @Column(nullable = false) private LocalDateTime requestedAt;
    @Column(length = 100) private String decidedBy;
    private LocalDateTime decidedAt;
    @Column(length = 255) private String note;

    @PrePersist void onCreate() { if (requestedAt == null) requestedAt = LocalDateTime.now(); }
}
