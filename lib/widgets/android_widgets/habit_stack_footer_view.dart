import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';

class HabitStackFooterView extends StatelessWidget {
  const HabitStackFooterView({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 400,
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 4),
      decoration: const BoxDecoration(color: Colors.transparent),
      child: Container(
        height: 2,
        decoration: const BoxDecoration(
          color: AppTheme.typeHabitColor,
          borderRadius: BorderRadius.vertical(
            bottom: Radius.circular(AppTheme.radiusMedium),
          ),
        ),
      ),
    );
  }
}
