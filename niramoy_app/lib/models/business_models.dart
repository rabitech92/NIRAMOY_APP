int _i(dynamic v) => v == null ? 0 : (v as num).toInt();
double _d(dynamic v) => v == null ? 0 : (v as num).toDouble();
double? _dn(dynamic v) => v == null ? null : (v as num).toDouble();
String? _s(dynamic v) => v as String?;
List<Map<String, dynamic>> _list(dynamic v) =>
    ((v as List?) ?? const []).map((e) => e as Map<String, dynamic>).toList();
List<double> _doubles(dynamic v) => ((v as List?) ?? const []).map((e) => (e as num).toDouble()).toList();

class BizStock {
  BizStock({
    required this.costValue,
    required this.saleValue,
    required this.potentialProfit,
    required this.units,
    required this.batches,
    required this.expiredCost,
    required this.expiringCost,
  });

  final double costValue;
  final double saleValue;
  final double potentialProfit;
  final int units;
  final int batches;
  final double expiredCost;
  final double expiringCost;

  factory BizStock.fromJson(Map<String, dynamic> j) => BizStock(
        costValue: _d(j['costValue']),
        saleValue: _d(j['saleValue']),
        potentialProfit: _d(j['potentialProfit']),
        units: _i(j['units']),
        batches: _i(j['batches']),
        expiredCost: _d(j['expiredCost']),
        expiringCost: _d(j['expiringCost']),
      );
}

/// একটি মাস বা বছরের সারি
class BizRow {
  BizRow({
    required this.label,
    required this.purchases,
    required this.returns,
    required this.netPurchases,
    required this.sales,
    required this.profit,
    required this.invoices,
    this.stockEnd,
  });

  final String label;
  final double purchases;
  final double returns;
  final double netPurchases;
  final double sales;
  final double profit;
  final int invoices;
  final double? stockEnd;

  factory BizRow.fromJson(Map<String, dynamic> j) => BizRow(
        label: _s(j['label']) ?? '',
        purchases: _d(j['purchases']),
        returns: _d(j['returns']),
        netPurchases: _d(j['netPurchases']),
        sales: _d(j['sales']),
        profit: _d(j['profit']),
        invoices: _i(j['invoices']),
        stockEnd: _dn(j['stockEnd']),
      );
}

class BizValueRow {
  BizValueRow({required this.name, required this.cost, required this.sale, required this.units});

  final String name;
  final double cost;
  final double sale;
  final int units;

  factory BizValueRow.fromJson(Map<String, dynamic> j) =>
      BizValueRow(name: _s(j['name']) ?? '', cost: _d(j['cost']), sale: _d(j['sale']), units: _i(j['units']));
}

class BusinessSummary {
  BusinessSummary({
    required this.stock,
    required this.salesToday,
    required this.salesWeek,
    required this.salesMonth,
    required this.salesYear,
    required this.salesTotal,
    required this.purchaseMonth,
    required this.purchaseYear,
    required this.purchaseTotal,
    required this.months,
    required this.years,
    required this.byBranch,
    required this.byCategory,
    required this.topStock,
    required this.chartLabels,
    required this.chartPurchases,
    required this.chartSales,
  });

  final BizStock stock;
  final double salesToday;
  final double salesWeek;
  final double salesMonth;
  final double salesYear;
  final double salesTotal;
  final double purchaseMonth;
  final double purchaseYear;
  final double purchaseTotal;
  final List<BizRow> months;
  final List<BizRow> years;
  final List<BizValueRow> byBranch;
  final List<BizValueRow> byCategory;
  final List<BizValueRow> topStock;
  final List<String> chartLabels;
  final List<double> chartPurchases;
  final List<double> chartSales;

  factory BusinessSummary.fromJson(dynamic json) {
    final j = json as Map<String, dynamic>;
    return BusinessSummary(
      stock: BizStock.fromJson(j['stock'] as Map<String, dynamic>),
      salesToday: _d(j['salesToday']),
      salesWeek: _d(j['salesWeek']),
      salesMonth: _d(j['salesMonth']),
      salesYear: _d(j['salesYear']),
      salesTotal: _d(j['salesTotal']),
      purchaseMonth: _d(j['purchaseMonth']),
      purchaseYear: _d(j['purchaseYear']),
      purchaseTotal: _d(j['purchaseTotal']),
      months: _list(j['months']).map(BizRow.fromJson).toList(),
      years: _list(j['years']).map(BizRow.fromJson).toList(),
      byBranch: _list(j['byBranch']).map(BizValueRow.fromJson).toList(),
      byCategory: _list(j['byCategory']).map(BizValueRow.fromJson).toList(),
      topStock: _list(j['topStock']).map(BizValueRow.fromJson).toList(),
      chartLabels: ((j['chartLabels'] as List?) ?? const []).map((e) => e as String).toList(),
      chartPurchases: _doubles(j['chartPurchases']),
      chartSales: _doubles(j['chartSales']),
    );
  }
}
