import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// সার্ভারের ঠিকানা, নির্বাচিত শাখা (সাধারণ সেটিংস) ও লগইন টোকেন (এনক্রিপ্টেড স্টোরেজ)।
/// টোকেন মেমরিতে রেখে সিঙ্ক্রোনাসভাবে পড়া হয়; সংরক্ষণ Keystore-ভিত্তিক secure storage-এ।
class AppConfig {
  AppConfig._(this._prefs, this._secure, this._token);

  /// অ্যান্ড্রয়েড এমুলেটর থেকে PC-র localhost; আসল ফোনে সেটিংসে PC-র IP দিতে হবে
  static const defaultBaseUrl = 'http://10.0.2.2:8080';

  static const _kBaseUrl = 'base_url';
  static const _kToken = 'token';
  static const _kBranch = 'branch_id';

  final SharedPreferences _prefs;
  final FlutterSecureStorage _secure;
  String? _token;

  static Future<AppConfig> load() async {
    final prefs = await SharedPreferences.getInstance();
    const secure = FlutterSecureStorage(aOptions: AndroidOptions(encryptedSharedPreferences: true));

    String? token;
    try {
      token = await secure.read(key: _kToken);
    } catch (_) {
      token = null;
    }

    // আগের সংস্করণে টোকেন সাধারণ সেটিংসে ছিল — নিরাপদ স্টোরেজে সরিয়ে পুরনোটি মুছে ফেলা হয়
    final legacy = prefs.getString(_kToken);
    if (legacy != null) {
      if (token == null) {
        token = legacy;
        try {
          await secure.write(key: _kToken, value: legacy);
        } catch (_) {}
      }
      await prefs.remove(_kToken);
    }
    return AppConfig._(prefs, secure, token);
  }

  String get baseUrl => _prefs.getString(_kBaseUrl) ?? defaultBaseUrl;

  Future<void> setBaseUrl(String value) async {
    await _prefs.setString(_kBaseUrl, normalizeUrl(value));
  }

  String? get token => _token;

  Future<void> setToken(String? value) async {
    _token = value;
    try {
      if (value == null) {
        await _secure.delete(key: _kToken);
      } else {
        await _secure.write(key: _kToken, value: value);
      }
    } catch (_) {
      // Keystore সমস্যায় টোকেন শুধু এই সেশনের জন্য মেমরিতে থাকবে
    }
  }

  int? get branchId => _prefs.getInt(_kBranch);

  Future<void> setBranchId(int? id) async {
    if (id == null) {
      await _prefs.remove(_kBranch);
    } else {
      await _prefs.setInt(_kBranch, id);
    }
  }

  static String normalizeUrl(String input) {
    var v = input.trim();
    if (v.isEmpty) return defaultBaseUrl;
    if (!v.startsWith('http://') && !v.startsWith('https://')) v = 'http://$v';
    while (v.endsWith('/')) {
      v = v.substring(0, v.length - 1);
    }
    return v;
  }
}
