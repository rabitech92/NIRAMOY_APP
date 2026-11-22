package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** গ্রাহকের নিয়মিত ওষুধ সাবস্ক্রিপশন (মাসিক/পাক্ষিক রিফিল) */
@Entity
@Table(name = "refill_subscriptions", indexes = @Index(name = "idx_refill_next", columnList = "nextRefillDate"))
@Getter @Setter @NoArgsConstructor
public class CustomerSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(optional = false) @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false, length = 100) private String customerName;
    @Column(nullable = false, length = 30) private String customerPhone;
    @Column(nullable = false, length = 100) private String planName;

    @Column(nullable = false) private int frequencyDays = 30;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate nextRefillDate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private RefillStatus status = RefillStatus.ACTIVE;

    @Column(length = 255) private String note;
    @Column(nullable = false) private int cycles = 0;
    private LocalDateTime lastBilledAt;
    private Long lastSaleId;

    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(length = 100) private String createdBy;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id asc")
    private List<CustomerSubscriptionItem> items = new ArrayList<>();

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public void addItem(CustomerSubscriptionItem i) { i.setSubscription(this); items.add(i); }

    /** আজ বা আগে রিফিল হওয়ার কথা ছিল */
    public boolean isDue() { return status == RefillStatus.ACTIVE && !nextRefillDate.isAfter(LocalDate.now()); }

    /** ঋণাত্মক = দেরি, ০ = আজ, ধনাত্মক = বাকি দিন */
    public long getDaysToNext() { return ChronoUnit.DAYS.between(LocalDate.now(), nextRefillDate); }
}
