import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../widgets/common.dart';
import '../widgets/paged_list.dart';
import 'sale_detail_screen.dart';

/// গ্রাহকের নিয়মিত ওষুধ সাবস্ক্রিপশন (রিফিল) তালিকা
class RefillsScreen extends StatefulWidget {
  const RefillsScreen({super.key, this.initialFilter = ''});

  final String initialFilter;

  @override
  State<RefillsScreen> createState() => _RefillsScreenState();
}

class _RefillsScreenState extends State<RefillsScreen> {
  static const _filters = {
    '': 'সক্রিয় সব',
    'DUE': 'আজ বা দেরি',
    'WEEK': '৭ দিনের মধ্যে',
    'PAUSED': 'বিরতি',
    'ALL': 'সব',
  };

  Timer? _debounce;
  String _q = '';
  late String _filter = widget.initialFilter;

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }

  void _onSearch(String v) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      if (mounted) setState(() => _q = v.trim());
    });
  }

  Future<PageData<Refill>> _fetch(int page) async {
    final j = await context.read<ApiClient>().get('/refills', query: {'q': _q, 'filter': _filter, 'page': '$page'});
    return PageData<Refill>.fromJson(j, Refill.fromJson);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('নিয়মিত ওষুধ (রিফিল)')),
      body: Column(
        children: [
          SearchAndFilters(
            hint: 'গ্রাহকের নাম, মোবাইল বা প্ল্যান',
            onSearch: _onSearch,
            filters: _filters,
            selected: _filter,
            onFilter: (f) => setState(() => _filter = f),
          ),
          Expanded(
            child: PagedListView<Refill>(
              queryKey: '$_q|$_filter',
              fetch: _fetch,
              emptyText: 'কোনো সাবস্ক্রিপশন পাওয়া যায়নি।',
              itemBuilder: (context, r) => _RefillTile(refill: r),
            ),
          ),
        ],
      ),
    );
  }
}

Widget _duePill(Refill r) {
  if (!r.active) return Pill(r.statusLabel, kind: PillKind.warn);
  if (r.daysToNext < 0) return Pill('${-r.daysToNext} দিন দেরি', kind: PillKind.bad);
  if (r.daysToNext == 0) return const Pill('আজ', kind: PillKind.warn);
  return Pill('${r.daysToNext} দিন পরে');
}

class _RefillTile extends StatelessWidget {
  const _RefillTile({required this.refill});

  final Refill refill;

  @override
  Widget build(BuildContext context) {
    final r = refill;
    return AppCard(
      onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => RefillDetailScreen(refill: r))),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(r.customerName, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15))),
              _duePill(r),
            ],
          ),
          Text('${r.customerPhone} · ${r.planName}', style: const TextStyle(color: Brand.muted, fontSize: 12)),
          const SizedBox(height: 6),
          ...r.items.map((i) => Text('• ${i.medicineName} × ${i.quantity} ${i.unit}', style: const TextStyle(fontSize: 13))),
          const SizedBox(height: 6),
          Text('পরের রিফিল: ${dateText(r.nextRefillDate)} · ${r.frequencyDays} দিন পরপর',
              style: const TextStyle(fontSize: 12, color: Brand.muted)),
        ],
      ),
    );
  }
}

class RefillDetailScreen extends StatefulWidget {
  const RefillDetailScreen({super.key, required this.refill});

  final Refill refill;

  @override
  State<RefillDetailScreen> createState() => _RefillDetailScreenState();
}

class _RefillDetailScreenState extends State<RefillDetailScreen> {
  String _payment = 'CASH';
  bool _busy = false;

  Future<void> _bill() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('রিফিলের বিল'),
        content: const Text('স্টক থেকে ওষুধ কেটে বিল তৈরি হবে এবং পরের রিফিলের তারিখ এগিয়ে যাবে। এগোবেন?'),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('না')),
          FilledButton(onPressed: () => Navigator.of(ctx).pop(true), child: const Text('হ্যাঁ, বিল করুন')),
        ],
      ),
    );
    if (ok != true || !mounted) return;

    final api = context.read<ApiClient>();
    setState(() => _busy = true);
    try {
      final j = await api.post('/refills/${widget.refill.id}/bill', body: {'method': _payment});
      final sale = Sale.fromJson(j as Map<String, dynamic>);
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(builder: (_) => SaleDetailScreen(saleId: sale.id, initial: sale)),
      );
    } on ApiException catch (e) {
      if (mounted) showSnack(context, e.message, error: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = widget.refill;
    return Scaffold(
      appBar: AppBar(title: Text(r.customerName)),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          AppCard(
            child: Column(
              children: [
                Row(
                  children: [
                    Expanded(child: Text(r.planName, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16))),
                    _duePill(r),
                  ],
                ),
                const Divider(),
                KeyValueRow('মোবাইল', r.customerPhone, bold: true),
                KeyValueRow('কত দিন পরপর', '${r.frequencyDays} দিন'),
                KeyValueRow('পরের রিফিল', dateText(r.nextRefillDate), bold: true),
                KeyValueRow('ছাড়', '${r.discountPercent.toStringAsFixed(r.discountPercent == r.discountPercent.roundToDouble() ? 0 : 2)}%'),
                KeyValueRow('এ পর্যন্ত রিফিল', '${r.cycles} বার'),
                KeyValueRow('শাখা', r.branchName),
                if ((r.note ?? '').isNotEmpty) KeyValueRow('নোট', r.note!),
              ],
            ),
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('ওষুধের তালিকা', style: TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 6),
                ...r.items.map((i) => Padding(
                      padding: const EdgeInsets.symmetric(vertical: 3),
                      child: Row(
                        children: [
                          Expanded(child: Text(i.medicineName)),
                          Text('${i.quantity} ${i.unit}', style: const TextStyle(fontWeight: FontWeight.w700)),
                        ],
                      ),
                    )),
              ],
            ),
          ),
          OutlinedButton.icon(
            onPressed: () async {
              await Clipboard.setData(ClipboardData(text: r.customerPhone));
              if (context.mounted) showSnack(context, 'মোবাইল নম্বর কপি হয়েছে।');
            },
            icon: const Icon(Icons.copy_outlined),
            label: const Text('মোবাইল নম্বর কপি করুন'),
          ),
          if (r.active) ...[
            const SectionTitle('এখন রিফিলের বিল করুন'),
            AppCard(
              child: Column(
                children: [
                  DropdownButtonFormField<String>(
                    value: _payment,
                    decoration: const InputDecoration(labelText: 'পেমেন্ট পদ্ধতি', border: OutlineInputBorder()),
                    items: payMethods
                        .where((p) => p.code != 'CREDIT')
                        .map((p) => DropdownMenuItem<String>(value: p.code, child: Text(p.label)))
                        .toList(),
                    onChanged: (v) => setState(() => _payment = v ?? 'CASH'),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      onPressed: _busy ? null : _bill,
                      style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
                      icon: _busy
                          ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.receipt_long),
                      label: const Text('রিফিল বিল করুন'),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}
