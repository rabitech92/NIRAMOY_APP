package com.niramoy.pharmacy.entity;

public enum PurchaseSource {
    ORDER("ক্রয় তালিকা থেকে গ্রহণ"), MANUAL_STOCK("হাতে স্টক এন্ট্রি"), MANUAL_TOTAL("হাতে ক্রয়ের হিসাব");

    private final String label;
    PurchaseSource(String label) { this.label = label; }
    public String getLabel() { return label; }
}
