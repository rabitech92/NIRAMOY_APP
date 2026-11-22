package com.niramoy.pharmacy.entity;

public enum SubscriptionState {
    PENDING("যাচাই অপেক্ষায়"), ACTIVE("সক্রিয়"), REJECTED("প্রত্যাখ্যাত");

    private final String label;
    SubscriptionState(String label) { this.label = label; }
    public String getLabel() { return label; }
}
