package com.niramoy.pharmacy.service;

import com.niramoy.pharmacy.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

/** POS থেকে আসা অনুরোধ। দাম/VAT ক্লায়েন্ট থেকে নেওয়া হয় না — সার্ভার নিজে হিসাব করে। */
public record CheckoutRequest(List<Line> items, BigDecimal discount, PaymentMethod paymentMethod,
                              BigDecimal paidAmount, String customerName, String customerPhone) {
    public record Line(Long medicineId, int strips, int pieces) { }
}
