package com.niramoy.pharmacy.entity;

public enum ExpenseStatus {
    PAID("পরিশোধিত"), UNPAID("বকেয়া"), PARTIAL("আংশিক");

    private final String label;
    ExpenseStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
