package com.niramoy.pharmacy.entity;

public enum Role {
    SUPER_ADMIN("সুপার অ্যাডমিন"), OWNER("মালিক"), BRANCH_MANAGER("শাখা ম্যানেজার"),
    PHARMACIST("ফার্মাসিস্ট"), CASHIER("ক্যাশিয়ার"), INVENTORY_MANAGER("ইনভেন্টরি ম্যানেজার");

    private final String label;
    Role(String label) { this.label = label; }
    public String getLabel() { return label; }
    /** সব শাখা দেখার অধিকার আছে কি না */
    public boolean isGlobal() { return this == SUPER_ADMIN || this == OWNER; }
}
