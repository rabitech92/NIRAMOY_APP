package com.niramoy.pharmacy.entity;

public enum SaleStatus {
    COMPLETED("সম্পন্ন"), RETURNED("ফেরত সম্পন্ন");

    private final String label;
    SaleStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
