import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/business_models.dart';
import '../widgets/common.dart';

/// ক্রয়-বিক্রয় ও স্টক হিসাব: স্টকে কত টাকার মাল, মাসে/বছরে কত ক্রয়-বিক্রয় ও লাভ
class BusinessScreen extends StatefulWidget {
  const BusinessScreen({super.key});

  @override
  State<BusinessScreen> createState() => _BusinessScreenState();
}

class _BusinessScreenState extends State<BusinessScreen> {
  late Future<BusinessSummary> _future;
  bool _yearly = false;

  @override
  void initState() {
    super.initState();
    _future = _fetch();
  }

  Future<BusinessSummary> _fetch() async {
    final j = await context.read<ApiClient>().get('/business');
    return BusinessSummary.fromJson(j);
  }

  Future<void> _reload() async {
    final next = _fetch();
    setState(() => _future = next);
    try {
      await next;
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ক্রয়-বিক্রয় ও স্টক হিসাব')),
      body: FutureBuilder<BusinessSummary>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snap.hasError) {
            final msg = snap.error is ApiException ? (snap.error as ApiException).message : 'তথ্য আনা যায়নি।';
            return Center(child: ErrorView(message: msg, onRetry: _reload));
          }
          final s = snap.data!;
          final rows = _yearly ? s.years : s.months;
          return RefreshIndicator(
            onRefresh: _reload,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(12),
              children: [
                const SectionTitle('বর্তমান স্টক'),
                _grid([
                  StatTile('স্টকে মালের মূল্য (ক্রয়মূল্যে)', money(s.stock.costValue), Icons.inventory_2_outlined, Brand.green),
                  StatTile('স্টকের বিক্রয়মূল্য', money(s.stock.saleValue), Icons.sell_outlined, Brand.navy),
                  StatTile('সম্ভাব্য লাভ', money(s.stock.potentialProfit), Icons.savings_outlined, Brand.navy),
                  StatTile('মোট পিস · ব্যাচ', '${s.stock.units} · ${s.stock.batches}', Icons.numbers, Brand.muted),
                  StatTile('মেয়াদোত্তীর্ণ মাল', money(s.stock.expiredCost), Icons.event_busy_outlined,
                      s.stock.expiredCost > 0 ? Brand.bad : Brand.muted),
                  StatTile('৯০ দিনে মেয়াদ শেষ', money(s.stock.expiringCost), Icons.hourglass_bottom_outlined,
                      s.stock.expiringCost > 0 ? Brand.warn : Brand.muted),
                ]),
                const SectionTitle('বিক্রয় ও ক্রয় এক নজরে'),
                _grid([
                  StatTile('আজকের বিক্রয়', money(s.salesToday), Icons.today_outlined, Brand.green),
                  StatTile('এই সপ্তাহের বিক্রয়', money(s.salesWeek), Icons.date_range_outlined, Brand.green),
                  StatTile('এই মাসের বিক্রয়', money(s.salesMonth), Icons.calendar_month_outlined, Brand.green),
                  StatTile('এই বছরের বিক্রয়', money(s.salesYear), Icons.event_outlined, Brand.green),
                  StatTile('এই মাসের ক্রয় (নিট)', money(s.purchaseMonth), Icons.shopping_bag_outlined, Brand.warn),
                  StatTile('এই বছরের ক্রয় (নিট)', money(s.purchaseYear), Icons.shopping_bag_outlined, Brand.warn),
                  StatTile('সর্বমোট বিক্রয়', money(s.salesTotal), Icons.all_inclusive, Brand.navy),
                  StatTile('সর্বমোট ক্রয় (নিট)', money(s.purchaseTotal), Icons.all_inclusive, Brand.navy),
                ]),
                const SectionTitle('মাসভিত্তিক ক্রয় বনাম বিক্রয়'),
                AppCard(child: _DualBars(labels: s.chartLabels, purchases: s.chartPurchases, sales: s.chartSales)),
                const SectionTitle('মাস / বছর অনুযায়ী হিসাব'),
                Row(
                  children: [
                    ChoiceChip(label: const Text('মাসিক (১২ মাস)'), selected: !_yearly, onSelected: (_) => setState(() => _yearly = false)),
                    const SizedBox(width: 8),
                    ChoiceChip(label: const Text('বার্ষিক'), selected: _yearly, onSelected: (_) => setState(() => _yearly = true)),
                  ],
                ),
                const SizedBox(height: 8),
                ...rows.map((r) => AppCard(
                      child: Column(
                        children: [
                          Row(
                            children: [
                              Expanded(child: Text(r.label, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15))),
                              Text('${r.invoices}টি ইনভয়েস', style: const TextStyle(color: Brand.muted, fontSize: 12)),
                            ],
                          ),
                          const Divider(),
                          KeyValueRow('নিট ক্রয়', money(r.netPurchases), bold: true),
                          if (r.returns > 0.004) KeyValueRow('(ক্রয় ${money(r.purchases)} − ফেরত ${money(r.returns)})', ''),
                          KeyValueRow('বিক্রয়', money(r.sales), bold: true),
                          KeyValueRow('লাভ', money(r.profit)),
                          KeyValueRow(_yearly ? 'বছর শেষে স্টক' : 'মাস শেষে স্টক', r.stockEnd == null ? '—' : money(r.stockEnd)),
                        ],
                      ),
                    )),
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 4),
                  child: Text(
                    'লাভ = বিক্রয়মূল্য − ক্রয়মূল্য − ছাড় (দোকানের খরচ বাদ নয়)। "শেষে স্টক" চালুর দিন থেকে জমা হয়, আগেরগুলো "—"।',
                    style: TextStyle(color: Brand.muted, fontSize: 12),
                  ),
                ),
                if (s.byBranch.length > 1) _valueCard('শাখা অনুযায়ী স্টকের মূল্য', s.byBranch),
                _valueCard('ক্যাটাগরি অনুযায়ী স্টকের মূল্য', s.byCategory),
                _valueCard('সবচেয়ে বেশি টাকা আটকে আছে (শীর্ষ ১০)', s.topStock),
                const SizedBox(height: 16),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _grid(List<Widget> tiles) {
    return LayoutBuilder(builder: (context, c) {
      final w = (c.maxWidth - 10) / 2;
      return Wrap(
        spacing: 10,
        runSpacing: 10,
        children: tiles.map((t) => SizedBox(width: w, child: t)).toList(),
      );
    });
  }

