import 'package:flutter/material.dart';
import '../../../../../theme/app_theme.dart';

class HabitWidgetHeaderView extends StatelessWidget {
  final int habitCount;
  final String title;

  const HabitWidgetHeaderView({
    super.key,
    required this.habitCount,
    required this.title,
  });

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
          Expanded(
            child: Text(
              title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: AppTheme.fsHeadingSmall,
                fontWeight: AppTheme.fwBold,
                color: AppTheme.typeHabitColor,
                letterSpacing: AppTheme.lsTight,
              ),
            ),
          ),
          const SizedBox(width: 8),
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
