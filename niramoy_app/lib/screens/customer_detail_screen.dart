import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/insight_models.dart';
import '../widgets/common.dart';
import 'sale_detail_screen.dart';

/// একজন গ্রাহকের ক্রয়ের বিস্তারিত: জীবনকাল/আজ/সপ্তাহ/মাস/বছর, দৈনিক-সাপ্তাহিক-মাসিক-বার্ষিক, বেশি কেনা ওষুধ, কেনাকাটার তালিকা
class CustomerDetailScreen extends StatefulWidget {
  const CustomerDetailScreen({super.key, required this.phone});

  final String phone;

  @override
  State<CustomerDetailScreen> createState() => _CustomerDetailScreenState();
}

class _CustomerDetailScreenState extends State<CustomerDetailScreen> {
  late Future<CustomerDetail> _future;
  String _tab = 'daily';

  static const _tabs = {'daily': 'দৈনিক', 'weekly': 'সাপ্তাহিক', 'monthly': 'মাসিক', 'yearly': 'বার্ষিক'};

  @override
  void initState() {
    super.initState();
    _future = _fetch();
  }

  Future<CustomerDetail> _fetch() async {
    final j = await context.read<ApiClient>().get('/customers/${widget.phone}');
    return CustomerDetail.fromJson(j as Map<String, dynamic>);
  }

  Future<void> _reload() async {
    final next = _fetch();
    setState(() => _future = next);
    try {
      await next;
    } catch (_) {}
  }

  List<Bucket> _buckets(CustomerDetail c) {
    switch (_tab) {
      case 'weekly':
        return c.weekly;
      case 'monthly':
        return c.monthly;
      case 'yearly':
        return c.yearly;
      default:
        return c.daily;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('গ্রাহকের বিস্তারিত')),
      body: FutureBuilder<CustomerDetail>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snap.hasError) {
            final msg = snap.error is ApiException ? (snap.error as ApiException).message : 'তথ্য আনা যায়নি।';
            return Center(child: ErrorView(message: msg, onRetry: _reload));
          }
          final c = snap.data!;
          final buckets = _buckets(c);
          return RefreshIndicator(
            onRefresh: _reload,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(12),
              children: [
                AppCard(
                  child: Row(
                    children: [
                      const CircleAvatar(backgroundColor: Brand.goodBg, child: Icon(Icons.person, color: Brand.green)),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(c.name, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 17)),
                            Text(c.phone, style: const TextStyle(color: Brand.muted)),
                            Text('প্রথম ক্রয়: ${dateText(c.firstAt)} · শেষ ক্রয়: ${dateText(c.lastAt)}',
                                style: const TextStyle(color: Brand.muted, fontSize: 12)),
                          ],
                        ),
                      ),
                      IconButton(
                        tooltip: 'নম্বর কপি করুন',
                        icon: const Icon(Icons.copy_outlined),
                        onPressed: () async {
                          await Clipboard.setData(ClipboardData(text: c.phone));
                          if (context.mounted) showSnack(context, 'মোবাইল নম্বর কপি হয়েছে।');
                        },
                      ),
                    ],
                  ),
                ),
                LayoutBuilder(builder: (context, box) {
                  final w = (box.maxWidth - 10) / 2;
                  final tiles = <Widget>[
                    StatTile('জীবনকালের মোট ক্রয়', money(c.lifetime), Icons.all_inclusive, Brand.green),
                    StatTile('আজ', money(c.today), Icons.today_outlined, Brand.navy),
                    StatTile('এই সপ্তাহ', money(c.week), Icons.date_range_outlined, Brand.navy),
                    StatTile('এই মাস', money(c.month), Icons.calendar_month_outlined, Brand.navy),
                    StatTile('এই বছর', money(c.year), Icons.event_outlined, Brand.navy),
                    StatTile('ইনভয়েস', '${c.invoices}টি', Icons.receipt_long_outlined, Brand.muted),
                    StatTile('গড় ইনভয়েস', money(c.average), Icons.functions, Brand.muted),
                    StatTile('বকেয়া', money(c.due), Icons.account_balance_wallet_outlined, c.due > 0 ? Brand.bad : Brand.muted),
                  ];
                  return Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: tiles.map((t) => SizedBox(width: w, child: t)).toList(),
                  );
                }),
                const SectionTitle('সময় অনুযায়ী ক্রয়'),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: _tabs.entries
                        .map((e) => Padding(
                              padding: const EdgeInsets.only(right: 8),
                              child: ChoiceChip(
                                label: Text(e.value),
                                selected: _tab == e.key,
                                onSelected: (_) => setState(() => _tab = e.key),
                              ),
                            ))
                        .toList(),
                  ),
                ),
                const SizedBox(height: 8),
                AppCard(
                  child: buckets.isEmpty
                      ? const Text('এই সময়ে কোনো ক্রয় নেই।', style: TextStyle(color: Brand.muted))
                      : Column(
                          children: buckets
                              .map((b) => Padding(
                                    padding: const EdgeInsets.symmetric(vertical: 5),
                                    child: Row(
                                      children: [
                                        Expanded(child: Text(b.label)),
                                        Text('${b.invoices}টি', style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                        const SizedBox(width: 12),
                                        Text(money(b.amount), style: const TextStyle(fontWeight: FontWeight.w700)),
                                      ],
                                    ),
                                  ))
                              .toList(),
                        ),
                ),
                const SectionTitle('সবচেয়ে বেশি কেনা ওষুধ'),
                AppCard(
                  child: c.topItems.isEmpty
                      ? const Text('কোনো তথ্য নেই।', style: TextStyle(color: Brand.muted))
                      : Column(
                          children: c.topItems
                              .asMap()
                              .entries
                              .map((e) => Padding(
                                    padding: const EdgeInsets.symmetric(vertical: 5),
                                    child: Row(
                                      children: [
                                        SizedBox(width: 24, child: Text('${e.key + 1}', style: const TextStyle(color: Brand.muted))),
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text(e.value.name, style: const TextStyle(fontWeight: FontWeight.w600)),
                                              Text('${e.value.times} বার · ${e.value.pieces} পিস',
                                                  style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                            ],
                                          ),
                                        ),
                                        Text(money(e.value.amount), style: const TextStyle(fontWeight: FontWeight.w700)),
                                      ],
                                    ),
                                  ))
                              .toList(),
                        ),
                ),
                const SectionTitle('কেনাকাটার তালিকা (সাম্প্রতিক ৩০টি)'),
                ...c.recent.map((s) => AppCard(
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute<void>(builder: (_) => SaleDetailScreen(saleId: s.id, initial: s)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(child: Text(s.invoiceNo, style: const TextStyle(fontWeight: FontWeight.w700))),
                              Text(money(s.total), style: const TextStyle(fontWeight: FontWeight.w800)),
                            ],
                          ),
                          Text(dateTimeText(s.createdAt), style: const TextStyle(color: Brand.muted, fontSize: 12)),
                          const SizedBox(height: 4),
                          ...s.items.map((i) => Text('• ${i.name} × ${i.quantity} ${i.unit}', style: const TextStyle(fontSize: 13))),
                          if (s.hasDue) ...[
                            const SizedBox(height: 4),
                            Pill('বকেয়া ${money(s.dueAmount)}', kind: PillKind.bad),
                          ],
                        ],
                      ),
                    )),
                const SizedBox(height: 16),
              ],
            ),
          );
        },
      ),
    );
  }
}
