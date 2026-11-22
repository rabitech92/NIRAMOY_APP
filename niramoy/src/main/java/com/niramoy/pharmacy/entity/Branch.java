package com.niramoy.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branches")
@Getter @Setter @NoArgsConstructor
public class Branch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String managerName;

    @Column(nullable = false)
    private boolean active = true;

    public Branch(String name, String address, String phone, String managerName) {
        this.name = name; this.address = address; this.phone = phone; this.managerName = managerName;
    }
}
