package com.niramoy.pharmacy.entity;

/** ক্রয় তালিকার প্রতিটি আইটেমের অবস্থা */
public enum ItemStatus {
    NOT_ORDERED("অর্ডার হয়নি"), ORDERED("অর্ডার সম্পন্ন"), RECEIVED("পণ্য গ্রহণ সম্পন্ন");

    private final String label;
    ItemStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
