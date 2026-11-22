package com.niramoy.pharmacy.entity;

public enum Priority {
    NORMAL("সাধারণ"), URGENT("জরুরি");

    private final String label;
    Priority(String label) { this.label = label; }
    public String getLabel() { return label; }
}
