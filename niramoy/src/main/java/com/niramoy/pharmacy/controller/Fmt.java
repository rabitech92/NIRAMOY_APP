package com.niramoy.pharmacy.controller;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Thymeleaf-এ ${@fmt.money(x)} হিসেবে ব্যবহৃত ফরম্যাটার */
@Component("fmt")
public class Fmt {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String money(BigDecimal v) {
        return "৳" + String.format(Locale.US, "%,.2f", v == null ? BigDecimal.ZERO : v);
    }
    /** পিস-ভিত্তিক স্টককে "৩ পাতা ৪ পিস" আকারে দেখায় */
    public String stock(int stock, int stripSize) {
        if (stripSize <= 1) return stock + " পিস";
        int strips = stock / stripSize, loose = stock % stripSize;
        if (strips == 0) return loose + " পিস";
        return loose == 0 ? strips + " পাতা" : strips + " পাতা " + loose + " পিস";
    }
    public String date(LocalDate d) { return d == null ? "" : DATE.format(d); }
    public String dateTime(LocalDateTime t) { return t == null ? "" : DATE_TIME.format(t); }
}
