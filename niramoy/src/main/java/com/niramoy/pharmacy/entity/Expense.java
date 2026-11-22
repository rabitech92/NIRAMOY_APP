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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses",
        indexes = {@Index(name = "idx_expense_date", columnList = "expenseDate"),
                   @Index(name = "idx_expense_branch", columnList = "branch_id")})
@Getter @Setter @NoArgsConstructor
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 40)
    private String expenseNo;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(optional = false) @JoinColumn(name = "category_id")
    private ExpenseCategory category;

    @ManyToOne @JoinColumn(name = "sub_category_id")
    private ExpenseSubCategory subCategory;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** এ পর্যন্ত পরিশোধিত (পেমেন্টগুলোর যোগফল) */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private ExpenseStatus status = ExpenseStatus.UNPAID;

    @Column(length = 100) private String paidTo;
    @Column(length = 60) private String referenceNo;
    @Column(length = 255) private String description;

    @ManyToOne(optional = false) @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(length = 100) private String createdBy;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(length = 100) private String updatedBy;
    private LocalDateTime updatedAt;

    @Column(nullable = false) private boolean voided = false;
    @Column(length = 255) private String voidReason;
    @Column(length = 100) private String voidedBy;
    private LocalDateTime voidedAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("id asc")
    private List<ExpensePayment> payments = new ArrayList<>();

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public void addPayment(ExpensePayment p) { p.setExpense(this); payments.add(p); }

    public BigDecimal getDueAmount() { return amount.subtract(paidAmount); }

    /** পরিশোধিত অঙ্ক থেকে অবস্থা ঠিক করে */
    public void recompute() {
        if (paidAmount.signum() <= 0) status = ExpenseStatus.UNPAID;
        else if (paidAmount.compareTo(amount) >= 0) status = ExpenseStatus.PAID;
        else status = ExpenseStatus.PARTIAL;
    }
}
