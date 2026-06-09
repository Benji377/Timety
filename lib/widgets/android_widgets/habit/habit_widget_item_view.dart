import 'package:flutter/material.dart';
import '../../../../../data/habit/habit_models.dart';
import '../../../../../theme/app_theme.dart';

class HabitWidgetItemView extends StatelessWidget {
  final Habit habit;
  final bool isDone;
  final bool isLocked;
  final bool isStacked;
  final String frequency;

  const HabitWidgetItemView({
    super.key,
    required this.habit,
    required this.isDone,
    required this.isLocked,
    required this.frequency,
    this.isStacked = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 400,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: const BoxDecoration(color: Colors.transparent),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        decoration: isStacked
            ? BoxDecoration(
                color: AppTheme.typeHabitColor.withValues(alpha: 0.05),
                border: const Border.symmetric(
                  vertical: BorderSide(
                    color: AppTheme.typeHabitColor,
                    width: 2,
                  ),
                ),
              )
            : BoxDecoration(
                color: Colors.white,
                border: Border.all(
                  color: isDone
                      ? AppTheme.successColor
                      : AppTheme.typeHabitColor,
                  width: AppTheme.listTileBorderWidth,
                ),
                borderRadius: AppTheme.brMedium,
              ),
        child: Row(
          children: [
            Icon(
              isLocked
                  ? Icons.lock_outline
                  : (habit.iconData ?? Icons.fiber_manual_record),
              size: 16,
              color: isLocked
                  ? Colors.grey
                  : (isDone ? AppTheme.successColor : Color(habit.colorValue)),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                habit.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontSize: AppTheme.fsBodyMedium,
                  fontWeight: AppTheme.fwBold,
                  color: isLocked ? Colors.grey : AppTheme.inkLight,
                  decoration: isDone ? TextDecoration.lineThrough : null,
                ),
              ),
            ),
            if (frequency.isNotEmpty) ...[
              const SizedBox(width: 4),
              Text(
                frequency,
                style: const TextStyle(
                  fontSize: AppTheme.fsCaption,
                  color: Colors.grey,
                  fontWeight: AppTheme.fwMedium,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
