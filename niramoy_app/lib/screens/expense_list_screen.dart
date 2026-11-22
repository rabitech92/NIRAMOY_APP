import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/expense_models.dart';
import '../models/models.dart';
import '../widgets/common.dart';
import '../widgets/paged_list.dart';
import 'expense_detail_screen.dart';

/// খরচের তালিকা ও রিপোর্ট: অনুসন্ধান, অবস্থা ও সময় (আজ/সপ্তাহ/মাস/বছর) অনুযায়ী ফিল্টার
class ExpenseListScreen extends StatefulWidget {
  const ExpenseListScreen({super.key});

  @override
  State<ExpenseListScreen> createState() => _ExpenseListScreenState();
}

class _ExpenseListScreenState extends State<ExpenseListScreen> {
  static const _statuses = {'': 'সব', 'PAID': 'পরিশোধিত', 'PARTIAL': 'আংশিক', 'UNPAID': 'বকেয়া'};
  static const _ranges = {'TODAY': 'আজ', 'WEEK': 'এই সপ্তাহ', 'MONTH': 'এই মাস', 'YEAR': 'এই বছর'};

  Timer? _debounce;
  String _q = '';
  String _status = '';
  String _range = 'MONTH';
  double _total = 0;
  double _paid = 0;

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

  /// [from, to] (yyyy-MM-dd)। সপ্তাহ শুরু শনিবার থেকে
  List<String> _dates() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    DateTime from;
    switch (_range) {
      case 'TODAY':
        from = today;
        break;
      case 'WEEK':
        from = today.subtract(Duration(days: (today.weekday + 1) % 7));
        break;
      case 'YEAR':
        from = DateTime(today.year, 1, 1);
        break;
      default:
        from = DateTime(today.year, today.month, 1);
    }
    return [isoDate(from), isoDate(today)];
  }

  Future<PageData<Expense>> _fetch(int page) async {
    final d = _dates();
    final j = await context.read<ApiClient>().get('/expenses', query: {
      'q': _q,
      'status': _status,
      'from': d[0],
      'to': d[1],
      'page': '$page',
    });
    final list = ExpenseList.fromJson(j);
    if (page == 0 && mounted) {
      setState(() {
        _total = list.totalAmount;
        _paid = list.totalPaid;
      });
    }
    return list.page;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('খরচের তালিকা')),
      body: Column(
        children: [
          SearchAndFilters(
            hint: 'নং, কাকে দেওয়া, রেফারেন্স বা বিবরণ',
            onSearch: _onSearch,
            filters: _statuses,
            selected: _status,
            onFilter: (f) => setState(() => _status = f),
          ),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 4),
            child: Row(
              children: _ranges.entries
                  .map((e) => Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: FilterChip(
                          label: Text(e.value),
                          selected: _range == e.key,
                          onSelected: (_) => setState(() => _range = e.key),
                        ),
                      ))
                  .toList(),
            ),
          ),
          Container(
            margin: const EdgeInsets.fromLTRB(12, 4, 12, 4),
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: Brand.line),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _totalCell('মোট খরচ', money(_total), Brand.navy),
                _totalCell('পরিশোধিত', money(_paid), Brand.green),
                _totalCell('বকেয়া', money(_total - _paid), Brand.bad),
              ],
            ),
          ),
          Expanded(
            child: PagedListView<Expense>(
              queryKey: '$_q|$_status|$_range',
              fetch: _fetch,
              emptyText: 'এই সময়ে কোনো খরচ পাওয়া যায়নি।',
              itemBuilder: (context, e) => ExpenseTile(expense: e),
            ),
          ),
        ],
      ),
    );
  }

  Widget _totalCell(String label, String value, Color color) {
    return Column(
      children: [
        Text(label, style: const TextStyle(color: Brand.muted, fontSize: 12)),
        const SizedBox(height: 2),
        Text(value, style: TextStyle(fontWeight: FontWeight.w800, color: color)),
      ],
    );
  }
}

PillKind _statusKind(String status) {
  if (status == 'PAID') return PillKind.good;
  if (status == 'PARTIAL') return PillKind.warn;
  return PillKind.bad;
}

/// খরচের তালিকার একটি সারি
class ExpenseTile extends StatelessWidget {
  const ExpenseTile({super.key, required this.expense, this.onReturn});

  final Expense expense;
  final VoidCallback? onReturn;

  @override
  Widget build(BuildContext context) {
    final e = expense;
    return AppCard(
      onTap: () async {
        await Navigator.of(context).push(
          MaterialPageRoute<void>(builder: (_) => ExpenseDetailScreen(expenseId: e.id)),
        );
        onReturn?.call();
      },
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(e.categoryName, style: const TextStyle(fontWeight: FontWeight.w700)),
                if ((e.subCategoryName ?? '').isNotEmpty)
                  Text(e.subCategoryName!, style: const TextStyle(fontSize: 12)),
                const SizedBox(height: 2),
                Text(
                  '${e.expenseNo} · ${dateText(e.expenseDate)}${(e.paidTo ?? '').isEmpty ? '' : ' · ${e.paidTo}'}',
                  style: const TextStyle(color: Brand.muted, fontSize: 12),
                ),
                const SizedBox(height: 6),
                e.voided ? const Pill('বাতিল', kind: PillKind.bad) : Pill(e.statusLabel, kind: _statusKind(e.status)),
              ],
            ),
          ),
          Text(
            money(e.amount),
            style: TextStyle(
              fontWeight: FontWeight.w800,
              fontSize: 16,
              decoration: e.voided ? TextDecoration.lineThrough : null,
            ),
          ),
        ],
      ),
    );
  }
}
