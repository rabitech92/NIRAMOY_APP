import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../state/auth_state.dart';
import '../state/cart_state.dart';
import '../widgets/common.dart';
import 'barcode_scanner_screen.dart';
import 'cart_screen.dart';

/// বিক্রয়ের জন্য ওষুধ খুঁজে কার্টে যোগ করার পেজ
class PosScreen extends StatefulWidget {
  const PosScreen({super.key});

  @override
  State<PosScreen> createState() => _PosScreenState();
}

class _PosScreenState extends State<PosScreen> {
  final TextEditingController _searchCtl = TextEditingController();
  Timer? _debounce;
  String _q = '';
  Future<List<Medicine>>? _future;

  @override
  void dispose() {
    _debounce?.cancel();
    _searchCtl.dispose();
    super.dispose();
  }

  Future<List<Medicine>> _fetch() async {
    final j = await context.read<ApiClient>().get('/pos/search', query: {'q': _q});
    return ((j as List?) ?? const []).map((e) => Medicine.fromJson(e as Map<String, dynamic>)).toList();
  }

  void _onSearch(String v) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), () {
      if (!mounted) return;
      setState(() {
        _q = v.trim();
        _future = _fetch();
      });
    });
  }

  /// ক্যামেরায় বারকোড স্ক্যান: একটি ওষুধ মিললে (বা বারকোড হুবহু মিললে) সরাসরি ১ পিস কার্টে যোগ হয়
  Future<void> _scan() async {
    final code = await scanBarcode(context);
    if (code == null || !mounted) return;
    _searchCtl.text = code;
    setState(() {
      _q = code;
      _future = _fetch();
    });
    try {
      final list = await _future!;
      if (!mounted) return;
      final exact = list.where((m) => m.barcode == code).toList();
      final pick = exact.isNotEmpty ? exact.first : (list.length == 1 ? list.first : null);
      if (pick != null) {
        _add(pick, strip: false);
      } else if (list.isEmpty) {
        showSnack(context, 'এই বারকোডের কোনো ওষুধ স্টকে পাওয়া যায়নি।', error: true);
      }
    } on ApiException catch (e) {
      if (mounted) showSnack(context, e.message, error: true);
    }
  }

  void _add(Medicine m, {required bool strip}) {
    final ok = context.read<CartState>().add(m, strip: strip);
    if (!ok) {
      showSnack(context, 'পর্যাপ্ত স্টক নেই। স্টক: ${m.stockLabel}', error: true);
    } else if (_q.isNotEmpty && _q == m.barcode) {
      showSnack(context, '${m.name} কার্টে যোগ হয়েছে।');
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthState>();
    if (auth.branchId == null) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text(
            'বিক্রয়ের জন্য ওপরের শাখা আইকন (🏬) থেকে একটি নির্দিষ্ট শাখা নির্বাচন করুন।',
            textAlign: TextAlign.center,
            style: TextStyle(color: Brand.muted, fontSize: 15),
          ),
        ),
      );
    }
    _future ??= _fetch();

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(12, 10, 12, 6),
          child: TextField(
            controller: _searchCtl,
            onChanged: _onSearch,
            decoration: InputDecoration(
              hintText: 'ওষুধের নাম, জেনেরিক, বারকোড বা বক্স',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: IconButton(
                tooltip: 'বারকোড স্ক্যান করুন',
                icon: const Icon(Icons.qr_code_scanner),
                onPressed: _scan,
              ),
              filled: true,
              fillColor: Colors.white,
              contentPadding: const EdgeInsets.symmetric(vertical: 10),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: Brand.line)),
              enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: Brand.line)),
            ),
          ),
        ),
        Expanded(
          child: FutureBuilder<List<Medicine>>(
            future: _future,
            builder: (context, snap) {
              if (snap.connectionState != ConnectionState.done) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snap.hasError) {
                final msg = snap.error is ApiException ? (snap.error as ApiException).message : 'তথ্য আনা যায়নি।';
                return Center(child: ErrorView(message: msg, onRetry: () => setState(() => _future = _fetch())));
              }
              final list = snap.data!;
              if (list.isEmpty) {
                return const Center(child: Text('কোনো ওষুধ পাওয়া যায়নি।', style: TextStyle(color: Brand.muted)));
              }
              return ListView.builder(
                padding: const EdgeInsets.fromLTRB(12, 4, 12, 12),
                itemCount: list.length,
                itemBuilder: (context, i) => _ResultTile(medicine: list[i], onAdd: _add),
              );
            },
          ),
        ),
        const _CartBar(),
      ],
    );
  }
}

class _ResultTile extends StatelessWidget {
  const _ResultTile({required this.medicine, required this.onAdd});

  final Medicine medicine;
  final void Function(Medicine m, {required bool strip}) onAdd;

  @override
  Widget build(BuildContext context) {
    final m = medicine;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(m.name, style: const TextStyle(fontWeight: FontWeight.w700))),
              if (m.bestSeller)
                const Padding(padding: EdgeInsets.only(right: 6), child: Text('⭐', style: TextStyle(fontSize: 16))),
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
          const SizedBox(height: 4),
          Text('${m.batchNo} · মেয়াদ ${dateText(m.expiryDate)} · স্টক ${m.stockLabel}', style: const TextStyle(fontSize: 12)),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => onAdd(m, strip: false),
                  child: Text('+ পিস ${money(m.price)}'),
                ),
              ),
              if (m.stripSize > 1) ...[
                const SizedBox(width: 8),
                Expanded(
                  child: FilledButton(
                    onPressed: () => onAdd(m, strip: true),
                    child: Text('+ পাতা ${money(m.stripPrice)}'),
                  ),
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}

class _CartBar extends StatelessWidget {
  const _CartBar();

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartState>();
    if (cart.isEmpty) return const SizedBox.shrink();
    return Material(
      color: Brand.navy,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          child: Row(
            children: [
              const Icon(Icons.shopping_cart_outlined, color: Colors.white),
              const SizedBox(width: 10),
              Expanded(
                child: Text('কার্টে ${cart.count}টি ওষুধ · ${money(cart.subtotal)}',
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
              ),
              FilledButton(
                onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const CartScreen())),
                child: const Text('কার্ট দেখুন'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
