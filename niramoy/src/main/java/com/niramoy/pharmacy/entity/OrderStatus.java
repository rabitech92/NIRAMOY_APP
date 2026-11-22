package com.niramoy.pharmacy.entity;

public enum OrderStatus {
    PENDING("গ্রহণ অপেক্ষায়"), RECEIVED("গ্রহণ সম্পন্ন");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
