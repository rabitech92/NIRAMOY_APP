package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expense_sub_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_expense_sub", columnNames = {"category_id", "name"}))
@Getter @Setter @NoArgsConstructor
public class ExpenseSubCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "category_id")
    private ExpenseCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;
}
