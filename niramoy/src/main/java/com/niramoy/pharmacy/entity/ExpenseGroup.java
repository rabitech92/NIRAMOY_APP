package com.niramoy.pharmacy.entity;

public enum ExpenseGroup {
    SHOP("দোকানের নিয়মিত খরচ"), STAFF("কর্মচারী সংক্রান্ত খরচ"), OPERATING("দৈনন্দিন পরিচালন খরচ"),
    BUSINESS("ব্যবসায়িক ও অন্যান্য খরচ"), EMERGENCY("জরুরি ও অনাকাঙ্ক্ষিত খরচ");

    private final String label;
    ExpenseGroup(String label) { this.label = label; }
    public String getLabel() { return label; }
}
