import 'package:flutter/material.dart';

import '../core/brand.dart';

void showSnack(BuildContext context, String message, {bool error = false}) {
  final m = ScaffoldMessenger.of(context);
  m.hideCurrentSnackBar();
  m.showSnackBar(SnackBar(
    content: Text(message),
    backgroundColor: error ? Brand.bad : Brand.navy,
    behavior: SnackBarBehavior.floating,
  ));
}

class AppCard extends StatelessWidget {
  const AppCard({
    super.key,
    required this.child,
    this.onTap,
    this.padding = const EdgeInsets.all(14),
    this.margin = const EdgeInsets.only(bottom: 10),
  });

  final Widget child;
  final VoidCallback? onTap;
  final EdgeInsets padding;
  final EdgeInsets margin;

  @override
  Widget build(BuildContext context) {
    final content = Padding(padding: padding, child: child);
    return Card(
      margin: margin,
      color: Colors.white,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: Brand.line),
      ),
      child: onTap == null ? content : InkWell(onTap: onTap, child: content),
    );
  }
}

enum PillKind { good, warn, bad, soft }

class Pill extends StatelessWidget {
  const Pill(this.text, {super.key, this.kind = PillKind.soft});

  final String text;
  final PillKind kind;

  @override
  Widget build(BuildContext context) {
    Color fg;
    Color bg;
    switch (kind) {
      case PillKind.good:
        fg = const Color(0xFF14734C);
        bg = Brand.goodBg;
        break;
      case PillKind.warn:
        fg = Brand.warn;
        bg = Brand.warnBg;
        break;
      case PillKind.bad:
        fg = Brand.bad;
        bg = Brand.badBg;
        break;
      case PillKind.soft:
        fg = const Color(0xFF4A6173);
        bg = const Color(0xFFEAF0F5);
        break;
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 3),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(99)),
      child: Text(text, style: TextStyle(color: fg, fontSize: 12, fontWeight: FontWeight.w600)),
    );
  }
}

class ErrorView extends StatelessWidget {
  const ErrorView({super.key, required this.message, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.cloud_off_outlined, size: 40, color: Brand.muted),
          const SizedBox(height: 10),
          Text(message, textAlign: TextAlign.center, style: const TextStyle(color: Brand.muted)),
          if (onRetry != null) ...[
            const SizedBox(height: 12),
            FilledButton.tonal(onPressed: onRetry, child: const Text('আবার চেষ্টা করুন')),
          ],
        ],
      ),
    );
  }
}

class KeyValueRow extends StatelessWidget {
  const KeyValueRow(this.label, this.value, {super.key, this.bold = false});

  final String label;
  final String value;
  final bool bold;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(flex: 4, child: Text(label, style: const TextStyle(color: Brand.muted))),
          Expanded(
            flex: 6,
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(fontWeight: bold ? FontWeight.w700 : FontWeight.w500),
            ),
          ),
        ],
      ),
    );
  }
}

class SectionTitle extends StatelessWidget {
  const SectionTitle(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(2, 10, 2, 8),
      child: Text(text, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700, color: Brand.navy)),
    );
  }
}

/// সার্চ বক্স + ফিল্টার চিপের সাধারণ হেডার
class SearchAndFilters extends StatelessWidget {
  const SearchAndFilters({
    super.key,
    required this.hint,
    required this.onSearch,
    required this.filters,
    required this.selected,
    required this.onFilter,
    this.controller,
    this.onScan,
  });

  final String hint;
  final ValueChanged<String> onSearch;
  final Map<String, String> filters;
  final String selected;
  final ValueChanged<String> onFilter;
  final TextEditingController? controller;

  /// দিলে সার্চ বক্সে ক্যামেরা-স্ক্যান আইকন দেখায়
  final VoidCallback? onScan;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 4),
      child: Column(
        children: [
          TextField(
            controller: controller,
            onChanged: onSearch,
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(
              hintText: hint,
              prefixIcon: const Icon(Icons.search),
              suffixIcon: onScan == null
                  ? null
                  : IconButton(
                      tooltip: 'বারকোড স্ক্যান করুন',
                      icon: const Icon(Icons.qr_code_scanner),
                      onPressed: onScan,
                    ),
              filled: true,
              fillColor: Colors.white,
              contentPadding: const EdgeInsets.symmetric(vertical: 10),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Brand.line),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: Brand.line),
              ),
            ),
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: filters.entries
                  .map((e) => Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: ChoiceChip(
                          label: Text(e.value),
                          selected: selected == e.key,
                          onSelected: (_) => onFilter(e.key),
                        ),
                      ))
                  .toList(),
            ),
          ),
        ],
      ),
    );
  }
}

/// ড্যাশবোর্ড/খরচের সংখ্যা-কার্ড
class StatTile extends StatelessWidget {
  const StatTile(this.label, this.value, this.icon, this.color, {super.key, this.onTap});

  final String label;
  final String value;
  final IconData icon;
  final Color color;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      margin: EdgeInsets.zero,
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color),
          const SizedBox(height: 10),
          Text(label, style: const TextStyle(color: Brand.muted, fontSize: 12)),
          const SizedBox(height: 2),
          Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }
}

/// একটি লেখা ইনপুট নেওয়ার ডায়ালগ (যেমন বাতিলের কারণ)। বাতিল করলে null।
Future<String?> showTextPrompt(
  BuildContext context, {
  required String title,
  required String hint,
  String confirmLabel = 'নিশ্চিত করুন',
}) {
  return showDialog<String>(
    context: context,
    builder: (_) => _PromptDialog(title: title, hint: hint, confirmLabel: confirmLabel),
  );
}

class _PromptDialog extends StatefulWidget {
  const _PromptDialog({required this.title, required this.hint, required this.confirmLabel});

  final String title;
  final String hint;
  final String confirmLabel;

  @override
  State<_PromptDialog> createState() => _PromptDialogState();
}

class _PromptDialogState extends State<_PromptDialog> {
  final TextEditingController _c = TextEditingController();

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.title),
      content: TextField(
        controller: _c,
        autofocus: true,
        decoration: InputDecoration(hintText: widget.hint, border: const OutlineInputBorder()),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(null), child: const Text('বাতিল')),
        FilledButton(onPressed: () => Navigator.of(context).pop(_c.text.trim()), child: Text(widget.confirmLabel)),
      ],
    );
  }
}
