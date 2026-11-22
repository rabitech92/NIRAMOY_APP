import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../state/auth_state.dart';
import '../state/cart_state.dart';
import '../widgets/common.dart';
import 'sale_detail_screen.dart';

/// কার্ট, গ্রাহকের তথ্য, পেমেন্ট ও বিক্রয় সম্পন্ন করার পেজ
class CartScreen extends StatefulWidget {
  const CartScreen({super.key});

  @override
  State<CartScreen> createState() => _CartScreenState();
}

class _CartScreenState extends State<CartScreen> {
  final _discount = TextEditingController();
  final _paid = TextEditingController();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  String _payment = 'CASH';
  bool _busy = false;

  @override
  void dispose() {
    _discount.dispose();
    _paid.dispose();
    _name.dispose();
    _phone.dispose();
    super.dispose();
  }

  double _round2(double v) => (v * 100).round() / 100;

  Future<void> _submit(CartState cart, double total) async {
    final name = _name.text.trim();
    if (name.isEmpty) {
      showSnack(context, 'গ্রাহকের নাম আবশ্যক।', error: true);
      return;
    }
    final phone = normalizePhone(_phone.text);
    if (phone == null) {
      showSnack(context, 'সঠিক মোবাইল নম্বর দিন (যেমন: 01712345678)।', error: true);
      return;
    }

    final discountText = _discount.text.trim();
    final paidText = _paid.text.trim();
    final credit = _payment == 'CREDIT';
    double? paidAmount;
    if (!credit && paidText.isNotEmpty) {
      final p = double.tryParse(paidText);
      if (p == null || p < 0) {
        showSnack(context, 'পরিশোধের অঙ্ক সঠিক নয়।', error: true);
        return;
      }
      paidAmount = p > total ? total : p;
    }

    final api = context.read<ApiClient>();
    setState(() => _busy = true);
    try {
      final j = await api.post('/pos/checkout', body: {
        'items': cart.lines.map((l) => {'medicineId': l.med.id, 'strips': l.strips, 'pieces': l.pieces}).toList(),
        'discount': discountText.isEmpty ? 0 : (double.tryParse(discountText) ?? 0),
        'paymentMethod': _payment,
        'paidAmount': paidAmount,
        'customerName': name,
        'customerPhone': phone,
      });
      final sale = Sale.fromJson(j as Map<String, dynamic>);
      cart.clear();
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(builder: (_) => SaleDetailScreen(saleId: sale.id, initial: sale)),
      );
    } on ApiException catch (e) {
      if (mounted) showSnack(context, e.message, error: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartState>();
    final vat = context.read<AuthState>().vatPercent;

    final subtotal = cart.subtotal;
    var discount = double.tryParse(_discount.text.trim()) ?? 0;
    if (discount < 0) discount = 0;
    if (discount > subtotal) discount = subtotal;
    final net = subtotal - discount;
    final vatAmount = net * vat / 100;
    final total = _round2(net + vatAmount);

    final credit = _payment == 'CREDIT';
    var paid = total;
    if (credit) {
      paid = 0;
    } else if (_paid.text.trim().isNotEmpty) {
      paid = (double.tryParse(_paid.text.trim()) ?? total).clamp(0.0, total).toDouble();
    }
    final due = _round2(total - paid);

    return Scaffold(
      appBar: AppBar(title: const Text('কার্ট ও বিল')),
      body: cart.isEmpty
          ? const Center(child: Text('কার্ট খালি।', style: TextStyle(color: Brand.muted)))
          : ListView(
              padding: const EdgeInsets.all(12),
              children: [
                ...cart.lines.map((l) => _LineTile(key: ValueKey(l.med.id), line: l)),
                const SectionTitle('গ্রাহকের তথ্য *'),
                AppCard(
                  child: Column(
                    children: [
                      TextField(
                        controller: _name,
                        textCapitalization: TextCapitalization.words,
                        decoration: const InputDecoration(labelText: 'গ্রাহকের নাম', border: OutlineInputBorder()),
                      ),
                      const SizedBox(height: 10),
                      TextField(
                        controller: _phone,
                        keyboardType: TextInputType.phone,
                        decoration: const InputDecoration(
                            labelText: 'মোবাইল নম্বর', hintText: '01XXXXXXXXX', border: OutlineInputBorder()),
                      ),
                    ],
                  ),
                ),
                const SectionTitle('পেমেন্ট'),
                AppCard(
                  child: Column(
                    children: [
                      DropdownButtonFormField<String>(
                        value: _payment,
                        decoration: const InputDecoration(labelText: 'পেমেন্ট পদ্ধতি', border: OutlineInputBorder()),
                        items: payMethods
                            .map((p) => DropdownMenuItem<String>(value: p.code, child: Text(p.label)))
                            .toList(),
                        onChanged: (v) => setState(() {
                          _payment = v ?? 'CASH';
                          _paid.clear();
                        }),
                      ),
                      const SizedBox(height: 10),
                      Row(
                        children: [
                          Expanded(
                            child: TextField(
                              controller: _discount,
                              keyboardType: const TextInputType.numberWithOptions(decimal: true),
                              onChanged: (_) => setState(() {}),
                              decoration: const InputDecoration(labelText: 'ছাড় (৳)', border: OutlineInputBorder()),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: TextField(
                              controller: _paid,
                              enabled: !credit,
                              keyboardType: const TextInputType.numberWithOptions(decimal: true),
                              onChanged: (_) => setState(() {}),
                              decoration: InputDecoration(
                                labelText: 'পরিশোধ (৳)',
                                hintText: credit ? '০' : 'সম্পূর্ণ',
                                border: const OutlineInputBorder(),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      const Align(
                        alignment: Alignment.centerLeft,
                        child: Text('পরিশোধ কম দিলে বাকিটা গ্রাহকের বকেয়া হবে।',
                            style: TextStyle(color: Brand.muted, fontSize: 12)),
                      ),
                    ],
                  ),
                ),
                const SectionTitle('হিসাব'),
                AppCard(
                  child: Column(
                    children: [
                      KeyValueRow('উপমোট', money(subtotal)),
                      KeyValueRow('ছাড়', '−${money(discount)}'),
                      KeyValueRow('VAT (${vat.toStringAsFixed(vat == vat.roundToDouble() ? 0 : 2)}%)', money(vatAmount)),
                      const Divider(),
                      KeyValueRow('সর্বমোট', money(total), bold: true),
                      if (due > 0.004) KeyValueRow('পরিশোধ', money(paid)),
                      if (due > 0.004) KeyValueRow('বকেয়া', money(due), bold: true),
                    ],
                  ),
                ),
                const SizedBox(height: 6),
                FilledButton.icon(
                  onPressed: _busy ? null : () => _submit(cart, total),
                  style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
                  icon: _busy
                      ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.check_circle_outline),
                  label: Text('বিক্রয় সম্পন্ন · ${money(total)}', style: const TextStyle(fontSize: 16)),
                ),
                const SizedBox(height: 20),
              ],
            ),
    );
  }
}

class _LineTile extends StatefulWidget {
  const _LineTile({super.key, required this.line});

  final CartLine line;

  @override
  State<_LineTile> createState() => _LineTileState();
}

class _LineTileState extends State<_LineTile> {
  late final TextEditingController _strips = TextEditingController(text: '${widget.line.strips}');
  late final TextEditingController _pieces = TextEditingController(text: '${widget.line.pieces}');

  @override
  void dispose() {
    _strips.dispose();
    _pieces.dispose();
    super.dispose();
  }

  void _commit() {
    final l = widget.line;
    final s = int.tryParse(_strips.text.trim()) ?? 0;
    final p = int.tryParse(_pieces.text.trim()) ?? 0;
    final ok = context.read<CartState>().setQty(l, s, p);
    if (!ok) {
      showSnack(context, 'পর্যাপ্ত স্টক নেই। স্টক: ${l.med.stockLabel}', error: true);
      _strips.text = '${l.strips}';
      _pieces.text = '${l.pieces}';
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = widget.line;
    final hasStrip = l.med.stripSize > 1;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: Text(l.med.name, style: const TextStyle(fontWeight: FontWeight.w700))),
              IconButton(
                visualDensity: VisualDensity.compact,
                icon: const Icon(Icons.delete_outline, color: Brand.bad),
                onPressed: () => context.read<CartState>().remove(l),
              ),
            ],
          ),
          Text(
            hasStrip
                ? 'পিস ${money(l.med.price)} · পাতা ${money(l.med.stripPrice)} (১ পাতা = ${l.med.stripSize} পিস)'
                : 'পিস ${money(l.med.price)}',
            style: const TextStyle(color: Brand.muted, fontSize: 12),
          ),
          if ((l.med.boxNo ?? '').isNotEmpty)
            Text('বক্স ${l.med.boxNo} · স্টক ${l.med.stockLabel}', style: const TextStyle(color: Brand.warn, fontSize: 12)),
          const SizedBox(height: 8),
          Row(
            children: [
              if (hasStrip) ...[
                Expanded(child: _numField(_strips, 'পাতা')),
                const SizedBox(width: 10),
              ],
              Expanded(child: _numField(_pieces, 'পিস')),
              const SizedBox(width: 12),
              Text(money(l.total), style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _numField(TextEditingController c, String label) {
    return TextField(
      controller: c,
      keyboardType: TextInputType.number,
      onChanged: (_) => _commit(),
      decoration: InputDecoration(labelText: label, isDense: true, border: const OutlineInputBorder()),
    );
  }
}
