import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/brand.dart';
import '../state/auth_state.dart';
import 'dashboard_screen.dart';
import 'inventory_screen.dart';
import 'more_screen.dart';
import 'pos_screen.dart';
import 'sales_screen.dart';

class _Dest {
  _Dest(this.title, this.icon, this.selectedIcon, this.builder);

  final String title;
  final IconData icon;
  final IconData selectedIcon;
  final Widget Function() builder;
}

/// নিচের নেভিগেশন বার; ব্যবহারকারীর ভূমিকা অনুযায়ী ট্যাব দেখায়
class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthState>();
    final user = auth.user!;
    final sub = auth.subscription;

    final dests = <_Dest>[
      _Dest('ড্যাশবোর্ড', Icons.dashboard_outlined, Icons.dashboard, () => const DashboardScreen()),
      if (user.canInventory)
        _Dest('ওষুধ ও স্টক', Icons.medication_outlined, Icons.medication, () => const InventoryScreen()),
      if (user.canSell) _Dest('বিক্রয় (POS)', Icons.point_of_sale, Icons.point_of_sale, () => const PosScreen()),
      if (user.canSell)
        _Dest('ইনভয়েস', Icons.receipt_long_outlined, Icons.receipt_long, () => const SalesScreen()),
      _Dest('আরও', Icons.menu, Icons.menu_open, () => const MoreScreen()),
    ];
    final index = _index >= dests.length ? 0 : _index;
    final current = dests[index];

    return Scaffold(
      appBar: AppBar(
        title: Text(current.title),
        actions: [
          if (user.isGlobal)
            PopupMenuButton<int>(
              tooltip: 'শাখা বদলান',
              icon: const Icon(Icons.store_outlined),
              onSelected: (v) => auth.selectBranch(v == 0 ? null : v),
              itemBuilder: (_) => [
                const PopupMenuItem<int>(value: 0, child: Text('সব শাখা')),
                ...auth.branches.map((b) => PopupMenuItem<int>(value: b.id, child: Text(b.name))),
              ],
            ),
          Padding(
            padding: const EdgeInsets.only(right: 14),
            child: Center(
              child: Text(auth.branchLabel, style: const TextStyle(fontSize: 13, color: Colors.white70)),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          if (sub != null && sub.warning)
            Container(
              width: double.infinity,
              color: Brand.warnBg,
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              child: Text(sub.message, style: const TextStyle(color: Brand.warn, fontWeight: FontWeight.w600)),
            ),
          Expanded(
            // শাখা বা ট্যাব বদলালে পেজ নতুন করে লোড হয়
            child: KeyedSubtree(key: ValueKey('${auth.branchId}-$index'), child: current.builder()),
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: dests
            .map((d) => NavigationDestination(icon: Icon(d.icon), selectedIcon: Icon(d.selectedIcon), label: d.title))
            .toList(),
      ),
    );
  }
}
