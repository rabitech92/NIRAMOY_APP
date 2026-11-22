import '../core/format.dart';

// ---------- JSON সহায়ক ----------
int _i(dynamic v) => v == null ? 0 : (v as num).toInt();
double _d(dynamic v) => v == null ? 0 : (v as num).toDouble();
double? _dn(dynamic v) => v == null ? null : (v as num).toDouble();
String? _s(dynamic v) => v as String?;
DateTime? _dt(dynamic v) => v == null ? null : DateTime.tryParse(v as String);

List<Map<String, dynamic>> _list(dynamic v) =>
    ((v as List?) ?? const []).map((e) => e as Map<String, dynamic>).toList();

class PageData<T> {
  PageData({required this.items, required this.page, required this.totalPages, required this.totalElements});

  final List<T> items;
  final int page;
  final int totalPages;
  final int totalElements;

  bool get hasMore => page + 1 < totalPages;

  factory PageData.fromJson(dynamic json, T Function(Map<String, dynamic>) parse) {
    final j = json as Map<String, dynamic>;
    return PageData<T>(
      items: _list(j['items']).map(parse).toList(),
      page: _i(j['page']),
      totalPages: _i(j['totalPages']),
      totalElements: _i(j['totalElements']),
    );
  }
}

// ---------- ব্যবহারকারী / শাখা / সাবস্ক্রিপশন ----------
class AppUser {
  AppUser({
    required this.id,
    required this.username,
    required this.fullName,
    required this.role,
    required this.roleLabel,
    required this.isGlobal,
    this.branchId,
    this.branchName,
  });

  final int id;
  final String username;
  final String fullName;
  final String role;
  final String roleLabel;
  final bool isGlobal;
  final int? branchId;
  final String? branchName;

  factory AppUser.fromJson(Map<String, dynamic> j) => AppUser(
        id: _i(j['id']),
        username: _s(j['username']) ?? '',
        fullName: _s(j['fullName']) ?? '',
        role: _s(j['role']) ?? '',
        roleLabel: _s(j['roleLabel']) ?? '',
        isGlobal: j['global'] == true,
        branchId: j['branchId'] == null ? null : _i(j['branchId']),
        branchName: _s(j['branchName']),
      );

  // সার্ভারের URL অনুমতির সাথে মিল রাখা হয়েছে
  bool get canSell => const ['SUPER_ADMIN', 'OWNER', 'BRANCH_MANAGER', 'PHARMACIST', 'CASHIER'].contains(role);
  bool get canInventory => const ['SUPER_ADMIN', 'OWNER', 'BRANCH_MANAGER', 'PHARMACIST', 'INVENTORY_MANAGER'].contains(role);
  bool get canExpenses => const ['SUPER_ADMIN', 'OWNER', 'BRANCH_MANAGER'].contains(role);
  bool get canVoidExpense => const ['SUPER_ADMIN', 'OWNER'].contains(role);
  bool get canCustomers => const ['SUPER_ADMIN', 'OWNER', 'BRANCH_MANAGER'].contains(role);
  bool get canMark => const ['SUPER_ADMIN', 'OWNER', 'BRANCH_MANAGER', 'INVENTORY_MANAGER'].contains(role);
  bool get canDues => const ['SUPER_ADMIN', 'OWNER', 'BRANCH_MANAGER', 'INVENTORY_MANAGER'].contains(role);
}

class Branch {
  Branch({required this.id, required this.name});

  final int id;
  final String name;

  factory Branch.fromJson(Map<String, dynamic> j) => Branch(id: _i(j['id']), name: _s(j['name']) ?? '');
}

class SubscriptionInfo {
  SubscriptionInfo({
    required this.level,
    required this.planName,
    required this.endDate,
    required this.daysLeft,
    required this.blocked,
    required this.warning,
    required this.pending,
    required this.message,
  });

  final String level;
  final String planName;
  final DateTime? endDate;
  final int daysLeft;
  final bool blocked;
  final bool warning;
  final bool pending;
  final String message;

  factory SubscriptionInfo.fromJson(Map<String, dynamic> j) => SubscriptionInfo(
        level: _s(j['level']) ?? '',
        planName: _s(j['planName']) ?? '—',
        endDate: _dt(j['endDate']),
        daysLeft: _i(j['daysLeft']),
        blocked: j['blocked'] == true,
        warning: j['warning'] == true,
        pending: j['pending'] == true,
        message: _s(j['message']) ?? '',
      );
}

// ---------- ওষুধ ----------
class Medicine {
  Medicine({
    required this.id,
    required this.name,
    this.genericName,
    this.companyName,
    this.category,
    this.barcode,
    required this.batchNo,
    this.expiryDate,
    required this.stock,
    required this.stripSize,
    required this.price,
    required this.stripPrice,
    this.costPrice,
    this.boxNo,
    required this.branchName,
    required this.lowStock,
    required this.expired,
    required this.expiringSoon,
    this.bestSeller = false,
  });

