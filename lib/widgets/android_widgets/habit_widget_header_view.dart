import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';

class HabitWidgetHeaderView extends StatelessWidget {
  final int habitCount;

  const HabitWidgetHeaderView({super.key, required this.habitCount});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 400,
      padding: const EdgeInsets.all(AppTheme.spaceMedium),
      decoration: const BoxDecoration(color: Colors.transparent),
      child: Row(
        children: [
          const Icon(
            Icons.alarm_outlined,
            color: AppTheme.typeHabitColor,
            size: AppTheme.iconSizeMedium,
          ),
          const SizedBox(width: AppTheme.spaceSmall),
          const Text(
            'Habits Today',
            style: TextStyle(
              fontSize: AppTheme.fsHeadingSmall,
              fontWeight: AppTheme.fwBold,
              color: AppTheme.typeHabitColor,
              letterSpacing: AppTheme.lsTight,
            ),
          ),
          const Spacer(),
          Text(
            '$habitCount',
            style: const TextStyle(
              fontSize: AppTheme.fsHeadingSmall,
              fontWeight: AppTheme.fwBold,
              color: AppTheme.typeHabitColor,
            ),
          ),
        ],
      ),
    );
  }
}
