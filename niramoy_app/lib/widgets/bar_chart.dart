import 'package:flutter/material.dart';

import '../core/brand.dart';
import '../models/models.dart';

/// বাইরের লাইব্রেরি ছাড়া সাধারণ বার চার্ট (৭ দিনের বিক্রয়)
class SimpleBarChart extends StatelessWidget {
  const SimpleBarChart({super.key, required this.data, this.barHeight = 110});

  final List<TrendPoint> data;
  final double barHeight;

  String _compact(double v) => v >= 1000 ? '${(v / 1000).toStringAsFixed(1)}k' : v.toStringAsFixed(0);

  @override
  Widget build(BuildContext context) {
    final maxV = data.fold<double>(0, (m, e) => e.value > m ? e.value : m);
    return SizedBox(
      height: barHeight + 52,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: data.map((e) {
          final h = maxV <= 0 ? 2.0 : (e.value / maxV * barHeight).clamp(2.0, barHeight).toDouble();
          return Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Text(_compact(e.value), style: const TextStyle(fontSize: 10, color: Brand.muted)),
                const SizedBox(height: 4),
                Container(
                  height: h,
                  margin: const EdgeInsets.symmetric(horizontal: 5),
                  decoration: const BoxDecoration(
                    color: Brand.green,
                    borderRadius: BorderRadius.vertical(top: Radius.circular(6)),
                  ),
                ),
                const SizedBox(height: 4),
                Text(e.label, style: const TextStyle(fontSize: 11)),
              ],
            ),
          );
        }).toList(),
      ),
    );
  }
}