  final int id;
  final String name;
  final String? genericName;
  final String? companyName;
  final String? category;
  final String? barcode;
  final String batchNo;
  final DateTime? expiryDate;
  final int stock;
  final int stripSize;
  final double price;
  final double stripPrice;
  final double? costPrice;
  final String? boxNo;
  final String branchName;
  final bool lowStock;
  final bool expired;
  final bool expiringSoon;

  /// "বেশি বিক্রিত" চিহ্নিত ওষুধ
  final bool bestSeller;

  factory Medicine.fromJson(Map<String, dynamic> j) => Medicine(
        id: _i(j['id']),
        name: _s(j['name']) ?? '',
        genericName: _s(j['genericName']),
        companyName: _s(j['companyName']),
        category: _s(j['category']),
        barcode: _s(j['barcode']),
        batchNo: _s(j['batchNo']) ?? '',
        expiryDate: _dt(j['expiryDate']),
        stock: _i(j['stock']),
        stripSize: _i(j['stripSize']) < 1 ? 1 : _i(j['stripSize']),
        price: _d(j['price']),
        stripPrice: _d(j['stripPrice']),
        costPrice: _dn(j['costPrice']),
        boxNo: _s(j['boxNo']),
        branchName: _s(j['branchName']) ?? '',
        lowStock: j['lowStock'] == true,
        expired: j['expired'] == true,
        expiringSoon: j['expiringSoon'] == true,
        bestSeller: j['bestSeller'] == true,
      );

  String get stockLabel => stockText(stock, stripSize);

  /// জেনেরিক ও কোম্পানি একসাথে
  String get subtitle => [genericName, companyName].where((e) => e != null && e.isNotEmpty).join(' · ');
}

// ---------- ইনভয়েস ----------
class SaleItem {
  SaleItem({
    required this.name,
    required this.batchNo,
    required this.quantity,
    required this.unit,
    required this.unitPrice,
    required this.lineTotal,
  });

  final String name;
  final String batchNo;
  final int quantity;
  final String unit;
  final double unitPrice;
  final double lineTotal;

  factory SaleItem.fromJson(Map<String, dynamic> j) => SaleItem(
        name: _s(j['name']) ?? '',
        batchNo: _s(j['batchNo']) ?? '',
        quantity: _i(j['quantity']),
        unit: _s(j['unit']) ?? 'পিস',
        unitPrice: _d(j['unitPrice']),
        lineTotal: _d(j['lineTotal']),
      );
}

class Sale {
  Sale({
    required this.id,
    required this.invoiceNo,
    this.createdAt,
    required this.branchName,
    this.customerName,
    this.customerPhone,
    required this.paymentLabel,
    required this.status,
    required this.statusLabel,
    required this.subtotal,
    required this.discount,
    required this.vatPercent,
    required this.vatAmount,
    required this.total,
    required this.dueAmount,
    this.cashier,
    required this.items,
  });

  final int id;
  final String invoiceNo;
  final DateTime? createdAt;
  final String branchName;
  final String? customerName;
  final String? customerPhone;
  final String paymentLabel;
  final String status;
  final String statusLabel;
  final double subtotal;
  final double discount;
  final double vatPercent;
  final double vatAmount;
  final double total;
  final double dueAmount;
  final String? cashier;
  final List<SaleItem> items;

  bool get returned => status == 'RETURNED';
  bool get hasDue => dueAmount > 0.004;
  double get paid => total - dueAmount;

  factory Sale.fromJson(Map<String, dynamic> j) => Sale(
        id: _i(j['id']),
        invoiceNo: _s(j['invoiceNo']) ?? '',
        createdAt: _dt(j['createdAt']),
        branchName: _s(j['branchName']) ?? '',
        customerName: _s(j['customerName']),
        customerPhone: _s(j['customerPhone']),
        paymentLabel: _s(j['paymentLabel']) ?? '',
        status: _s(j['status']) ?? '',
        statusLabel: _s(j['statusLabel']) ?? '',
        subtotal: _d(j['subtotal']),
        discount: _d(j['discount']),
        vatPercent: _d(j['vatPercent']),
        vatAmount: _d(j['vatAmount']),
        total: _d(j['total']),
        dueAmount: _d(j['dueAmount']),
        cashier: _s(j['cashier']),
        items: _list(j['items']).map(SaleItem.fromJson).toList(),
      );

  /// ক্লিপবোর্ডে কপি/শেয়ারের জন্য সাদামাটা রসিদ
  String toReceiptText() {
    final b = StringBuffer()
      ..writeln('নিরাময় ফার্মেসি — $branchName শাখা')
      ..writeln('ইনভয়েস: $invoiceNo')
      ..writeln('তারিখ: ${dateTimeText(createdAt)}');
    if ((customerName ?? '').isNotEmpty) b.writeln('গ্রাহক: $customerName');
    if ((customerPhone ?? '').isNotEmpty) b.writeln('মোবাইল: $customerPhone');
    b.writeln('------------------------');
    for (final i in items) {
      b.writeln('${i.name}  ${i.quantity} ${i.unit} × ${money(i.unitPrice)} = ${money(i.lineTotal)}');
    }
    b
      ..writeln('------------------------')
      ..writeln('উপমোট: ${money(subtotal)}')
      ..writeln('ছাড়: ${money(discount)}')
      ..writeln('VAT: ${money(vatAmount)}')
      ..writeln('সর্বমোট: ${money(total)}')
      ..writeln('পেমেন্ট: $paymentLabel');
    if (hasDue) b.writeln('বকেয়া: ${money(dueAmount)}');
    return b.toString();
  }
}

