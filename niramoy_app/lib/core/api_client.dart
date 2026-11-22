import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'app_config.dart';

class ApiException implements Exception {
  ApiException(this.message, {this.statusCode, this.code});

  final String message;
  final int? statusCode;
  final String? code;

  bool get isUnauthorized => statusCode == 401;
  bool get isSubscriptionExpired => code == 'SUBSCRIPTION_EXPIRED';

  @override
  String toString() => message;
}

/// Spring Boot REST API (/api/v1) এর HTTP ক্লায়েন্ট। JWT ও শাখার হেডার নিজে যোগ করে।
class ApiClient {
  ApiClient(this.config, {http.Client? client}) : _client = client ?? http.Client();

  final AppConfig config;
  final http.Client _client;

  /// সার্ভার 401 দিলে (টোকেনের মেয়াদ শেষ) কল হয়
  void Function()? onUnauthorized;

  Uri _uri(String path, [Map<String, String?>? query]) {
    final base = Uri.parse(config.baseUrl);
    final params = <String, String>{};
    query?.forEach((k, v) {
      if (v != null && v.isNotEmpty) params[k] = v;
    });
    return base.replace(
      path: '${base.path}/api/v1$path',
      queryParameters: params.isEmpty ? null : params,
    );
  }

  Map<String, String> _headers({bool auth = true}) {
    final h = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'application/json; charset=utf-8',
    };
    if (auth) {
      final t = config.token;
      if (t != null) h['Authorization'] = 'Bearer $t';
      final b = config.branchId;
      if (b != null) h['X-Branch-Id'] = '$b';
    }
    return h;
  }

  Future<dynamic> get(String path, {Map<String, String?>? query}) =>
      _send(() => _client.get(_uri(path, query), headers: _headers()));

  Future<dynamic> post(String path, {Object? body, bool auth = true}) => _send(() => _client.post(
        _uri(path),
        headers: _headers(auth: auth),
        body: jsonEncode(body ?? <String, dynamic>{}),
      ));

  Future<dynamic> _send(Future<http.Response> Function() call) async {
    http.Response res;
    try {
      res = await call().timeout(const Duration(seconds: 25));
    } on TimeoutException {
      throw ApiException('সার্ভার সাড়া দিচ্ছে না। সার্ভারের ঠিকানা ও ইন্টারনেট পরীক্ষা করুন।');
    } catch (_) {
      throw ApiException('সার্ভারে সংযোগ করা যায়নি। একই Wi-Fi-তে আছেন কিনা এবং সার্ভারের ঠিকানা সঠিক কিনা দেখুন।');
    }

    final text = utf8.decode(res.bodyBytes);
    dynamic data;
    if (text.isNotEmpty) {
      try {
        data = jsonDecode(text);
      } catch (_) {
        data = null;
      }
    }

    if (res.statusCode >= 200 && res.statusCode < 300) return data;

    var message = 'অনুরোধ সম্পন্ন করা যায়নি (${res.statusCode})।';
    String? code;
    if (data is Map<String, dynamic>) {
      final m = data['message'];
      if (m is String && m.isNotEmpty) message = m;
      final c = data['code'];
      if (c is String) code = c;
    }
    if (res.statusCode == 401) onUnauthorized?.call();
    throw ApiException(message, statusCode: res.statusCode, code: code);
  }
}
