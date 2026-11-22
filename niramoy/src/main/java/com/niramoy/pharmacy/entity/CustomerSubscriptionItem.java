package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refill_subscription_items")
@Getter @Setter @NoArgsConstructor
public class CustomerSubscriptionItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private CustomerSubscription subscription;

    @Column(nullable = false, length = 150)
    private String medicineName;

    @Column(nullable = false)
    private int quantity;

    /** পিস বা পাতা */
    @Column(nullable = false, length = 20)
    private String unit = "পিস";
}
