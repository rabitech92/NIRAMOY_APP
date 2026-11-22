import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'core/api_client.dart';
import 'core/app_config.dart';
import 'core/brand.dart';
import 'screens/home_shell.dart';
import 'screens/login_screen.dart';
import 'state/auth_state.dart';
import 'state/cart_state.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final config = await AppConfig.load();
  final api = ApiClient(config);
  final auth = AuthState(config, api);

  runApp(
    MultiProvider(
      providers: [
        Provider<AppConfig>.value(value: config),
        Provider<ApiClient>.value(value: api),
        ChangeNotifierProvider<AuthState>.value(value: auth),
        ChangeNotifierProvider<CartState>(create: (_) => CartState()),
      ],
      child: const NiramoyApp(),
    ),
  );

  // সংরক্ষিত সেশন থাকলে স্বয়ংক্রিয় লগইন
  auth.restore();
}

class NiramoyApp extends StatelessWidget {
  const NiramoyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'নিরাময় ফার্মেসি',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Brand.green),
        scaffoldBackgroundColor: Brand.background,
        appBarTheme: const AppBarTheme(
          backgroundColor: Brand.navy,
          foregroundColor: Colors.white,
          elevation: 0,
          centerTitle: false,
        ),
      ),
      home: const _Root(),
    );
  }
}

class _Root extends StatelessWidget {
  const _Root();

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthState>();
    if (auth.restoring) {
      return const Scaffold(
        backgroundColor: Brand.navy,
        body: Center(child: CircularProgressIndicator(color: Colors.white)),
      );
    }
    return auth.loggedIn ? const HomeShell() : const LoginScreen();
  }
}
