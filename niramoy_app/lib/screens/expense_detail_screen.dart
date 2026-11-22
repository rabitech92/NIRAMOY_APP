import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/expense_models.dart';
import '../state/auth_state.dart';
import '../widgets/common.dart';

class ExpenseDetailScreen extends StatefulWidget {
  const ExpenseDetailScreen({super.key, required this.expenseId, this.initial});

  final int expenseId;
  final Expense? initial;

  @override
  State<ExpenseDetailScreen> createState() => _ExpenseDetailScreenState();
}

class _ExpenseDetailScreenState extends State<ExpenseDetailScreen> {
  Expense? _exp;
  String? _error;

  @override
  void initState() {
    super.initState();
    final i = widget.initial;
    if (i != null) {
      _exp = i;
    } else {
      _load();
    }
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final j = await context.read<ApiClient>().get('/expenses/${widget.expenseId}');
      if (!mounted) return;
      setState(() => _exp = Expense.fromJson(j as Map<String, dynamic>));
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _pay(Expense e) async {
    final amount = TextEditingController(text: e.dueAmount.toStringAsFixed(2));
    final ref = TextEditingController();
    String method = 'CASH';
    final api = context.read<ApiClient>();

    final ok = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheet) => Padding(
          padding: EdgeInsets.fromLTRB(16, 16, 16, MediaQuery.of(ctx).viewInsets.bottom + 16),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('বাকি পরিশোধ — ${e.expenseNo}', style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
              Text('বকেয়া: ${money(e.dueAmount)}', style: const TextStyle(color: Brand.muted)),
              const SizedBox(height: 12),
              TextField(
                controller: amount,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: 'পরিমাণ (৳)', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 10),
              DropdownButtonFormField<String>(
                value: method,
                decoration: const InputDecoration(labelText: 'পদ্ধতি', border: OutlineInputBorder()),
                items: expensePayMethods
                    .map((p) => DropdownMenuItem<String>(value: p.code, child: Text(p.label)))
                    .toList(),
                onChanged: (v) => setSheet(() => method = v ?? 'CASH'),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: ref,
                decoration: const InputDecoration(labelText: 'রেফারেন্স / ট্রানজেকশন আইডি', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 14),
              FilledButton(onPressed: () => Navigator.of(ctx).pop(true), child: const Text('পেমেন্ট নথিভুক্ত করুন')),
            ],
          ),
        ),
      ),
    );

    // বটম শিট বন্ধ হওয়ার অ্যানিমেশনের সময় controller dispose করলে ত্রুটি হয়, তাই এখানে করা হয়নি
    final value = double.tryParse(amount.text.trim());
    final reference = ref.text.trim();
    if (ok != true || !mounted) return;
    if (value == null || value <= 0) {
      showSnack(context, 'সঠিক পরিমাণ দিন।', error: true);
      return;
    }
    try {
      final j = await api.post('/expenses/${e.id}/pay', body: {
        'amount': value,
        'method': method,
        'referenceNo': reference.isEmpty ? null : reference,
      });
      if (!mounted) return;
      setState(() => _exp = Expense.fromJson(j as Map<String, dynamic>));
      showSnack(context, 'পেমেন্ট নথিভুক্ত হয়েছে।');
    } on ApiException catch (ex) {
      if (mounted) showSnack(context, ex.message, error: true);
    }
  }

  Future<void> _void(Expense e) async {
    final reason = await showTextPrompt(
      context,
      title: 'খরচ বাতিল করুন',
      hint: 'বাতিলের কারণ (আবশ্যক)',
      confirmLabel: 'বাতিল করুন',
    );
    if (reason == null || !mounted) return;
    if (reason.isEmpty) {
      showSnack(context, 'বাতিলের কারণ লিখুন।', error: true);
      return;
    }
    final api = context.read<ApiClient>();
    try {
      final j = await api.post('/expenses/${e.id}/void', body: {'reason': reason});
      if (!mounted) return;
      setState(() => _exp = Expense.fromJson(j as Map<String, dynamic>));
      showSnack(context, 'খরচ বাতিল করা হয়েছে।');
    } on ApiException catch (ex) {
      if (mounted) showSnack(context, ex.message, error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final e = _exp;
    final canVoid = context.watch<AuthState>().user?.canVoidExpense ?? false;
    return Scaffold(
      appBar: AppBar(title: Text(e?.expenseNo ?? 'খরচ')),
      body: e == null
          ? Center(child: _error == null ? const CircularProgressIndicator() : ErrorView(message: _error!, onRetry: _load))
          : ListView(
              padding: const EdgeInsets.all(12),
              children: [
                if (e.voided)
                  Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(color: Brand.badBg, borderRadius: BorderRadius.circular(12)),
                    child: Text(
                      'এই খরচ বাতিল (হিসাব থেকে বাদ)। কারণ: ${e.voidReason ?? ''}${(e.voidedBy ?? '').isEmpty ? '' : ' — ${e.voidedBy}'}',
                      style: const TextStyle(color: Brand.bad, fontWeight: FontWeight.w600),
                    ),
                  ),
                AppCard(
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Expanded(child: Text(e.categoryName, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800))),
                          Pill(e.statusLabel,
                              kind: e.status == 'PAID' ? PillKind.good : (e.status == 'PARTIAL' ? PillKind.warn : PillKind.bad)),
                        ],
                      ),
                      const Divider(),
                      KeyValueRow('তারিখ', dateText(e.expenseDate)),
                      if ((e.subCategoryName ?? '').isNotEmpty) KeyValueRow('উপ-ক্যাটাগরি', e.subCategoryName!),
                      KeyValueRow('কাকে দেওয়া হয়েছে', e.paidTo ?? '—'),
                      KeyValueRow('রেফারেন্স', e.referenceNo ?? '—'),
                      KeyValueRow('শাখা', e.branchName),
                      KeyValueRow('এন্ট্রি করেছেন', '${e.createdBy ?? '—'}, ${dateTimeText(e.createdAt)}'),
                      if ((e.description ?? '').isNotEmpty) KeyValueRow('বিবরণ', e.description!),
                    ],
                  ),
                ),
                AppCard(
                  child: Column(
                    children: [
                      KeyValueRow('মোট খরচ', money(e.amount), bold: true),
                      KeyValueRow('পরিশোধিত', money(e.paidAmount)),
                      KeyValueRow('বকেয়া', money(e.dueAmount), bold: true),
                    ],
                  ),
                ),
                const SectionTitle('পেমেন্টের ইতিহাস'),
                if (e.payments.isEmpty)
                  const AppCard(child: Text('এখনো কোনো পেমেন্ট হয়নি।', style: TextStyle(color: Brand.muted)))
                else
                  ...e.payments.map((p) => AppCard(
                        child: Row(
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text('${p.methodLabel} · ${dateText(p.paidOn)}', style: const TextStyle(fontWeight: FontWeight.w600)),
                                  if ((p.referenceNo ?? '').isNotEmpty)
                                    Text('রেফারেন্স: ${p.referenceNo}', style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                  if ((p.paidBy ?? '').isNotEmpty)
                                    Text(p.paidBy!, style: const TextStyle(color: Brand.muted, fontSize: 12)),
                                ],
                              ),
                            ),
                            Text(money(p.amount), style: const TextStyle(fontWeight: FontWeight.w800)),
                          ],
                        ),
                      )),
                const SizedBox(height: 6),
                if (!e.voided && e.hasDue)
                  FilledButton.icon(
                    onPressed: () => _pay(e),
                    style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
                    icon: const Icon(Icons.payments_outlined),
                    label: const Text('বাকি পরিশোধ করুন'),
                  ),
                if (!e.voided && canVoid) ...[
                  const SizedBox(height: 10),
                  OutlinedButton.icon(
                    onPressed: () => _void(e),
                    style: OutlinedButton.styleFrom(foregroundColor: Brand.bad),
                    icon: const Icon(Icons.block),
                    label: const Text('খরচ বাতিল করুন'),
                  ),
                ],
                const SizedBox(height: 20),
              ],
            ),
    );
  }
}
