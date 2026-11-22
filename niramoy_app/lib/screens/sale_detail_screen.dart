import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../widgets/common.dart';

class SaleDetailScreen extends StatefulWidget {
  const SaleDetailScreen({super.key, required this.saleId, this.initial});

  final int saleId;
  final Sale? initial;

  @override
  State<SaleDetailScreen> createState() => _SaleDetailScreenState();
}

class _SaleDetailScreenState extends State<SaleDetailScreen> {
  Sale? _sale;
  String? _error;

  @override
  void initState() {
    super.initState();
    final i = widget.initial;
    if (i != null && i.items.isNotEmpty) {
      _sale = i;
    } else {
      _load();
    }
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final j = await context.read<ApiClient>().get('/sales/${widget.saleId}');
      if (!mounted) return;
      setState(() => _sale = Sale.fromJson(j as Map<String, dynamic>));
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = _sale;
    return Scaffold(
      appBar: AppBar(
        title: Text(s?.invoiceNo ?? 'ইনভয়েস'),
        actions: [
          if (s != null)
            IconButton(
              tooltip: 'রসিদ কপি করুন',
              icon: const Icon(Icons.copy_all_outlined),
              onPressed: () async {
                await Clipboard.setData(ClipboardData(text: s.toReceiptText()));
                if (context.mounted) showSnack(context, 'রসিদ কপি হয়েছে (WhatsApp/SMS-এ পেস্ট করুন)।');
              },
            ),
        ],
      ),
      body: s == null
          ? Center(
              child: _error == null ? const CircularProgressIndicator() : ErrorView(message: _error!, onRetry: _load),
            )
          : ListView(
              padding: const EdgeInsets.all(12),
              children: [
                AppCard(
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text('নিরাময় ফার্মেসি — ${s.branchName}',
                                style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                          ),
                          if (s.returned) const Pill('ফেরত সম্পন্ন', kind: PillKind.warn),
                        ],
                      ),
                      const Divider(),
                      KeyValueRow('ইনভয়েস', s.invoiceNo, bold: true),
                      KeyValueRow('সময়', dateTimeText(s.createdAt)),
                      if ((s.customerName ?? '').isNotEmpty) KeyValueRow('গ্রাহক', s.customerName!),
                      if ((s.customerPhone ?? '').isNotEmpty) KeyValueRow('মোবাইল', s.customerPhone!),
                      if ((s.cashier ?? '').isNotEmpty) KeyValueRow('বিক্রেতা', s.cashier!),
                    ],
                  ),
                ),
                AppCard(
                  child: Column(
                    children: [
                      ...s.items.map((i) => Padding(
                            padding: const EdgeInsets.symmetric(vertical: 6),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(i.name, style: const TextStyle(fontWeight: FontWeight.w600)),
                                      Text('${i.quantity} ${i.unit} × ${money(i.unitPrice)} · ${i.batchNo}',
                                          style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                    ],
                                  ),
                                ),
                                Text(money(i.lineTotal), style: const TextStyle(fontWeight: FontWeight.w700)),
                              ],
                            ),
                          )),
                    ],
                  ),
                ),
                AppCard(
                  child: Column(
                    children: [
                      KeyValueRow('উপমোট', money(s.subtotal)),
                      KeyValueRow('ছাড়', '−${money(s.discount)}'),
                      KeyValueRow('VAT (${s.vatPercent.toStringAsFixed(s.vatPercent == s.vatPercent.roundToDouble() ? 0 : 2)}%)', money(s.vatAmount)),
                      const Divider(),
                      KeyValueRow('সর্বমোট', money(s.total), bold: true),
                      KeyValueRow('পেমেন্ট', s.paymentLabel),
                      if (s.hasDue) KeyValueRow('পরিশোধিত', money(s.paid)),
                      if (s.hasDue) KeyValueRow('বকেয়া', money(s.dueAmount), bold: true),
                    ],
                  ),
                ),
              ],
            ),
    );
  }
}