  Widget _valueCard(String title, List<BizValueRow> rows) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SectionTitle(title),
        AppCard(
          child: rows.isEmpty
              ? const Text('স্টকে কোনো মাল নেই।', style: TextStyle(color: Brand.muted))
              : Column(
                  children: rows
                      .map((v) => Padding(
                            padding: const EdgeInsets.symmetric(vertical: 5),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(v.name, style: const TextStyle(fontWeight: FontWeight.w600)),
                                      Text('${v.units} পিস · বিক্রয়মূল্য ${money(v.sale)}',
                                          style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                    ],
                                  ),
                                ),
                                Text(money(v.cost), style: const TextStyle(fontWeight: FontWeight.w800)),
                              ],
                            ),
                          ))
                      .toList(),
                ),
        ),
      ],
    );
  }
}

/// প্রতি মাসের জন্য দুটি বার: ক্রয় (হলুদ) ও বিক্রয় (সবুজ)
class _DualBars extends StatelessWidget {
  const _DualBars({required this.labels, required this.purchases, required this.sales});

  final List<String> labels;
  final List<double> purchases;
  final List<double> sales;

  @override
  Widget build(BuildContext context) {
    final n = labels.length;
    var maxV = 0.0;
    for (var i = 0; i < n; i++) {
      if (i < purchases.length && purchases[i] > maxV) maxV = purchases[i];
      if (i < sales.length && sales[i] > maxV) maxV = sales[i];
    }
    const barArea = 110.0;
    double h(double v) => maxV <= 0 ? 2.0 : (v / maxV * barArea).clamp(2.0, barArea).toDouble();

    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: const [
            _Legend(color: Brand.warn, text: 'ক্রয়'),
            SizedBox(width: 16),
            _Legend(color: Brand.green, text: 'বিক্রয়'),
          ],
        ),
        const SizedBox(height: 8),
        SizedBox(
          height: barArea + 30,
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: List.generate(n, (i) {
              final p = i < purchases.length ? purchases[i] : 0.0;
              final s = i < sales.length ? sales[i] : 0.0;
              return Expanded(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          width: 6,
                          height: h(p),
                          decoration: const BoxDecoration(
                              color: Brand.warn, borderRadius: BorderRadius.vertical(top: Radius.circular(3))),
                        ),
                        const SizedBox(width: 1),
                        Container(
                          width: 6,
                          height: h(s),
                          decoration: const BoxDecoration(
                              color: Brand.green, borderRadius: BorderRadius.vertical(top: Radius.circular(3))),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(labels[i], style: const TextStyle(fontSize: 8), maxLines: 1, overflow: TextOverflow.clip),
                  ],
                ),
              );
            }),
          ),
        ),
      ],
    );
  }
}

class _Legend extends StatelessWidget {
  const _Legend({required this.color, required this.text});

  final Color color;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(width: 10, height: 10, decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(2))),
        const SizedBox(width: 4),
        Text(text, style: const TextStyle(fontSize: 12)),
      ],
    );
  }
}
