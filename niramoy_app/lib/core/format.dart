import 'package:intl/intl.dart';

final NumberFormat _moneyFormat = NumberFormat('#,##0.00', 'en_US');

String money(num? v) => '৳${_moneyFormat.format(v ?? 0)}';

String dateText(DateTime? d) => d == null ? '' : DateFormat('dd/MM/yyyy').format(d);

String isoDate(DateTime d) => DateFormat('yyyy-MM-dd').format(d);

String dateTimeText(DateTime? d) => d == null ? '' : DateFormat('dd/MM/yyyy HH:mm').format(d);

/// BD মোবাইল: 01XXXXXXXXX (+88 / বাংলা অঙ্ক সহ গ্রহণযোগ্য)। সঠিক না হলে null
String? normalizePhone(String raw) {
  const bn = '০১২৩৪৫৬৭৮৯';
  final sb = StringBuffer();
  for (final ch in raw.split('')) {
    final i = bn.indexOf(ch);
    if (i >= 0) {
      sb.write(i);
    } else if (ch.trim().isNotEmpty && ch != '-' && ch != '(' && ch != ')') {
      sb.write(ch);
    }
  }
  var d = sb.toString();
  if (d.startsWith('+88')) {
    d = d.substring(3);
  } else if (d.startsWith('88') && d.length == 13) {
    d = d.substring(2);
  }
  return RegExp(r'^01[3-9][0-9]{8}$').hasMatch(d) ? d : null;
}

String stockText(int stock, int stripSize) {
  if (stripSize <= 1) return '$stock পিস';
  final strips = stock ~/ stripSize;
  final loose = stock % stripSize;
  if (strips == 0) return '$loose পিস';
  return loose == 0 ? '$strips পাতা' : '$strips পাতা $loose পিস';
}
