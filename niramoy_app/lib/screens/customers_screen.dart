import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/insight_models.dart';
import '../models/models.dart';
import '../widgets/common.dart';
import '../widgets/paged_list.dart';
import 'customer_detail_screen.dart';

/// গ্রাহক বিশ্লেষণ: কে জীবনকালে কত টাকার ওষুধ কিনেছেন + আজ/সপ্তাহ/মাস/বছর
class CustomersScreen extends StatefulWidget {
  const CustomersScreen({super.key});

  @override
  State<CustomersScreen> createState() => _CustomersScreenState();
}

class _CustomersScreenState extends State<CustomersScreen> {
  static const _sorts = {'total': 'মোট ক্রয় বেশি', 'recent': 'সাম্প্রতিক ক্রয়'};

  Timer? _debounce;
  String _q = '';
  String _sort = 'total';
  int _all = 0;
  double _allSum = 0;
  double _allAvg = 0;

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

  Future<PageData<CustomerRow>> _fetch(int page) async {
    final j = await context.read<ApiClient>().get('/customers', query: {'q': _q, 'sort': _sort, 'page': '$page'});
    final r = CustomerListResult.fromJson(j);
    if (page == 0 && mounted) {
      setState(() {
        _all = r.allCustomers;
        _allSum = r.allLifetime;
        _allAvg = r.allAverage;
      });
    }
    return r.page;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('গ্রাহক বিশ্লেষণ')),
      body: Column(
        children: [
          SearchAndFilters(
            hint: 'গ্রাহকের নাম বা মোবাইল',
            onSearch: _onSearch,
            filters: _sorts,
            selected: _sort,
            onFilter: (f) => setState(() => _sort = f),
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
                _cell('মোট গ্রাহক', '$_all', Brand.navy),
                _cell('মোট ক্রয়', money(_allSum), Brand.green),
                _cell('গড় প্রতি গ্রাহক', money(_allAvg), Brand.warn),
              ],
            ),
          ),
          Expanded(
            child: PagedListView<CustomerRow>(
              queryKey: '$_q|$_sort',
              fetch: _fetch,
              emptyText: 'কোনো গ্রাহক পাওয়া যায়নি।\n(গ্রাহক চেনা হয় বিলের মোবাইল নম্বর দিয়ে)',
              itemBuilder: (context, r) => _CustomerTile(row: r),
            ),
          ),
        ],
      ),
    );
  }

  Widget _cell(String label, String value, Color color) {
    return Column(
      children: [
        Text(label, style: const TextStyle(color: Brand.muted, fontSize: 11)),
        const SizedBox(height: 2),
        Text(value, style: TextStyle(fontWeight: FontWeight.w800, color: color, fontSize: 13)),
      ],
    );
  }
}

class _CustomerTile extends StatelessWidget {
  const _CustomerTile({required this.row});

  final CustomerRow row;

  @override
  Widget build(BuildContext context) {
    final r = row;
    return AppCard(
      onTap: () => Navigator.of(context).push(
        MaterialPageRoute<void>(builder: (_) => CustomerDetailScreen(phone: r.phone)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(r.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15)),
                    Text('${r.phone} · ${r.invoices}টি ইনভয়েস', style: const TextStyle(color: Brand.muted, fontSize: 12)),
                  ],
                ),
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(money(r.lifetime), style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                  const Text('জীবনকালে', style: TextStyle(color: Brand.muted, fontSize: 11)),
                ],
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _mini('আজ', r.today),
              _mini('সপ্তাহ', r.week),
              _mini('মাস', r.month),
              _mini('বছর', r.year),
            ],
          ),
          if (r.due > 0.004) ...[
            const SizedBox(height: 6),
            Pill('বকেয়া ${money(r.due)}', kind: PillKind.bad),
          ],
        ],
      ),
    );
  }

  Widget _mini(String label, double v) {
    return Column(
      children: [
        Text(label, style: const TextStyle(color: Brand.muted, fontSize: 11)),
        Text(money(v), style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
      ],
    );
  }
}
