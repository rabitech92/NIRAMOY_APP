package com.niramoy.pharmacy.entity;

public enum RefillStatus {
    ACTIVE("সক্রিয়"), PAUSED("বিরতি"), CANCELLED("বাতিল");

    private final String label;
    RefillStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
