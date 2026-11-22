package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** সফটওয়্যার সাবস্ক্রিপশনের প্ল্যান (ফ্রি ট্রায়াল, মাসিক, ষান্মাসিক, বার্ষিক ...) */
@Entity
@Table(name = "subscription_plans")
@Getter @Setter @NoArgsConstructor
public class SubscriptionPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private int durationDays;

    @Column(nullable = false)
    private int maxBranches;

    @Column(nullable = false)
    private int maxUsers;

    @Column(nullable = false)
    private boolean active = true;

    public boolean isFree() { return price == null || price.signum() <= 0; }

    public SubscriptionPlan(String code, String name, String price, int durationDays, int maxBranches, int maxUsers) {
        this.code = code; this.name = name; this.price = new BigDecimal(price);
        this.durationDays = durationDays; this.maxBranches = maxBranches; this.maxUsers = maxUsers;
    }
}
