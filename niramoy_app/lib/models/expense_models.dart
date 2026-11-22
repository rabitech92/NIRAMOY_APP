import 'models.dart' show PageData;

int _i(dynamic v) => v == null ? 0 : (v as num).toInt();
double _d(dynamic v) => v == null ? 0 : (v as num).toDouble();
String? _s(dynamic v) => v as String?;
DateTime? _dt(dynamic v) => v == null ? null : DateTime.tryParse(v as String);
List<Map<String, dynamic>> _list(dynamic v) =>
    ((v as List?) ?? const []).map((e) => e as Map<String, dynamic>).toList();

class ExpensePayment {
  ExpensePayment({this.paidOn, required this.methodLabel, required this.amount, this.referenceNo, this.paidBy});

  final DateTime? paidOn;
  final String methodLabel;
  final double amount;
  final String? referenceNo;
  final String? paidBy;

  factory ExpensePayment.fromJson(Map<String, dynamic> j) => ExpensePayment(
        paidOn: _dt(j['paidOn']),
        methodLabel: _s(j['methodLabel']) ?? '',
        amount: _d(j['amount']),
        referenceNo: _s(j['referenceNo']),
        paidBy: _s(j['paidBy']),
      );
}

class Expense {
  Expense({
    required this.id,
    required this.expenseNo,
    this.expenseDate,
    required this.categoryName,
    this.subCategoryName,
    required this.amount,
    required this.paidAmount,
    required this.dueAmount,
    required this.status,
    required this.statusLabel,
    this.paidTo,
    this.referenceNo,
    this.description,
    required this.branchName,
    this.createdBy,
    this.createdAt,
    required this.voided,
    this.voidReason,
    this.voidedBy,
    required this.payments,
  });

  final int id;
  final String expenseNo;
  final DateTime? expenseDate;
  final String categoryName;
  final String? subCategoryName;
  final double amount;
  final double paidAmount;
  final double dueAmount;
  final String status;
  final String statusLabel;
  final String? paidTo;
  final String? referenceNo;
  final String? description;
  final String branchName;
  final String? createdBy;
  final DateTime? createdAt;
  final bool voided;
  final String? voidReason;
  final String? voidedBy;
  final List<ExpensePayment> payments;

  bool get hasDue => dueAmount > 0.004;

  factory Expense.fromJson(Map<String, dynamic> j) => Expense(
        id: _i(j['id']),
        expenseNo: _s(j['expenseNo']) ?? '',
        expenseDate: _dt(j['expenseDate']),
        categoryName: _s(j['categoryName']) ?? '',
        subCategoryName: _s(j['subCategoryName']),
        amount: _d(j['amount']),
        paidAmount: _d(j['paidAmount']),
        dueAmount: _d(j['dueAmount']),
        status: _s(j['status']) ?? '',
        statusLabel: _s(j['statusLabel']) ?? '',
        paidTo: _s(j['paidTo']),
        referenceNo: _s(j['referenceNo']),
        description: _s(j['description']),
        branchName: _s(j['branchName']) ?? '',
        createdBy: _s(j['createdBy']),
        createdAt: _dt(j['createdAt']),
        voided: j['voided'] == true,
        voidReason: _s(j['voidReason']),
        voidedBy: _s(j['voidedBy']),
        payments: _list(j['payments']).map(ExpensePayment.fromJson).toList(),
      );
}

class ExpenseList {
  ExpenseList({required this.page, required this.totalAmount, required this.totalPaid});

  final PageData<Expense> page;
  final double totalAmount;
  final double totalPaid;

  factory ExpenseList.fromJson(dynamic json) {
    final j = json as Map<String, dynamic>;
    return ExpenseList(
      page: PageData<Expense>.fromJson(j, Expense.fromJson),
      totalAmount: _d(j['totalAmount']),
      totalPaid: _d(j['totalPaid']),
    );
  }
}

class Slice {
  Slice({required this.label, required this.amount, required this.percent});

  final String label;
  final double amount;
  final int percent;

  factory Slice.fromJson(Map<String, dynamic> j) =>
      Slice(label: _s(j['label']) ?? '', amount: _d(j['amount']), percent: _i(j['percent']));
}

class ExpenseDashboard {
  ExpenseDashboard({
    required this.today,
    required this.month,
    required this.pending,
    required this.year,
    required this.monthLabel,
    required this.yearNumber,
    required this.byCategory,
    required this.byPayment,
    required this.recent,
  });

  final double today;
  final double month;
  final double pending;
  final double year;
  final String monthLabel;
  final int yearNumber;
  final List<Slice> byCategory;
  final List<Slice> byPayment;
  final List<Expense> recent;

  factory ExpenseDashboard.fromJson(Map<String, dynamic> j) => ExpenseDashboard(
        today: _d(j['today']),
        month: _d(j['month']),
        pending: _d(j['pending']),
        year: _d(j['year']),
        monthLabel: _s(j['monthLabel']) ?? '',
        yearNumber: _i(j['yearNumber']),
        byCategory: _list(j['byCategory']).map(Slice.fromJson).toList(),
        byPayment: _list(j['byPayment']).map(Slice.fromJson).toList(),
        recent: _list(j['recent']).map(Expense.fromJson).toList(),
      );
}

class ExpenseCategory {
  ExpenseCategory({required this.id, required this.name, this.nameEn, required this.groupLabel});

  final int id;
  final String name;
  final String? nameEn;
  final String groupLabel;

  factory ExpenseCategory.fromJson(Map<String, dynamic> j) => ExpenseCategory(
        id: _i(j['id']),
        name: _s(j['name']) ?? '',
        nameEn: _s(j['nameEn']),
        groupLabel: _s(j['groupLabel']) ?? '',
      );

  String get label => (nameEn == null || nameEn!.isEmpty) ? name : '$name ($nameEn)';
}

class ExpenseSub {
  ExpenseSub({required this.id, required this.categoryId, required this.name});

  final int id;
  final int categoryId;
  final String name;

  factory ExpenseSub.fromJson(Map<String, dynamic> j) =>
      ExpenseSub(id: _i(j['id']), categoryId: _i(j['categoryId']), name: _s(j['name']) ?? '');
}

class ExpenseMeta {
  ExpenseMeta({required this.categories, required this.subs});

  final List<ExpenseCategory> categories;
  final List<ExpenseSub> subs;

  factory ExpenseMeta.fromJson(Map<String, dynamic> j) => ExpenseMeta(
        categories: _list(j['categories']).map(ExpenseCategory.fromJson).toList(),
        subs: _list(j['subCategories']).map(ExpenseSub.fromJson).toList(),
      );
}
