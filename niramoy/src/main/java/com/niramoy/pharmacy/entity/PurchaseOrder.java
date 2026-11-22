package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ক্রয় তালিকা / ক্রয় আদেশ। ফার্মাসিস্ট আইটেম তালিকা বানান, এমপিওকে দেখান;
 * এমপিও যেগুলোর অর্ডার নেন সেগুলো "অর্ডার সম্পন্ন" হয়, বাকিগুলো "অর্ডার হয়নি" থাকে।
 * amount = এ পর্যন্ত গ্রহণ করা পণ্যের মোট ক্রয়মূল্য।
 */
@Entity
@Table(name = "purchase_orders")
@Getter @Setter @NoArgsConstructor
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 40)
    private String orderNo;

    @ManyToOne(optional = false) @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(optional = false) @JoinColumn(name = "branch_id")
    private Branch branch;

    /** এমপিও (Medical Promotion Officer)-এর নাম */
    @Column(length = 100)
    private String mpoName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 255)
    private String itemsSummary;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false) private LocalDateTime createdAt;
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id asc")
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public void addItem(PurchaseOrderItem item) { item.setOrder(this); items.add(item); }

    public int getTotalItems() { return items.size(); }
    public long getNotOrderedCount() { return count(ItemStatus.NOT_ORDERED); }
    public long getOrderedCount() { return count(ItemStatus.ORDERED); }
    public long getReceivedCount() { return count(ItemStatus.RECEIVED); }
    public long getUrgentOpenCount() {
        return items.stream().filter(i -> i.getPriority() == Priority.URGENT && i.getStatus() != ItemStatus.RECEIVED).count();
    }
    private long count(ItemStatus s) { return items.stream().filter(i -> i.getStatus() == s).count(); }
}
