import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/insight_models.dart';
import '../state/auth_state.dart';
import '../widgets/common.dart';

/// কোন ওষুধ বেশি বিক্রি হয় তার তালিকা এবং "বেশি বিক্রিত" ⭐ চিহ্নিত করা
class BestSellersScreen extends StatefulWidget {
  const BestSellersScreen({super.key});

  @override
  State<BestSellersScreen> createState() => _BestSellersScreenState();
}

class _BestSellersScreenState extends State<BestSellersScreen> {
  static const _days = {7: '৭ দিন', 30: '৩০ দিন', 90: '৯০ দিন', 365: '১ বছর', 0: 'সব সময়'};
  static const _sorts = {'qty': 'পরিমাণ', 'revenue': 'টাকা', 'orders': 'ইনভয়েস'};

  int _day = 30;
  String _sort = 'qty';
  final int _limit = 20;
  bool _busy = false;

  BestSellers? _data;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final j = await context.read<ApiClient>().get('/best-sellers', query: {
        'days': '$_day',
        'sort': _sort,
        'limit': '$_limit',
      });
      if (!mounted) return;
      setState(() => _data = BestSellers.fromJson(j));
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _act(String path, {String? name}) async {
    final api = context.read<ApiClient>();
    setState(() => _busy = true);
    try {
      final j = await api.post('/best-sellers/$path', body: {
        'name': name,
        'days': _day,
        'sort': _sort,
        'limit': _limit,
      });
      if (!mounted) return;
      final d = BestSellers.fromJson(j);
      setState(() => _data = d);
      if ((d.message ?? '').isNotEmpty) showSnack(context, d.message!);
    } on ApiException catch (e) {
      if (mounted) showSnack(context, e.message, error: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final canMark = context.watch<AuthState>().user?.canMark ?? false;
    final d = _data;
    return Scaffold(
      appBar: AppBar(title: const Text('বেশি বিক্রিত ওষুধ')),
      body: d == null
          ? Center(child: _error == null ? const CircularProgressIndicator() : ErrorView(message: _error!, onRetry: _load))
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(12),
                children: [
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: _days.entries
                          .map((e) => Padding(
                                padding: const EdgeInsets.only(right: 8),
                                child: ChoiceChip(
                                  label: Text(e.value),
                                  selected: _day == e.key,
                                  onSelected: (_) {
                                    setState(() => _day = e.key);
                                    _load();
                                  },
                                ),
                              ))
                          .toList(),
                    ),
                  ),
                  const SizedBox(height: 6),
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        const Padding(padding: EdgeInsets.only(right: 8), child: Text('সাজানো:', style: TextStyle(color: Brand.muted))),
                        ..._sorts.entries.map((e) => Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: FilterChip(
                                label: Text(e.value),
                                selected: _sort == e.key,
                                onSelected: (_) {
                                  setState(() => _sort = e.key);
                                  _load();
                                },
                              ),
                            )),
                      ],
                    ),
                  ),
                  if (canMark) ...[
                    const SizedBox(height: 8),
                    OutlinedButton.icon(
                      onPressed: _busy ? null : () => _act('mark-top'),
                      icon: const Icon(Icons.star),
                      label: Text('শীর্ষ ${d.rows.length}টি একসাথে চিহ্নিত করুন'),
                    ),
                  ],
                  SectionTitle('র‍্যাংকিং — ${d.periodLabel}'),
                  if (d.rows.isEmpty)
                    const AppCard(child: Text('এই সময়ে কোনো বিক্রয় নেই।', style: TextStyle(color: Brand.muted)))
                  else
                    ...d.rows.asMap().entries.map((e) => _row(e.key + 1, e.value, canMark)),
                  SectionTitle('চিহ্নিত ওষুধ (${d.marks.length})'),
                  if (d.marks.isEmpty)
                    const AppCard(child: Text('এখনো কোনো ওষুধ চিহ্নিত করা হয়নি।', style: TextStyle(color: Brand.muted)))
                  else
                    ...d.marks.map((m) => AppCard(
                          child: Row(
                            children: [
                              const Text('⭐ ', style: TextStyle(fontSize: 18)),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(m.name, style: const TextStyle(fontWeight: FontWeight.w700)),
                                    Text('${m.soldPieces} পিস · ${money(m.revenue)} · ${m.periodLabel ?? ''}',
                                        style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                    Text('${dateText(m.markedAt)} · ${m.markedBy ?? ''}${m.auto ? ' · স্বয়ংক্রিয়' : ''}',
                                        style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                  ],
                                ),
                              ),
                              if (canMark)
                                IconButton(
                                  tooltip: 'চিহ্ন তুলুন',
                                  icon: const Icon(Icons.star_border, color: Brand.bad),
                                  onPressed: _busy ? null : () => _act('unmark', name: m.name),
                                ),
                            ],
                          ),
                        )),
                  const SizedBox(height: 16),
                ],
              ),
            ),
    );
  }

  Widget _row(int rank, SellerRow r, bool canMark) {
    return AppCard(
      child: Row(
        children: [
          SizedBox(width: 28, child: Text('$rank', style: const TextStyle(color: Brand.muted, fontWeight: FontWeight.w700))),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(child: Text(r.name, style: const TextStyle(fontWeight: FontWeight.w700))),
                    if (r.marked) const Padding(padding: EdgeInsets.only(left: 6), child: Text('⭐')),
                  ],
                ),
                const SizedBox(height: 2),
                Text('${r.pieces} পিস · ${money(r.revenue)} · ${r.invoices}টি ইনভয়েস',
                    style: const TextStyle(color: Brand.muted, fontSize: 12)),
                const SizedBox(height: 4),
                Wrap(
                  spacing: 6,
                  children: [
                    Pill('স্টক ${r.stock} পিস'),
                    if (r.lowForSales) const Pill('বিক্রির তুলনায় কম', kind: PillKind.bad),
                  ],
                ),
              ],
            ),
          ),
          if (canMark && !r.marked)
            IconButton(
              tooltip: 'চিহ্নিত করুন',
              icon: const Icon(Icons.star_outline, color: Brand.warn),
              onPressed: _busy ? null : () => _act('mark', name: r.name),
            ),
        ],
      ),
    );
  }
}
