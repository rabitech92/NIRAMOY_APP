import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/expense_models.dart';
import '../widgets/common.dart';
import 'expense_detail_screen.dart';

/// নতুন খরচ এন্ট্রি
class ExpenseFormScreen extends StatefulWidget {
  const ExpenseFormScreen({super.key});

  @override
  State<ExpenseFormScreen> createState() => _ExpenseFormScreenState();
}

class _ExpenseFormScreenState extends State<ExpenseFormScreen> {
  ExpenseMeta? _meta;
  String? _metaError;

  DateTime _date = DateTime.now();
  int? _categoryId;
  int? _subId;
  String _status = 'PAID';
  String _method = 'CASH';

  final _amount = TextEditingController();
  final _paidNow = TextEditingController();
  final _paidTo = TextEditingController();
  final _ref = TextEditingController();
  final _desc = TextEditingController();
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _loadMeta();
  }

  @override
  void dispose() {
    _amount.dispose();
    _paidNow.dispose();
    _paidTo.dispose();
    _ref.dispose();
    _desc.dispose();
    super.dispose();
  }

  Future<void> _loadMeta() async {
    setState(() => _metaError = null);
    try {
      final j = await context.read<ApiClient>().get('/expenses/meta');
      if (!mounted) return;
      setState(() => _meta = ExpenseMeta.fromJson(j as Map<String, dynamic>));
    } on ApiException catch (e) {
      if (mounted) setState(() => _metaError = e.message);
    }
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: now.subtract(const Duration(days: 730)),
      lastDate: now,
    );
    if (picked != null && mounted) setState(() => _date = picked);
  }

  String? _nullIfEmpty(String v) => v.trim().isEmpty ? null : v.trim();

  Future<void> _submit() async {
    final amount = double.tryParse(_amount.text.trim());
    if (_categoryId == null) {
      showSnack(context, 'ক্যাটাগরি নির্বাচন করুন।', error: true);
      return;
    }
    if (amount == null || amount <= 0) {
      showSnack(context, 'টাকার পরিমাণ দিন।', error: true);
      return;
    }
    double? paidNow;
    if (_status == 'PARTIAL') {
      paidNow = double.tryParse(_paidNow.text.trim());
      if (paidNow == null || paidNow <= 0 || paidNow >= amount) {
        showSnack(context, 'আংশিক পরিশোধের অঙ্ক ০-এর বেশি ও মোট খরচের কম হতে হবে।', error: true);
        return;
      }
    }

    final api = context.read<ApiClient>();
    setState(() => _busy = true);
    try {
      final j = await api.post('/expenses', body: {
        'expenseDate': isoDate(_date),
        'categoryId': _categoryId,
        'subCategoryId': _subId,
        'amount': amount,
        'status': _status,
        'paidAmount': paidNow,
        'paymentMethod': _status == 'UNPAID' ? null : _method,
        'paidTo': _nullIfEmpty(_paidTo.text),
        'referenceNo': _nullIfEmpty(_ref.text),
        'description': _nullIfEmpty(_desc.text),
      });
      final e = Expense.fromJson(j as Map<String, dynamic>);
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(builder: (_) => ExpenseDetailScreen(expenseId: e.id, initial: e)),
      );
    } on ApiException catch (e) {
      if (mounted) showSnack(context, e.message, error: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final meta = _meta;
    return Scaffold(
      appBar: AppBar(title: const Text('নতুন খরচ')),
      body: meta == null
          ? Center(
              child: _metaError == null
                  ? const CircularProgressIndicator()
                  : ErrorView(message: _metaError!, onRetry: _loadMeta),
            )
          : _form(meta),
    );
  }

  Widget _form(ExpenseMeta meta) {
    final subs = meta.subs.where((s) => s.categoryId == _categoryId).toList();
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        AppCard(
          child: Column(
            children: [
              InkWell(
                onTap: _pickDate,
                child: InputDecorator(
                  decoration: const InputDecoration(labelText: 'খরচের তারিখ', border: OutlineInputBorder()),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [Text(dateText(_date)), const Icon(Icons.calendar_today_outlined, size: 18)],
                  ),
                ),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<int>(
                value: _categoryId,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'ক্যাটাগরি *', border: OutlineInputBorder()),
                items: meta.categories
                    .map((c) => DropdownMenuItem<int>(
                          value: c.id,
                          child: Text(c.label, overflow: TextOverflow.ellipsis),
                        ))
                    .toList(),
                onChanged: (v) => setState(() {
                  _categoryId = v;
                  _subId = null;
                }),
              ),
              if (subs.isNotEmpty) ...[
                const SizedBox(height: 12),
                DropdownButtonFormField<int?>(
                  value: _subId,
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: 'উপ-ক্যাটাগরি', border: OutlineInputBorder()),
                  items: [
                    const DropdownMenuItem<int?>(value: null, child: Text('— নেই —')),
                    ...subs.map((s) => DropdownMenuItem<int?>(value: s.id, child: Text(s.name))),
                  ],
                  onChanged: (v) => setState(() => _subId = v),
                ),
              ],
              const SizedBox(height: 12),
              TextField(
                controller: _amount,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: 'টাকার পরিমাণ (৳) *', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _paidTo,
                decoration: const InputDecoration(labelText: 'কাকে টাকা দেওয়া হয়েছে', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _desc,
                decoration: const InputDecoration(labelText: 'বিবরণ', border: OutlineInputBorder()),
              ),
            ],
          ),
        ),
        const SectionTitle('পেমেন্ট'),
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment<String>(value: 'PAID', label: Text('পরিশোধিত')),
                  ButtonSegment<String>(value: 'PARTIAL', label: Text('আংশিক')),
                  ButtonSegment<String>(value: 'UNPAID', label: Text('বকেয়া')),
                ],
                selected: {_status},
                onSelectionChanged: (s) => setState(() => _status = s.first),
              ),
              if (_status == 'PARTIAL') ...[
                const SizedBox(height: 12),
                TextField(
                  controller: _paidNow,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'এখন কত দিলেন (৳)', border: OutlineInputBorder()),
                ),
              ],
              if (_status != 'UNPAID') ...[
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  value: _method,
                  decoration: const InputDecoration(labelText: 'পেমেন্ট পদ্ধতি', border: OutlineInputBorder()),
                  items: expensePayMethods
                      .map((p) => DropdownMenuItem<String>(value: p.code, child: Text(p.label)))
                      .toList(),
                  onChanged: (v) => setState(() => _method = v ?? 'CASH'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _ref,
                  decoration: const InputDecoration(
                      labelText: 'রেফারেন্স / ট্রানজেকশন আইডি', border: OutlineInputBorder()),
                ),
              ],
              const SizedBox(height: 6),
              const Text(
                'পরিশোধিত = পুরো টাকা দেওয়া হয়েছে · আংশিক = কিছু দেওয়া হয়েছে, বাকি পরে · বকেয়া = এখনো কিছু দেওয়া হয়নি',
                style: TextStyle(color: Brand.muted, fontSize: 12),
              ),
            ],
          ),
        ),
        const SizedBox(height: 10),
        FilledButton.icon(
          onPressed: _busy ? null : _submit,
          style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
          icon: _busy
              ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
              : const Icon(Icons.check_circle_outline),
          label: const Text('খরচ সংরক্ষণ করুন', style: TextStyle(fontSize: 16)),
        ),
        const SizedBox(height: 20),
      ],
    );
  }
}
