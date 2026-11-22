import 'models.dart' show PageData, Sale;

int _i(dynamic v) => v == null ? 0 : (v as num).toInt();
double _d(dynamic v) => v == null ? 0 : (v as num).toDouble();
String? _s(dynamic v) => v as String?;
DateTime? _dt(dynamic v) => v == null ? null : DateTime.tryParse(v as String);
List<Map<String, dynamic>> _list(dynamic v) =>
    ((v as List?) ?? const []).map((e) => e as Map<String, dynamic>).toList();

// ---------- গ্রাহক বিশ্লেষণ ----------
class CustomerRow {
  CustomerRow({
    required this.phone,
    required this.name,
    required this.invoices,
    required this.lifetime,
    this.lastAt,
    required this.due,
    required this.today,
    required this.week,
    required this.month,
    required this.year,
  });

  final String phone;
  final String name;
  final int invoices;
  final double lifetime;
  final DateTime? lastAt;
  final double due;
  final double today;
  final double week;
  final double month;
  final double year;

  factory CustomerRow.fromJson(Map<String, dynamic> j) => CustomerRow(
        phone: _s(j['phone']) ?? '',
        name: _s(j['name']) ?? 'নাম নেই',
        invoices: _i(j['invoices']),
        lifetime: _d(j['lifetime']),
        lastAt: _dt(j['lastAt']),
        due: _d(j['due']),
        today: _d(j['today']),
        week: _d(j['week']),
        month: _d(j['month']),
        year: _d(j['year']),
      );
}

class CustomerListResult {
  CustomerListResult({required this.page, required this.allCustomers, required this.allLifetime, required this.allAverage});

  final PageData<CustomerRow> page;
  final int allCustomers;
  final double allLifetime;
  final double allAverage;

  factory CustomerListResult.fromJson(dynamic json) {
    final j = json as Map<String, dynamic>;
    return CustomerListResult(
      page: PageData<CustomerRow>.fromJson(j, CustomerRow.fromJson),
      allCustomers: _i(j['allCustomers']),
      allLifetime: _d(j['allLifetime']),
      allAverage: _d(j['allAverage']),
    );
  }
}

class Bucket {
  Bucket({required this.label, required this.invoices, required this.amount});

  final String label;
  final int invoices;
  final double amount;

  factory Bucket.fromJson(Map<String, dynamic> j) =>
      Bucket(label: _s(j['label']) ?? '', invoices: _i(j['invoices']), amount: _d(j['amount']));
}

class TopItem {
  TopItem({required this.name, required this.pieces, required this.amount, required this.times});

  final String name;
  final int pieces;
  final double amount;
  final int times;

  factory TopItem.fromJson(Map<String, dynamic> j) => TopItem(
        name: _s(j['name']) ?? '',
        pieces: _i(j['pieces']),
        amount: _d(j['amount']),
        times: _i(j['times']),
      );
}

class CustomerDetail {
  CustomerDetail({
    required this.phone,
    required this.name,
    required this.invoices,
    required this.lifetime,
    required this.average,
    required this.due,
    this.firstAt,
    this.lastAt,
    required this.today,
    required this.week,
    required this.month,
    required this.year,
    required this.daily,
    required this.weekly,
    required this.monthly,
    required this.yearly,
    required this.topItems,
    required this.recent,
  });

  final String phone;
  final String name;
  final int invoices;
  final double lifetime;
  final double average;
  final double due;
  final DateTime? firstAt;
  final DateTime? lastAt;
  final double today;
  final double week;
  final double month;
  final double year;
  final List<Bucket> daily;
  final List<Bucket> weekly;
  final List<Bucket> monthly;
  final List<Bucket> yearly;
  final List<TopItem> topItems;
  final List<Sale> recent;

  factory CustomerDetail.fromJson(Map<String, dynamic> j) => CustomerDetail(
        phone: _s(j['phone']) ?? '',
        name: _s(j['name']) ?? 'নাম নেই',
        invoices: _i(j['invoices']),
        lifetime: _d(j['lifetime']),
        average: _d(j['average']),
        due: _d(j['due']),
        firstAt: _dt(j['firstAt']),
        lastAt: _dt(j['lastAt']),
        today: _d(j['today']),
        week: _d(j['week']),
        month: _d(j['month']),
        year: _d(j['year']),
        daily: _list(j['daily']).map(Bucket.fromJson).toList(),
        weekly: _list(j['weekly']).map(Bucket.fromJson).toList(),
        monthly: _list(j['monthly']).map(Bucket.fromJson).toList(),
        yearly: _list(j['yearly']).map(Bucket.fromJson).toList(),
        topItems: _list(j['topItems']).map(TopItem.fromJson).toList(),
        recent: _list(j['recent']).map(Sale.fromJson).toList(),
      );
}

// ---------- বেশি বিক্রিত ----------
class SellerRow {
  SellerRow({
    required this.name,
    required this.pieces,
    required this.revenue,
    required this.invoices,
    required this.stock,
    required this.marked,
  });

  final String name;
  final int pieces;
  final double revenue;
  final int invoices;
  final int stock;
  final bool marked;

  /// বিক্রির তুলনায় স্টক কম (বিক্রির এক-চতুর্থাংশ বা কম)
  bool get lowForSales => stock <= pieces / 4;

  factory SellerRow.fromJson(Map<String, dynamic> j) => SellerRow(
        name: _s(j['name']) ?? '',
        pieces: _i(j['pieces']),
        revenue: _d(j['revenue']),
        invoices: _i(j['invoices']),
        stock: _i(j['stock']),
        marked: j['marked'] == true,
      );
}

class MarkInfo {
  MarkInfo({
    required this.name,
    required this.soldPieces,
    required this.revenue,
    this.periodLabel,
    this.markedBy,
    this.markedAt,
    required this.auto,
  });

  final String name;
  final int soldPieces;
  final double revenue;
  final String? periodLabel;
  final String? markedBy;
  final DateTime? markedAt;
  final bool auto;

  factory MarkInfo.fromJson(Map<String, dynamic> j) => MarkInfo(
        name: _s(j['name']) ?? '',
        soldPieces: _i(j['soldPieces']),
        revenue: _d(j['revenue']),
        periodLabel: _s(j['periodLabel']),
        markedBy: _s(j['markedBy']),
        markedAt: _dt(j['markedAt']),
        auto: j['auto'] == true,
      );
}

class BestSellers {
  BestSellers({
    required this.periodLabel,
    required this.days,
    required this.sort,
    required this.limit,
    required this.rows,
    required this.marks,
    this.message,
  });

  final String periodLabel;
  final int days;
  final String sort;
  final int limit;
  final List<SellerRow> rows;
  final List<MarkInfo> marks;
  final String? message;

  factory BestSellers.fromJson(dynamic json) {
    final j = json as Map<String, dynamic>;
    return BestSellers(
      periodLabel: _s(j['periodLabel']) ?? '',
      days: _i(j['days']),
      sort: _s(j['sort']) ?? 'qty',
      limit: _i(j['limit']),
      rows: _list(j['rows']).map(SellerRow.fromJson).toList(),
      marks: _list(j['marks']).map(MarkInfo.fromJson).toList(),
      message: _s(j['message']),
    );
  }
}
