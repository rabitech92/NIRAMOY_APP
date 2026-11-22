import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/app_config.dart';
import '../core/brand.dart';
import '../state/auth_state.dart';
import '../widgets/common.dart';
import 'server_dialog.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _username = TextEditingController();
  final _password = TextEditingController();
  bool _busy = false;
  bool _hide = true;

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final u = _username.text.trim();
    final p = _password.text;
    if (u.isEmpty || p.isEmpty) {
      showSnack(context, 'ইউজারনেম ও পাসওয়ার্ড দিন।', error: true);
      return;
    }
    setState(() => _busy = true);
    try {
      await context.read<AuthState>().login(u, p);
    } on ApiException catch (e) {
      if (mounted) showSnack(context, e.message, error: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final url = context.read<AppConfig>().baseUrl;
    return Scaffold(
      backgroundColor: Brand.navy,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Card(
                color: Colors.white,
                surfaceTintColor: Colors.transparent,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Icon(Icons.local_pharmacy, size: 48, color: Brand.green),
                      const SizedBox(height: 8),
                      const Text('নিরাময়',
                          textAlign: TextAlign.center,
                          style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800, color: Brand.navy)),
                      const Text('ফার্মেসি ব্যবস্থাপনা',
                          textAlign: TextAlign.center, style: TextStyle(color: Brand.muted)),
                      const SizedBox(height: 22),
                      TextField(
                        controller: _username,
                        autocorrect: false,
                        textInputAction: TextInputAction.next,
                        decoration: const InputDecoration(
                            labelText: 'ইউজারনেম', prefixIcon: Icon(Icons.person_outline), border: OutlineInputBorder()),
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _password,
                        obscureText: _hide,
                        onSubmitted: (_) => _submit(),
                        decoration: InputDecoration(
                          labelText: 'পাসওয়ার্ড',
                          prefixIcon: const Icon(Icons.lock_outline),
                          border: const OutlineInputBorder(),
                          suffixIcon: IconButton(
                            icon: Icon(_hide ? Icons.visibility_off_outlined : Icons.visibility_outlined),
                            onPressed: () => setState(() => _hide = !_hide),
                          ),
                        ),
                      ),
                      const SizedBox(height: 18),
                      FilledButton(
                        onPressed: _busy ? null : _submit,
                        style: FilledButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 14)),
                        child: _busy
                            ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                            : const Text('লগইন করুন', style: TextStyle(fontSize: 16)),
                      ),
                      const SizedBox(height: 10),
                      TextButton.icon(
                        onPressed: () async {
                          final changed = await showServerDialog(context);
                          if (changed && mounted) setState(() {});
                        },
                        icon: const Icon(Icons.dns_outlined, size: 18),
                        label: const Text('সার্ভার সেটিংস'),
                      ),
                      Text(url,
                          textAlign: TextAlign.center, style: const TextStyle(color: Brand.muted, fontSize: 12)),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
