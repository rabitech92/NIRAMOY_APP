package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "expense_categories")
@Getter @Setter @NoArgsConstructor
public class ExpenseCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** বাংলা নাম */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** ইংরেজি নাম (যেমন Shop Rent) */
    @Column(length = 100)
    private String nameEn;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ExpenseGroup groupType;

    @Column(nullable = false)
    private boolean active = true;

    public ExpenseCategory(ExpenseGroup group, String name, String nameEn) {
        this.groupType = group; this.name = name; this.nameEn = nameEn;
    }
}