// ---------- রিফিল সাবস্ক্রিপশন ----------
class RefillItem {
  RefillItem({required this.medicineName, required this.quantity, required this.unit});

  final String medicineName;
  final int quantity;
  final String unit;

  factory RefillItem.fromJson(Map<String, dynamic> j) =>
      RefillItem(medicineName: _s(j['medicineName']) ?? '', quantity: _i(j['quantity']), unit: _s(j['unit']) ?? 'পিস');
}

class Refill {
  Refill({
    required this.id,
    required this.customerName,
    required this.customerPhone,
    required this.planName,
    required this.frequencyDays,
    this.startDate,
    this.nextRefillDate,
    required this.daysToNext,
    required this.status,
    required this.statusLabel,
    required this.discountPercent,
    required this.cycles,
    required this.due,
    this.note,
    required this.branchName,
    required this.items,
  });

  final int id;
  final String customerName;
  final String customerPhone;
  final String planName;
  final int frequencyDays;
  final DateTime? startDate;
  final DateTime? nextRefillDate;
  final int daysToNext;
  final String status;
  final String statusLabel;
  final double discountPercent;
  final int cycles;
  final bool due;
  final String? note;
  final String branchName;
  final List<RefillItem> items;

  bool get active => status == 'ACTIVE';

  factory Refill.fromJson(Map<String, dynamic> j) => Refill(
        id: _i(j['id']),
        customerName: _s(j['customerName']) ?? '',
        customerPhone: _s(j['customerPhone']) ?? '',
        planName: _s(j['planName']) ?? '',
        frequencyDays: _i(j['frequencyDays']),
        startDate: _dt(j['startDate']),
        nextRefillDate: _dt(j['nextRefillDate']),
        daysToNext: _i(j['daysToNext']),
        status: _s(j['status']) ?? '',
        statusLabel: _s(j['statusLabel']) ?? '',
        discountPercent: _d(j['discountPercent']),
        cycles: _i(j['cycles']),
        due: j['due'] == true,
        note: _s(j['note']),
        branchName: _s(j['branchName']) ?? '',
        items: _list(j['items']).map(RefillItem.fromJson).toList(),
      );
}

// ---------- ড্যাশবোর্ড ----------
class TrendPoint {
  TrendPoint({required this.label, required this.value});

  final String label;
  final double value;

  factory TrendPoint.fromJson(Map<String, dynamic> j) => TrendPoint(label: _s(j['label']) ?? '', value: _d(j['value']));
}

class MixPoint {
  MixPoint({required this.label, required this.percent});

  final String label;
  final int percent;

  factory MixPoint.fromJson(Map<String, dynamic> j) => MixPoint(label: _s(j['label']) ?? '', percent: _i(j['percent']));
}

class Dashboard {
  Dashboard({
    required this.todaySales,
    this.todayProfit,
    this.receivable,
    this.supplierDue,
    required this.lowStock,
    required this.expiring,
    required this.medicineCount,
    required this.branchCount,
    required this.dueRefills,
    required this.trend,
    required this.paymentMix,
    required this.recentSales,
  });

  final double todaySales;
  final double? todayProfit;
  final double? receivable;
  final double? supplierDue;
  final int lowStock;
  final int expiring;
  final int medicineCount;
  final int branchCount;
  final int dueRefills;
  final List<TrendPoint> trend;
  final List<MixPoint> paymentMix;
  final List<Sale> recentSales;

  factory Dashboard.fromJson(Map<String, dynamic> j) => Dashboard(
        todaySales: _d(j['todaySales']),
        todayProfit: _dn(j['todayProfit']),
        receivable: _dn(j['receivable']),
        supplierDue: _dn(j['supplierDue']),
        lowStock: _i(j['lowStock']),
        expiring: _i(j['expiring']),
        medicineCount: _i(j['medicineCount']),
        branchCount: _i(j['branchCount']),
        dueRefills: _i(j['dueRefills']),
        trend: _list(j['trend']).map(TrendPoint.fromJson).toList(),
        paymentMix: _list(j['paymentMix']).map(MixPoint.fromJson).toList(),
        recentSales: _list(j['recentSales']).map(Sale.fromJson).toList(),
      );
}

class Dues {
  Dues({required this.total, required this.items});

  final double total;
  final List<Sale> items;

  factory Dues.fromJson(Map<String, dynamic> j) =>
      Dues(total: _d(j['total']), items: _list(j['items']).map(Sale.fromJson).toList());
}
