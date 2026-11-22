package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales",
        indexes = {@Index(name = "idx_sale_created", columnList = "createdAt"),
                   @Index(name = "idx_sale_phone", columnList = "customerPhone"),
                   @Index(name = "idx_sale_branch", columnList = "branch_id")})
@Getter @Setter @NoArgsConstructor
public class Sale {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 40)
    private String invoiceNo;

    @ManyToOne(optional = false) @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne @JoinColumn(name = "cashier_id")
    private AppUser cashier;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal discount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal vatPercent = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal vatAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal total = BigDecimal.ZERO;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal dueAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private SaleStatus status = SaleStatus.COMPLETED;

    @Column(length = 100) private String customerName;
    @Column(length = 30) private String customerPhone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public void addItem(SaleItem item) { item.setSale(this); items.add(item); }

    /** মোট লাভ = Σ (বিক্রয়মূল্য − ক্রয়মূল্য) × পরিমাণ − ছাড় */
    public BigDecimal getProfit() {
        BigDecimal margin = items.stream()
                .map(i -> i.getUnitPrice().subtract(i.getCostPrice()).multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return margin.subtract(discount);
    }
}
