package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.exception.BusinessException;

import java.util.regex.Pattern;

/** বাংলাদেশি মোবাইল নম্বর যাচাই ও 01XXXXXXXXX আকারে আনে */
public final class PhoneUtil {
    private static final Pattern BD_MOBILE = Pattern.compile("^01[3-9][0-9]{8}$");
    private PhoneUtil() { }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) throw new BusinessException("মোবাইল নম্বর আবশ্যক।");
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (c >= '০' && c <= '৯') sb.append((char) ('0' + (c - '০')));
            else if (!Character.isWhitespace(c) && c != '-' && c != '(' && c != ')') sb.append(c);
        }
        String d = sb.toString();
        if (d.startsWith("+88")) d = d.substring(3);
        else if (d.startsWith("88") && d.length() == 13) d = d.substring(2);
        if (!BD_MOBILE.matcher(d).matches())
            throw new BusinessException("সঠিক মোবাইল নম্বর দিন (যেমন: 01712345678)।");
        return d;
    }
}
