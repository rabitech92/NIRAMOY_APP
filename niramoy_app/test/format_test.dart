import 'package:flutter_test/flutter_test.dart';
import 'package:niramoy_pharmacy/core/format.dart';

void main() {
  test('টাকার ফরম্যাট', () {
    expect(money(1234.5), '৳1,234.50');
    expect(money(null), '৳0.00');
  });

  test('মোবাইল নম্বর যাচাই', () {
    expect(normalizePhone('01712345678'), '01712345678');
    expect(normalizePhone('+8801712345678'), '01712345678');
    expect(normalizePhone('০১৭১২৩৪৫৬৭৮'), '01712345678');
    expect(normalizePhone('017 1234-5678'), '01712345678');
    expect(normalizePhone('0171234567'), isNull);
    expect(normalizePhone('02712345678'), isNull);
  });

  test('ISO তারিখ', () {
    expect(isoDate(DateTime(2026, 11, 5)), '2026-11-05');
  });

  test('স্টক লেখা', () {
    expect(stockText(34, 10), '3 পাতা 4 পিস');
    expect(stockText(30, 10), '3 পাতা');
    expect(stockText(7, 10), '7 পিস');
    expect(stockText(5, 1), '5 পিস');
  });
}
