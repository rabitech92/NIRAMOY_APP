import 'package:flutter/foundation.dart';

import '../models/models.dart';

class CartLine {
  CartLine(this.med, {this.strips = 0, this.pieces = 0});

  final Medicine med;
  int strips;
  int pieces;

  int get pack => med.stripSize < 1 ? 1 : med.stripSize;

  /// স্টক থেকে যত পিস কমবে
  int get needPieces => strips * pack + pieces;

  double get total => strips * med.stripPrice + pieces * med.price;
}

/// POS কার্ট
class CartState extends ChangeNotifier {
  final List<CartLine> lines = [];

  bool get isEmpty => lines.isEmpty;
  int get count => lines.length;
  double get subtotal => lines.fold<double>(0, (sum, l) => sum + l.total);

  /// স্টক পর্যাপ্ত হলে true
  bool add(Medicine m, {required bool strip}) {
    CartLine? line;
    for (final l in lines) {
      if (l.med.id == m.id) line = l;
    }
    final target = line ?? CartLine(m);
    if (strip) {
      target.strips += 1;
    } else {
      target.pieces += 1;
    }
    if (target.needPieces > m.stock) {
      if (strip) {
        target.strips -= 1;
      } else {
        target.pieces -= 1;
      }
      return false;
    }
    if (line == null) lines.add(target);
    notifyListeners();
    return true;
  }

  /// পাতা ও পিস হাতে লিখে বদলানো। স্টকের বেশি হলে false; দুটোই ০ হলে লাইন মুছে যায়
  bool setQty(CartLine line, int strips, int pieces) {
    final s = strips < 0 ? 0 : strips;
    final p = pieces < 0 ? 0 : pieces;
    if (s * line.pack + p > line.med.stock) return false;
    line.strips = s;
    line.pieces = p;
    if (s == 0 && p == 0) lines.remove(line);
    notifyListeners();
    return true;
  }

  void remove(CartLine line) {
    lines.remove(line);
    notifyListeners();
  }

  void clear() {
    lines.clear();
    notifyListeners();
  }
}
