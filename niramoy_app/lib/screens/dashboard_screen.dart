import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../state/auth_state.dart';
import '../widgets/bar_chart.dart';
import '../widgets/common.dart';
import 'refills_screen.dart';
import 'sale_detail_screen.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  late Future<Dashboard> _future;

  @override
  void initState() {
    super.initState();
    _future = _fetch();
  }

  Future<Dashboard> _fetch() async {
    final j = await context.read<ApiClient>().get('/dashboard');
    return Dashboard.fromJson(j as Map<String, dynamic>);
  }

  Future<void> _refresh() async {
    final next = _fetch();
    setState(() => _future = next);
    try {
      await next;
    } catch (_) {
      // ত্রুটি FutureBuilder-ই দেখাবে
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = context.watch<AuthState>().user!;
    return FutureBuilder<Dashboard>(
      future: _future,
      builder: (context, snap) {
        if (snap.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snap.hasError) {
          final msg = snap.error is ApiException ? (snap.error as ApiException).message : 'তথ্য আনা যায়নি।';
          return Center(child: ErrorView(message: msg, onRetry: _refresh));
        }
        final d = snap.data!;
        return RefreshIndicator(
          onRefresh: _refresh,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(12),
            children: [
              Text('স্বাগতম, ${user.fullName}',
                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700, color: Brand.navy)),
              Text(user.roleLabel, style: const TextStyle(color: Brand.muted)),
              const SizedBox(height: 12),
              LayoutBuilder(builder: (context, c) {
                final w = (c.maxWidth - 10) / 2;
                final tiles = <Widget>[
                  _Stat('আজকের বিক্রয়', money(d.todaySales), Icons.trending_up, Brand.green),
                  if (d.todayProfit != null) _Stat('আজকের লাভ', money(d.todayProfit), Icons.savings_outlined, Brand.navy),
                  if (d.receivable != null) _Stat('গ্রাহক বকেয়া', money(d.receivable), Icons.account_balance_wallet_outlined, Brand.warn),
                  _Stat('কম স্টক', '${d.lowStock} টি', Icons.inventory_2_outlined, d.lowStock > 0 ? Brand.bad : Brand.muted),
                  _Stat('মেয়াদ ঘনিয়ে এসেছে', '${d.expiring} ব্যাচ', Icons.event_busy_outlined, d.expiring > 0 ? Brand.warn : Brand.muted),
                  if (user.canSell)
                    _Stat('রিফিল ডিউ', '${d.dueRefills} জন', Icons.repeat, d.dueRefills > 0 ? Brand.bad : Brand.muted,
                        onTap: () => Navigator.of(context).push(
                            MaterialPageRoute<void>(builder: (_) => const RefillsScreen(initialFilter: 'DUE')))),
                ];
                return Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: tiles.map((t) => SizedBox(width: w, child: t)).toList(),
                );
              }),
              const SectionTitle('গত ৭ দিনের বিক্রয়'),
              AppCard(child: SimpleBarChart(data: d.trend)),
              if (d.paymentMix.isNotEmpty) ...[
                const SectionTitle('পেমেন্ট সারাংশ (৭ দিন)'),
                AppCard(
                  child: Column(
                    children: d.paymentMix
                        .map((m) => Padding(
                              padding: const EdgeInsets.symmetric(vertical: 5),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                    children: [Text(m.label), Text('${m.percent}%', style: const TextStyle(fontWeight: FontWeight.w700))],
                                  ),
                                  const SizedBox(height: 4),
                                  LinearProgressIndicator(
                                    value: m.percent / 100,
                                    minHeight: 8,
                                    borderRadius: BorderRadius.circular(8),
                                    color: Brand.green,
                                    backgroundColor: const Color(0xFFEEF2F4),
                                  ),
                                ],
                              ),
                            ))
                        .toList(),
                  ),
                ),
              ],
              if (user.canSell) ...[
                const SectionTitle('সাম্প্রতিক বিক্রয়'),
                if (d.recentSales.isEmpty)
                  const AppCard(child: Text('এখনো কোনো বিক্রয় নেই।', style: TextStyle(color: Brand.muted)))
                else
                  ...d.recentSales.map((s) => SaleTile(sale: s)),
              ],
            ],
          ),
        );
      },
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat(this.label, this.value, this.icon, this.color, {this.onTap});

  final String label;
  final String value;
  final IconData icon;
  final Color color;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      margin: EdgeInsets.zero,
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color),
          const SizedBox(height: 10),
          Text(label, style: const TextStyle(color: Brand.muted, fontSize: 12)),
          const SizedBox(height: 2),
          Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}

/// ইনভয়েসের তালিকার একটি সারি (ড্যাশবোর্ড ও ইনভয়েস তালিকায় ব্যবহৃত)
class SaleTile extends StatelessWidget {
  const SaleTile({super.key, required this.sale});

  final Sale sale;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      onTap: () => Navigator.of(context).push(
        MaterialPageRoute<void>(builder: (_) => SaleDetailScreen(saleId: sale.id)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(sale.invoiceNo, style: const TextStyle(fontWeight: FontWeight.w700)),
                const SizedBox(height: 2),
                Text(
                  (sale.customerName ?? '').isEmpty ? dateTimeText(sale.createdAt) : '${sale.customerName} · ${dateTimeText(sale.createdAt)}',
                  style: const TextStyle(color: Brand.muted, fontSize: 12),
                ),
                const SizedBox(height: 6),
                Wrap(
                  spacing: 6,
                  children: [
                    Pill(sale.paymentLabel),
                    if (sale.returned) const Pill('ফেরত', kind: PillKind.warn),
                    if (sale.hasDue) Pill('বকেয়া ${money(sale.dueAmount)}', kind: PillKind.bad),
                  ],
                ),
              ],
            ),
          ),
          Text(money(sale.total), style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
        ],
      ),
    );
  }
}
