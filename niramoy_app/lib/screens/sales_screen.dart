import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../models/models.dart';
import '../widgets/common.dart';
import '../widgets/paged_list.dart';
import 'dashboard_screen.dart' show SaleTile;

class SalesScreen extends StatefulWidget {
  const SalesScreen({super.key});

  @override
  State<SalesScreen> createState() => _SalesScreenState();
}

class _SalesScreenState extends State<SalesScreen> {
  static const _filters = {'': 'সব', 'COMPLETED': 'সম্পন্ন', 'RETURNED': 'ফেরত'};

  Timer? _debounce;
  String _q = '';
  String _filter = '';

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

  Future<PageData<Sale>> _fetch(int page) async {
    final j = await context.read<ApiClient>().get('/sales', query: {'q': _q, 'status': _filter, 'page': '$page'});
    return PageData<Sale>.fromJson(j, Sale.fromJson);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SearchAndFilters(
          hint: 'ইনভয়েস নম্বর বা গ্রাহকের নাম',
          onSearch: _onSearch,
          filters: _filters,
          selected: _filter,
          onFilter: (f) => setState(() => _filter = f),
        ),
        Expanded(
          child: PagedListView<Sale>(
            queryKey: '$_q|$_filter',
            fetch: _fetch,
            emptyText: 'কোনো ইনভয়েস পাওয়া যায়নি।',
            itemBuilder: (context, s) => SaleTile(sale: s),
          ),
        ),
      ],
    );
  }
}
