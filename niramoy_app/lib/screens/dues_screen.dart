import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../widgets/common.dart';

/// গ্রাহকের বকেয়া ও আদায়
class DuesScreen extends StatefulWidget {
  const DuesScreen({super.key});

  @override
  State<DuesScreen> createState() => _DuesScreenState();
}

class _DuesScreenState extends State<DuesScreen> {
  late Future<Dues> _future;

  @override
  void initState() {
    super.initState();
    _future = _fetch();
  }

  Future<Dues> _fetch() async {
    final j = await context.read<ApiClient>().get('/dues');
    return Dues.fromJson(j as Map<String, dynamic>);
  }

  Future<void> _reload() async {
    final next = _fetch();
    setState(() => _future = next);
    try {
      await next;
    } catch (_) {}
  }

  Future<void> _collect(Sale s) async {
    final amount = TextEditingController(text: s.dueAmount.toStringAsFixed(2));
    String method = 'CASH';
    final api = context.read<ApiClient>();

    final ok = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheet) => Padding(
          padding: EdgeInsets.fromLTRB(16, 16, 16, MediaQuery.of(ctx).viewInsets.bottom + 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('${s.customerName ?? 'গ্রাহক'} — ${s.invoiceNo}',
                  style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
              Text('মোট বকেয়া: ${money(s.dueAmount)}', style: const TextStyle(color: Brand.muted)),
              const SizedBox(height: 12),
              TextField(
                controller: amount,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: 'আদায়ের পরিমাণ (৳)', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 10),
              DropdownButtonFormField<String>(
                value: method,
                decoration: const InputDecoration(labelText: 'পদ্ধতি', border: OutlineInputBorder()),
                items: payMethods
                    .where((p) => p.code != 'CREDIT')
                    .map((p) => DropdownMenuItem<String>(value: p.code, child: Text(p.label)))
                    .toList(),
                onChanged: (v) => setSheet(() => method = v ?? 'CASH'),
              ),
              const SizedBox(height: 14),
              FilledButton(onPressed: () => Navigator.of(ctx).pop(true), child: const Text('আদায় নথিভুক্ত করুন')),
            ],
          ),
        ),
      ),
    );

    // বটম শিট বন্ধ হওয়ার অ্যানিমেশন চলার সময় controller dispose করলে ত্রুটি হয়, তাই এখানে করা হয়নি
    final value = double.tryParse(amount.text.trim());
    if (ok != true || !mounted) return;
    if (value == null || value <= 0) {
      showSnack(context, 'সঠিক পরিমাণ দিন।', error: true);
      return;
    }
    try {
      await api.post('/dues/${s.id}/collect', body: {'amount': value, 'method': method});
      if (!mounted) return;
      showSnack(context, 'আদায় নথিভুক্ত হয়েছে।');
      await _reload();
    } on ApiException catch (e) {
      if (mounted) showSnack(context, e.message, error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('গ্রাহকের বকেয়া')),
      body: FutureBuilder<Dues>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snap.hasError) {
            final msg = snap.error is ApiException ? (snap.error as ApiException).message : 'তথ্য আনা যায়নি।';
            return Center(child: ErrorView(message: msg, onRetry: _reload));
          }
          final d = snap.data!;
          return RefreshIndicator(
            onRefresh: _reload,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(12),
              children: [
                AppCard(
                  child: Row(
                    children: [
                      const Icon(Icons.account_balance_wallet_outlined, color: Brand.warn),
                      const SizedBox(width: 10),
                      const Expanded(child: Text('মোট প্রাপ্য বকেয়া')),
                      Text(money(d.total), style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
                    ],
                  ),
                ),
                if (d.items.isEmpty)
                  const Padding(
                    padding: EdgeInsets.all(30),
                    child: Center(child: Text('কোনো বকেয়া নেই।', style: TextStyle(color: Brand.muted))),
                  ),
                ...d.items.map((s) => AppCard(
                      onTap: () => _collect(s),
                      child: Row(
                        children: [
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(s.customerName ?? '—', style: const TextStyle(fontWeight: FontWeight.w700)),
                                Text('${s.customerPhone ?? ''} · ${s.invoiceNo}',
                                    style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                Text(dateText(s.createdAt), style: const TextStyle(color: Brand.muted, fontSize: 12)),
                              ],
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text(money(s.dueAmount),
                                  style: const TextStyle(fontWeight: FontWeight.w800, color: Brand.bad, fontSize: 16)),
                              const Text('আদায় করুন ›', style: TextStyle(color: Brand.green, fontSize: 12)),
                            ],
                          ),
                        ],
                      ),
                    )),
              ],
            ),
          );
        },
      ),
    );
  }
}
