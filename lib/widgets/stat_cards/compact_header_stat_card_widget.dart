import 'package:flutter/material.dart';
import './stat_cards.dart';
import '../../data/other/stat_card_style.dart';

/// @deprecated Use StatCard with style: StatCardStyle.compactHeader instead
class CompactHeaderStatCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;

  const CompactHeaderStatCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return StatCard(
      title: title,
      value: value,
      icon: icon,
      color: color,
      style: StatCardStyle.compactHeader,
    );
  }
}
