import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/app_config.dart';
import '../core/brand.dart';
import '../core/format.dart';
import '../models/models.dart';
import '../state/auth_state.dart';
import '../widgets/common.dart';
import 'best_sellers_screen.dart';
import 'business_screen.dart';
import 'customers_screen.dart';
import 'dues_screen.dart';
import 'expense_home_screen.dart';
import 'refills_screen.dart';
import 'server_dialog.dart';

class MoreScreen extends StatelessWidget {
  const MoreScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthState>();
    final user = auth.user!;
    final url = context.read<AppConfig>().baseUrl;

    void open(Widget page) => Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => page));

    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        AppCard(
          child: Row(
            children: [
              const CircleAvatar(
                radius: 24,
                backgroundColor: Brand.goodBg,
                child: Icon(Icons.person, color: Brand.green),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(user.fullName, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
                    Text('${user.roleLabel} · ${auth.branchLabel}', style: const TextStyle(color: Brand.muted)),
                    Text('@${user.username}', style: const TextStyle(color: Brand.muted, fontSize: 12)),
                  ],
                ),
              ),
            ],
          ),
        ),
        if (user.canSell)
          _tile(Icons.repeat, 'নিয়মিত ওষুধ (রিফিল)', 'গ্রাহকের মাসিক/পাক্ষিক ওষুধ ও রিমাইন্ডার', () => open(const RefillsScreen())),
        if (user.canCustomers)
          _tile(Icons.account_balance_outlined, 'ক্রয়-বিক্রয় ও স্টক হিসাব', 'স্টকে কত টাকার মাল, মাসে/বছরে ক্রয়-বিক্রয় ও লাভ', () => open(const BusinessScreen())),
        if (user.canCustomers)
          _tile(Icons.groups_outlined, 'গ্রাহক বিশ্লেষণ', 'কে কত টাকার ওষুধ কিনেছেন — দৈনিক, সাপ্তাহিক, বার্ষিক', () => open(const CustomersScreen())),
        if (user.canInventory)
          _tile(Icons.star_outline, 'বেশি বিক্রিত ওষুধ', 'কোন ওষুধ বেশি বিক্রি হয় ও চিহ্নিত করা', () => open(const BestSellersScreen())),
        if (user.canExpenses)
          _tile(Icons.receipt_long_outlined, 'খরচ ব্যবস্থাপনা', 'দোকানের খরচ, বকেয়া খরচ ও রিপোর্ট', () => open(const ExpenseHomeScreen())),
        if (user.canDues)
          _tile(Icons.account_balance_wallet_outlined, 'গ্রাহকের বকেয়া', 'বকেয়ার তালিকা ও আদায়', () => open(const DuesScreen())),
        _tile(Icons.verified_outlined, 'সফটওয়্যার সাবস্ক্রিপশন', _subtitle(auth.subscription), () => open(const SubscriptionScreen())),
        _tile(Icons.dns_outlined, 'সার্ভার সেটিংস', url, () async {
          final changed = await showServerDialog(context);
          if (changed && context.mounted) {
            showSnack(context, 'সার্ভারের ঠিকানা সংরক্ষিত হয়েছে। আবার লগইন করুন।');
            await context.read<AuthState>().logout();
          }
        }),
        _tile(Icons.logout, 'লগআউট', 'এই ডিভাইস থেকে বের হন', () async {
          final ok = await showDialog<bool>(
            context: context,
            builder: (ctx) => AlertDialog(
              title: const Text('লগআউট'),
              content: const Text('আপনি কি লগআউট করবেন?'),
              actions: [
                TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('না')),
                FilledButton(onPressed: () => Navigator.of(ctx).pop(true), child: const Text('হ্যাঁ')),
              ],
            ),
          );
          if (ok == true && context.mounted) await context.read<AuthState>().logout();
        }, danger: true),
        const SizedBox(height: 16),
        const Center(child: Text('নিরাময় ফার্মেসি · মোবাইল অ্যাপ v1.0', style: TextStyle(color: Brand.muted, fontSize: 12))),
      ],
    );
  }

  static String _subtitle(SubscriptionInfo? s) {
    if (s == null) return '';
    if (s.endDate == null) return s.message;
    return '${s.planName} · মেয়াদ ${dateText(s.endDate)}';
  }

  Widget _tile(IconData icon, String title, String subtitle, VoidCallback onTap, {bool danger = false}) {
    return AppCard(
      onTap: onTap,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      child: Row(
        children: [
          Icon(icon, color: danger ? Brand.bad : Brand.navy),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: TextStyle(fontWeight: FontWeight.w700, color: danger ? Brand.bad : null)),
                if (subtitle.isNotEmpty) Text(subtitle, style: const TextStyle(color: Brand.muted, fontSize: 12)),
              ],
            ),
          ),
          const Icon(Icons.chevron_right, color: Brand.muted),
        ],
      ),
    );
  }
}

/// সাবস্ক্রিপশনের অবস্থা (নবায়ন ওয়েব সফটওয়্যার থেকে করতে হয়)
class SubscriptionScreen extends StatefulWidget {
  const SubscriptionScreen({super.key});

  @override
  State<SubscriptionScreen> createState() => _SubscriptionScreenState();
}

class _SubscriptionScreenState extends State<SubscriptionScreen> {
  late Future<SubscriptionInfo> _future;

  @override
  void initState() {
    super.initState();
    _future = _fetch();
  }

  Future<SubscriptionInfo> _fetch() async {
    final j = await context.read<ApiClient>().get('/subscription');
    return SubscriptionInfo.fromJson(j as Map<String, dynamic>);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('সাবস্ক্রিপশন')),
      body: FutureBuilder<SubscriptionInfo>(
        future: _future,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snap.hasError) {
            final msg = snap.error is ApiException ? (snap.error as ApiException).message : 'তথ্য আনা যায়নি।';
            return Center(child: ErrorView(message: msg, onRetry: () => setState(() => _future = _fetch())));
          }
          final s = snap.data!;
          PillKind kind = PillKind.good;
          String label = 'সক্রিয়';
          if (s.level == 'GRACE') {
            kind = PillKind.warn;
            label = 'গ্রেস পিরিয়ড';
          } else if (s.blocked) {
            kind = PillKind.bad;
            label = 'মেয়াদোত্তীর্ণ';
          }
          return ListView(
            padding: const EdgeInsets.all(12),
            children: [
              AppCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Pill(label, kind: kind),
                    const SizedBox(height: 10),
                    KeyValueRow('প্ল্যান', s.planName),
                    KeyValueRow('মেয়াদ শেষ', s.endDate == null ? '—' : dateText(s.endDate)),
                    KeyValueRow('বাকি দিন', s.daysLeft >= 0 ? '${s.daysLeft} দিন' : 'শেষ'),
                    if (s.pending) const KeyValueRow('অনুরোধ', 'নবায়নের অনুরোধ যাচাই অপেক্ষায়'),
                    const SizedBox(height: 8),
                    Text(s.message, style: const TextStyle(color: Brand.muted)),
                  ],
                ),
              ),
              const AppCard(
                child: Text(
                  'নবায়ন বা প্ল্যান বদলাতে ওয়েব সফটওয়্যারে (প্রশাসন → সাবস্ক্রিপশন) লগইন করুন। শুধু মালিক ও সুপার অ্যাডমিন নবায়ন করতে পারেন।',
                  style: TextStyle(color: Brand.muted),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
