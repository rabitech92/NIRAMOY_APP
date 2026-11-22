import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../widgets/common.dart';
import '../widgets/paged_list.dart';
import 'barcode_scanner_screen.dart';
import 'medicine_detail_screen.dart';

class InventoryScreen extends StatefulWidget {
  const InventoryScreen({super.key});

  @override
  State<InventoryScreen> createState() => _InventoryScreenState();
}

class _InventoryScreenState extends State<InventoryScreen> {
  static const _filters = {
    'ALL': 'সব',
    'LOW': 'কম স্টক',
    'EXPIRING': 'মেয়াদ ঘনিয়ে',
    'EXPIRED': 'মেয়াদোত্তীর্ণ',
    'OK': 'পর্যাপ্ত',
  };

  final TextEditingController _searchCtl = TextEditingController();
  Timer? _debounce;
  String _q = '';
  String _filter = 'ALL';

  @override
  void dispose() {
    _debounce?.cancel();
    _searchCtl.dispose();
    super.dispose();
  }

  Future<void> _scan() async {
    final code = await scanBarcode(context);
    if (code == null || !mounted) return;
    _searchCtl.text = code;
    setState(() => _q = code);
  }

  void _onSearch(String v) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 400), () {
      if (mounted) setState(() => _q = v.trim());
    });
  }

  Future<PageData<Medicine>> _fetch(int page) async {
    final j = await context.read<ApiClient>().get('/medicines', query: {'q': _q, 'filter': _filter, 'page': '$page'});
    return PageData<Medicine>.fromJson(j, Medicine.fromJson);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        SearchAndFilters(
          hint: 'নাম, জেনেরিক, কোম্পানি, বারকোড বা বক্স',
          onSearch: _onSearch,
          filters: _filters,
          selected: _filter,
          onFilter: (f) => setState(() => _filter = f),
          controller: _searchCtl,
          onScan: _scan,
        ),
        Expanded(
          child: PagedListView<Medicine>(
            queryKey: '$_q|$_filter',
            fetch: _fetch,
            emptyText: 'কোনো ওষুধ পাওয়া যায়নি।',
            itemBuilder: (context, m) => MedicineTile(medicine: m),
          ),
        ),
      ],
    );
  }
}

class MedicineTile extends StatelessWidget {
  const MedicineTile({super.key, required this.medicine});

  final Medicine medicine;

  @override
  Widget build(BuildContext context) {
    final m = medicine;
    return AppCard(
      onTap: () => Navigator.of(context).push(
        MaterialPageRoute<void>(builder: (_) => MedicineDetailScreen(medicine: m)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(m.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 15))),
              if ((m.boxNo ?? '').isNotEmpty)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(color: Brand.warnBg, borderRadius: BorderRadius.circular(6)),
                  child: Text('বক্স ${m.boxNo}',
                      style: const TextStyle(color: Brand.warn, fontWeight: FontWeight.w700, fontSize: 12)),
                ),
            ],
          ),
          if (m.subtitle.isNotEmpty) Text(m.subtitle, style: const TextStyle(color: Brand.muted, fontSize: 12)),
          const SizedBox(height: 6),
          Text('ব্যাচ ${m.batchNo} · মেয়াদ ${dateText(m.expiryDate)}', style: const TextStyle(fontSize: 12)),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: Text('স্টক: ${m.stockLabel}', style: const TextStyle(fontWeight: FontWeight.w600)),
              ),
              Text(
                m.stripSize > 1 ? '${money(m.price)}/পিস · ${money(m.stripPrice)}/পাতা' : '${money(m.price)}/পিস',
                style: const TextStyle(fontSize: 12, color: Brand.muted),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Wrap(
            spacing: 6,
            children: [
              if (m.expired) const Pill('মেয়াদোত্তীর্ণ', kind: PillKind.bad),
              if (!m.expired && m.expiringSoon) const Pill('মেয়াদ ঘনিয়ে এসেছে', kind: PillKind.warn),
              if (m.bestSeller) const Pill('⭐ বেশি বিক্রিত', kind: PillKind.warn),
              if (m.lowStock) const Pill('কম স্টক', kind: PillKind.warn),
              if (!m.expired && !m.expiringSoon && !m.lowStock) const Pill('পর্যাপ্ত', kind: PillKind.good),
            ],
          ),
        ],
      ),
    );
  }
}
