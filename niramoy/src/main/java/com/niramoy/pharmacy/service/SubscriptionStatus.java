package com.niramoy.pharmacy.service;

import lombok.Getter;

import java.time.LocalDate;

/** সফটওয়্যার সাবস্ক্রিপশনের বর্তমান অবস্থা (পেজে ব্যানার ও ব্লক করার জন্য) */
@Getter
public class SubscriptionStatus {
    public enum Level { ACTIVE, GRACE, EXPIRED, NONE }

    private final Level level;
    private final String planName;
    private final LocalDate endDate;
    private final long daysLeft;
    private final boolean pending;
    private final LocalDate computedOn;

    public SubscriptionStatus(Level level, String planName, LocalDate endDate, long daysLeft,
                              boolean pending, LocalDate computedOn) {
        this.level = level; this.planName = planName; this.endDate = endDate;
        this.daysLeft = daysLeft; this.pending = pending; this.computedOn = computedOn;
    }

    public boolean isBlocked() { return level == Level.EXPIRED || level == Level.NONE; }

    public boolean isWarning() {
        return level == Level.GRACE || (level == Level.ACTIVE && daysLeft <= SubscriptionService.WARN_DAYS);
    }

    public String getMessage() {
        return switch (level) {
            case ACTIVE -> daysLeft == 0 ? "আপনার সাবস্ক্রিপশনের মেয়াদ আজ শেষ হচ্ছে।"
                    : "আপনার সাবস্ক্রিপশনের মেয়াদ আর " + daysLeft + " দিন বাকি।";
            case GRACE -> "সাবস্ক্রিপশনের মেয়াদ শেষ হয়েছে। গ্রেস পিরিয়ড চলছে, দ্রুত নবায়ন করুন নইলে সফটওয়্যার বন্ধ হয়ে যাবে।";
            case EXPIRED -> "সাবস্ক্রিপশনের মেয়াদ শেষ হয়েছে।";
            case NONE -> "কোনো সক্রিয় সাবস্ক্রিপশন নেই।";
        };
    }
}
