import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../core/brand.dart';
import '../models/models.dart';
import 'common.dart';

/// পেজিনেশন (শেষে স্ক্রল করলে পরের পেজ), টেনে রিফ্রেশ ও ত্রুটির অবস্থাসহ তালিকা।
/// [queryKey] বদলালেই তালিকা প্রথম থেকে আবার লোড হয়।
class PagedListView<T> extends StatefulWidget {
  const PagedListView({
    super.key,
    required this.queryKey,
    required this.fetch,
    required this.itemBuilder,
    this.emptyText = 'কোনো তথ্য পাওয়া যায়নি।',
  });

  final String queryKey;
  final Future<PageData<T>> Function(int page) fetch;
  final Widget Function(BuildContext context, T item) itemBuilder;
  final String emptyText;

  @override
  State<PagedListView<T>> createState() => _PagedListViewState<T>();
}

class _PagedListViewState<T> extends State<PagedListView<T>> {
  final List<T> _items = [];
  final ScrollController _scroll = ScrollController();
  int _page = 0;
  bool _loading = true;
  bool _hasMore = true;
  String? _error;
  int _ticket = 0;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_onScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) => _load(reset: true));
  }

  @override
  void didUpdateWidget(covariant PagedListView<T> oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.queryKey != widget.queryKey) _load(reset: true);
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!_scroll.hasClients) return;
    if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 300 && !_loading && _hasMore && _error == null) {
      _load();
    }
  }

  Future<void> _load({bool reset = false}) async {
    final ticket = ++_ticket;
    if (!mounted) return;
    setState(() {
      _loading = true;
      _error = null;
      if (reset) {
        _page = 0;
        _hasMore = true;
        _items.clear();
      }
    });
    try {
      final data = await widget.fetch(_page);
      if (!mounted || ticket != _ticket) return;
      setState(() {
        _items.addAll(data.items);
        _hasMore = data.hasMore;
        _page = data.page + 1;
        _loading = false;
      });
    } on ApiException catch (e) {
      if (!mounted || ticket != _ticket) return;
      setState(() {
        _loading = false;
        _error = e.message;
      });
    } catch (_) {
      if (!mounted || ticket != _ticket) return;
      setState(() {
        _loading = false;
        _error = 'তথ্য আনা যায়নি।';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: () => _load(reset: true),
      child: _items.isEmpty ? _emptyState() : _list(),
    );
  }

  Widget _emptyState() {
    Widget child;
    if (_loading) {
      child = const CircularProgressIndicator();
    } else if (_error != null) {
      child = ErrorView(message: _error!, onRetry: () => _load(reset: true));
    } else {
      child = Text(widget.emptyText, style: const TextStyle(color: Brand.muted));
    }
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      children: [SizedBox(height: 260, child: Center(child: child))],
    );
  }

  Widget _list() {
    return ListView.builder(
      controller: _scroll,
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(12, 4, 12, 16),
      itemCount: _items.length + 1,
      itemBuilder: (context, i) {
        if (i < _items.length) return widget.itemBuilder(context, _items[i]);
        if (_loading) {
          return const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator()));
        }
        if (_error != null) {
          return ErrorView(message: _error!, onRetry: _load);
        }
        return const SizedBox(height: 8);
      },
    );
  }
}
