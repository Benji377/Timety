import 'package:flutter/material.dart';
import 'stat_card.dart';

/// @deprecated Use StatCard with style: StatCardStyle.kpi instead
class KpiStatCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;

  const KpiStatCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return StatCard(title: title, value: value, icon: icon, color: color);
  }
}
