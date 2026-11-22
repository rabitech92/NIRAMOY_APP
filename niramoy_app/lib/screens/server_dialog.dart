import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:provider/provider.dart';

import '../core/app_config.dart';
import '../core/brand.dart';

/// সার্ভারের ঠিকানা বদলানোর ডায়ালগ। সংরক্ষণ করলে true ফেরত দেয়।
Future<bool> showServerDialog(BuildContext context) async {
  final config = context.read<AppConfig>();
  final saved = await showDialog<bool>(
    context: context,
    builder: (_) => _ServerDialog(config: config),
  );
  return saved ?? false;
}

class _ServerDialog extends StatefulWidget {
  const _ServerDialog({required this.config});

  final AppConfig config;

  @override
  State<_ServerDialog> createState() => _ServerDialogState();
}

class _ServerDialogState extends State<_ServerDialog> {
  late final TextEditingController _controller = TextEditingController(text: widget.config.baseUrl);
  String? _result;
  bool _ok = false;
  bool _testing = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _test() async {
    final url = AppConfig.normalizeUrl(_controller.text);
    setState(() {
      _testing = true;
      _result = null;
    });
    try {
      final res = await http.get(Uri.parse('$url/login')).timeout(const Duration(seconds: 8));
      if (!mounted) return;
      setState(() {
        _ok = res.statusCode < 500;
        _result = _ok ? 'সংযোগ সফল ✔' : 'সার্ভার সাড়া দিয়েছে কিন্তু সমস্যা আছে (${res.statusCode})';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _ok = false;
        _result = 'সংযোগ হয়নি। ঠিকানা, Wi-Fi ও PC-র ফায়ারওয়াল পরীক্ষা করুন।';
      });
    } finally {
      if (mounted) setState(() => _testing = false);
    }
  }

  Future<void> _save() async {
    await widget.config.setBaseUrl(_controller.text);
    if (mounted) Navigator.of(context).pop(true);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('সার্ভার সেটিংস'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'PC-তে সফটওয়্যার যে ঠিকানায় চলে সেটি দিন। যেমন: http://192.168.0.105:8080',
            style: TextStyle(color: Brand.muted, fontSize: 13),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _controller,
            keyboardType: TextInputType.url,
            autocorrect: false,
            decoration: const InputDecoration(border: OutlineInputBorder(), labelText: 'সার্ভারের ঠিকানা'),
          ),
          if (_result != null) ...[
            const SizedBox(height: 10),
            Text(_result!, style: TextStyle(color: _ok ? Brand.green : Brand.bad, fontWeight: FontWeight.w600)),
          ],
        ],
      ),
      actions: [
        TextButton(onPressed: _testing ? null : _test, child: Text(_testing ? 'পরীক্ষা চলছে...' : 'সংযোগ পরীক্ষা')),
        TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('বাতিল')),
        FilledButton(onPressed: _save, child: const Text('সংরক্ষণ')),
      ],
    );
  }
}
