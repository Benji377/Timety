import 'package:flutter/material.dart';
import '../../../../../theme/app_theme.dart';

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
          borderRadius: const BorderRadius.vertical(
            top: Radius.circular(AppTheme.radiusMedium),
          ),
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
                color: allDone
                    ? AppTheme.successColor
                    : AppTheme.typeHabitColor,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
