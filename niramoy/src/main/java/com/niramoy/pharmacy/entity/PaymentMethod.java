package com.niramoy.pharmacy.entity;

public enum PaymentMethod {
    CASH("ক্যাশ"), BKASH("বিকাশ"), NAGAD("নগদ"), ROCKET("রকেট"), BANK("ব্যাংক"), CARD("কার্ড"), CREDIT("বকেয়া");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
