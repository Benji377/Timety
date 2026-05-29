import 'package:flutter/material.dart';
import '../../data/habit/habit_models.dart';
import '../../theme/app_theme.dart';

class HabitWidgetHeaderView extends StatelessWidget {
  final int habitCount;

  const HabitWidgetHeaderView({super.key, required this.habitCount});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 400,
      padding: const EdgeInsets.all(AppTheme.spaceMedium),
      decoration: const BoxDecoration(
        color: Colors.transparent,
      ),
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
                  vertical: BorderSide(color: AppTheme.typeHabitColor, width: 2),
                ),
              )
            : BoxDecoration(
                color: Colors.white,
                border: Border.all(
                  color: isDone ? AppTheme.successColor : AppTheme.typeHabitColor,
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

class HabitStackHeaderView extends StatelessWidget {
  final String name;
  final int completed;
  final int total;

  const HabitStackHeaderView({
    super.key,
    required this.name,
    required this.completed,
    required this.total,
  });

  @override
  Widget build(BuildContext context) {
    final allDone = completed == total;

    return Container(
      width: 400,
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      decoration: const BoxDecoration(color: Colors.transparent),
      child: Container(
        padding: const EdgeInsets.all(AppTheme.spaceSmall),
        decoration: BoxDecoration(
          color: AppTheme.typeHabitColor.withValues(alpha: 0.1),
          border: const Border(
            top: BorderSide(color: AppTheme.typeHabitColor, width: 2),
            left: BorderSide(color: AppTheme.typeHabitColor, width: 2),
            right: BorderSide(color: AppTheme.typeHabitColor, width: 2),
          ),
          borderRadius: const BorderRadius.vertical(top: Radius.circular(AppTheme.radiusMedium)),
        ),
        child: Row(
          children: [
            const Icon(Icons.layers, size: 12, color: AppTheme.typeHabitColor),
            const SizedBox(width: 4),
            Text(
              name.toUpperCase(),
              style: const TextStyle(
                fontSize: 10,
                fontWeight: AppTheme.fwBold,
                color: AppTheme.typeHabitColor,
              ),
            ),
            const Spacer(),
            Text(
              '$completed / $total',
              style: TextStyle(
                fontSize: 10,
                fontWeight: AppTheme.fwBold,
                color: allDone ? AppTheme.successColor : AppTheme.typeHabitColor,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

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
          borderRadius: BorderRadius.vertical(bottom: Radius.circular(AppTheme.radiusMedium)),
        ),
      ),
    );
  }
}
