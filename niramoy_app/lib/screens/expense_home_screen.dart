import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/expense_models.dart';
import '../widgets/common.dart';
import 'expense_form_screen.dart';
import 'expense_list_screen.dart';

/// খরচ ব্যবস্থাপনার ড্যাশবোর্ড (আজ/মাস/বছর/বকেয়া, ক্যাটাগরি ও পেমেন্ট পদ্ধতি অনুযায়ী ভাগ)
class ExpenseHomeScreen extends StatefulWidget {
  const ExpenseHomeScreen({super.key});

  @override
  State<ExpenseHomeScreen> createState() => _ExpenseHomeScreenState();
}

class _ExpenseHomeScreenState extends State<ExpenseHomeScreen> {
  late Future<ExpenseDashboard> _future;

  @override
  void initState() {
    super.initState();
    _future = _fetch();
  }

  Future<ExpenseDashboard> _fetch() async {
    final j = await context.read<ApiClient>().get('/expenses/dashboard');
    return ExpenseDashboard.fromJson(j as Map<String, dynamic>);
  }

  Future<void> _reload() async {
    final next = _fetch();
    setState(() => _future = next);
    try {
      await next;
    } catch (_) {}
  }

  Future<void> _open(Widget page) async {
    await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => page));
    if (mounted) _reload();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('খরচ ব্যবস্থাপনা')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _open(const ExpenseFormScreen()),
        icon: const Icon(Icons.add),
        label: const Text('নতুন খরচ'),
      ),
      body: FutureBuilder<ExpenseDashboard>(
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
              padding: const EdgeInsets.fromLTRB(12, 12, 12, 90),
              children: [
                LayoutBuilder(builder: (context, c) {
                  final w = (c.maxWidth - 10) / 2;
                  final tiles = <Widget>[
                    StatTile('আজকের খরচ', money(d.today), Icons.today_outlined, Brand.navy),
                    StatTile('এই মাস (${d.monthLabel})', money(d.month), Icons.calendar_month_outlined, Brand.green),
                    StatTile('বকেয়া খরচ', money(d.pending), Icons.hourglass_bottom_outlined,
                        d.pending > 0 ? Brand.bad : Brand.muted),
                    StatTile('এই বছর (${d.yearNumber})', money(d.year), Icons.bar_chart_outlined, Brand.warn),
                  ];
                  return Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: tiles.map((t) => SizedBox(width: w, child: t)).toList(),
                  );
                }),
                const SizedBox(height: 12),
                OutlinedButton.icon(
                  onPressed: () => _open(const ExpenseListScreen()),
                  icon: const Icon(Icons.list_alt_outlined),
                  label: const Text('সব খরচ ও রিপোর্ট দেখুন'),
                ),
                _Breakdown(title: 'এই মাসের খরচ — ক্যাটাগরি অনুযায়ী', slices: d.byCategory),
                _Breakdown(title: 'এই মাসের পেমেন্ট — পদ্ধতি অনুযায়ী', slices: d.byPayment),
                const SectionTitle('সাম্প্রতিক খরচ'),
                if (d.recent.isEmpty)
                  const AppCard(child: Text('এখনো কোনো খরচ নথিভুক্ত হয়নি।', style: TextStyle(color: Brand.muted)))
                else
                  ...d.recent.map((e) => ExpenseTile(expense: e, onReturn: _reload)),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _Breakdown extends StatelessWidget {
  const _Breakdown({required this.title, required this.slices});

  final String title;
  final List<Slice> slices;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SectionTitle(title),
        AppCard(
          child: slices.isEmpty
              ? const Text('এই মাসে কোনো তথ্য নেই।', style: TextStyle(color: Brand.muted))
              : Column(
                  children: slices
                      .map((s) => Padding(
                            padding: const EdgeInsets.symmetric(vertical: 5),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  children: [
                                    Expanded(child: Text(s.label)),
                                    Text('${money(s.amount)} · ${s.percent}%',
                                        style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13)),
                                  ],
                                ),
                                const SizedBox(height: 4),
                                LinearProgressIndicator(
                                  value: s.percent / 100,
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
    );
  }
}
