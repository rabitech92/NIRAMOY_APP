import 'package:flutter/material.dart';

class Brand {
  static const green = Color(0xFF13865A);
  static const navy = Color(0xFF112B44);
  static const background = Color(0xFFF3F6F7);
  static const line = Color(0xFFE2E9ED);
  static const muted = Color(0xFF64798B);
  static const warn = Color(0xFF916010);
  static const warnBg = Color(0xFFFFF1D8);
  static const bad = Color(0xFFAC3935);
  static const badBg = Color(0xFFFEE8E6);
  static const goodBg = Color(0xFFE1F5EB);
}

class PayMethod {
  const PayMethod(this.code, this.label);
  final String code;
  final String label;
}

/// সার্ভারের PaymentMethod enum-এর সাথে মিল রাখতে হবে
const List<PayMethod> payMethods = [
  PayMethod('CASH', 'ক্যাশ'),
  PayMethod('BKASH', 'বিকাশ'),
  PayMethod('NAGAD', 'নগদ'),
  PayMethod('ROCKET', 'রকেট'),
  PayMethod('BANK', 'ব্যাংক'),
  PayMethod('CARD', 'কার্ড'),
  PayMethod('CREDIT', 'বকেয়া'),
];

/// খরচের পেমেন্টে যেসব পদ্ধতি চলে (সার্ভারের ExpenseService.PAY_METHODS)
final List<PayMethod> expensePayMethods = payMethods
    .where((p) => const ['CASH', 'BANK', 'BKASH', 'NAGAD', 'ROCKET', 'CARD'].contains(p.code))
    .toList();
