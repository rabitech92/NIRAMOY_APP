import 'package:flutter/foundation.dart';

import '../core/api_client.dart';
import '../core/app_config.dart';
import '../models/models.dart';

/// লগইন, বর্তমান ব্যবহারকারী, শাখা নির্বাচন ও সাবস্ক্রিপশনের অবস্থা
class AuthState extends ChangeNotifier {
  AuthState(this.config, this.api) {
    api.onUnauthorized = _handleUnauthorized;
  }

  final AppConfig config;
  final ApiClient api;

  AppUser? user;
  SubscriptionInfo? subscription;
  List<Branch> branches = [];
  double vatPercent = 0;

  /// অ্যাপ চালুর সময় সংরক্ষিত সেশন যাচাই চলছে
  bool restoring = true;

  bool get loggedIn => user != null;

  /// null = সব শাখা (শুধু SUPER_ADMIN / OWNER)
  int? get branchId => (user?.isGlobal ?? false) ? config.branchId : user?.branchId;

  String get branchLabel {
    final u = user;
    if (u == null) return '';
    if (!u.isGlobal) return u.branchName ?? '';
    final id = config.branchId;
    if (id == null) return 'সব শাখা';
    for (final b in branches) {
      if (b.id == id) return b.name;
    }
    return 'সব শাখা';
  }

  Future<void> restore() async {
    if (config.token != null) {
      try {
        await refreshMe();
      } on ApiException catch (e) {
        if (e.isUnauthorized) await _clear();
      }
    }
    restoring = false;
    notifyListeners();
  }

  Future<void> login(String username, String password) async {
    final res = await api.post('/auth/login', auth: false, body: {'username': username, 'password': password});
    await config.setToken((res as Map<String, dynamic>)['token'] as String);
    try {
      await refreshMe();
    } catch (_) {
      await _clear();
      rethrow;
    }
  }

  Future<void> refreshMe() async {
    final j = await api.get('/me') as Map<String, dynamic>;
    user = AppUser.fromJson(j['user'] as Map<String, dynamic>);
    subscription = SubscriptionInfo.fromJson(j['subscription'] as Map<String, dynamic>);
    branches = ((j['branches'] as List?) ?? const [])
        .map((e) => Branch.fromJson(e as Map<String, dynamic>))
        .toList();
    vatPercent = (j['vatPercent'] as num?)?.toDouble() ?? 0;

    final u = user!;
    final saved = config.branchId;
    if (!u.isGlobal || (saved != null && !branches.any((b) => b.id == saved))) {
      await config.setBranchId(null);
    }
    notifyListeners();
  }

  Future<void> selectBranch(int? id) async {
    await config.setBranchId(id);
    notifyListeners();
  }

  Future<void> logout() async {
    await _clear();
    notifyListeners();
  }

  void _handleUnauthorized() {
    if (config.token != null || user != null) {
      _clear().then((_) => notifyListeners());
    }
  }

  Future<void> _clear() async {
    await config.setToken(null);
    await config.setBranchId(null);
    user = null;
    subscription = null;
    branches = [];
  }
}
