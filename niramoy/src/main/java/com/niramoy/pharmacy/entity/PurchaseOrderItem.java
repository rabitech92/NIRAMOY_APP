package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** ক্রয় তালিকার একটি ওষুধ (যা ফার্মেসিতে নেই এবং এমপিওর কাছে চাওয়া হচ্ছে) */
@Entity
@Table(name = "purchase_order_items")
@Getter @Setter @NoArgsConstructor
public class PurchaseOrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Column(nullable = false, length = 150)
    private String brandName;

    @Column(length = 150)
    private String genericName;

    @Column(length = 150)
    private String companyName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 20)
    private String unit = "পিস";

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15)
    private Priority priority = Priority.NORMAL;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.NOT_ORDERED;

    private LocalDateTime orderedAt;
    private LocalDateTime receivedAt;
}
