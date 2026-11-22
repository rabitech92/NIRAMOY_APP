import 'package:flutter/material.dart';

import '../core/format.dart';
import '../models/models.dart';
import '../widgets/common.dart';

class MedicineDetailScreen extends StatelessWidget {
  const MedicineDetailScreen({super.key, required this.medicine});

  final Medicine medicine;

  @override
  Widget build(BuildContext context) {
    final m = medicine;
    return Scaffold(
      appBar: AppBar(title: const Text('ওষুধের বিবরণ')),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(m.name, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 6,
                  children: [
                    if (m.expired) const Pill('মেয়াদোত্তীর্ণ', kind: PillKind.bad),
                    if (!m.expired && m.expiringSoon) const Pill('মেয়াদ ঘনিয়ে এসেছে', kind: PillKind.warn),
                    if (m.bestSeller) const Pill('⭐ বেশি বিক্রিত', kind: PillKind.warn),
                    if (m.lowStock) const Pill('কম স্টক', kind: PillKind.warn),
                  ],
                ),
              ],
            ),
          ),
          AppCard(
            child: Column(
              children: [
                KeyValueRow('জেনেরিক নাম', m.genericName ?? '—'),
                KeyValueRow('কোম্পানি', m.companyName ?? '—'),
                KeyValueRow('ক্যাটাগরি', m.category ?? '—'),
                KeyValueRow('বারকোড', m.barcode ?? '—'),
                KeyValueRow('বক্স / তাক', m.boxNo ?? '—', bold: true),
                KeyValueRow('শাখা', m.branchName),
              ],
            ),
          ),
          AppCard(
            child: Column(
              children: [
                KeyValueRow('ব্যাচ', m.batchNo),
                KeyValueRow('মেয়াদ', dateText(m.expiryDate)),
                KeyValueRow('স্টক', m.stockLabel, bold: true),
                KeyValueRow('মোট পিস', '${m.stock}'),
                KeyValueRow('এক পাতায় পিস', '${m.stripSize}'),
              ],
            ),
          ),
          AppCard(
            child: Column(
              children: [
                KeyValueRow('পিস বিক্রয়মূল্য', money(m.price)),
                if (m.stripSize > 1) KeyValueRow('পাতা বিক্রয়মূল্য', money(m.stripPrice)),
                if (m.costPrice != null) KeyValueRow('পিস ক্রয়মূল্য', money(m.costPrice)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
