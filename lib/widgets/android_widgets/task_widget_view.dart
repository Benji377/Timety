import 'package:flutter/material.dart';
import '../../data/task/task.dart';
import '../../theme/app_theme.dart';
import '../../utils/priority_utils.dart';

class TaskWidgetHeaderView extends StatelessWidget {
  final int taskCount;

  const TaskWidgetHeaderView({super.key, required this.taskCount});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 400,
      padding: const EdgeInsets.all(AppTheme.spaceMedium),
      decoration: const BoxDecoration(color: Colors.transparent),
      child: Row(
        children: [
          const Icon(
            Icons.check_circle_outline,
            color: AppTheme.warningColor,
            size: AppTheme.iconSizeMedium,
          ),
          const SizedBox(width: AppTheme.spaceSmall),
          Text(
            'Tasks Due ($taskCount)',
            style: const TextStyle(
              fontSize: AppTheme.fsHeadingSmall,
              fontWeight: AppTheme.fwBold,
              color: AppTheme.warningColor,
              letterSpacing: AppTheme.lsTight,
            ),
          ),
        ],
      ),
    );
  }
}

class TaskWidgetItemView extends StatelessWidget {
  final Task task;

  const TaskWidgetItemView({super.key, required this.task});

  Color _getBorderColor(Task task) {
    if (task.isCompleted) return AppTheme.successColor;
    final now = DateTime.now();
    if (task.dueDate != null) {
      if (task.dueDate!.isBefore(now)) return AppTheme.errorColor;
      final todayDate = DateTime(now.year, now.month, now.day);
      final dueDayDate = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );
      if (dueDayDate.isAtSameMomentAs(todayDate)) return AppTheme.warningColor;
    }
    return AppTheme.taskColor;
  }

  @override
  Widget build(BuildContext context) {
    final borderColor = _getBorderColor(task);

    return Container(
      width: 400,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      decoration: const BoxDecoration(color: Colors.transparent),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spaceMedium,
          vertical: AppTheme.spaceSmall,
        ),
        decoration: BoxDecoration(
          color: Colors.white,
          border: Border.all(
            color: borderColor,
            width: AppTheme.listTileBorderWidth,
          ),
          borderRadius: AppTheme.brMedium,
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    task.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: AppTheme.fsBodyMedium,
                      fontWeight: AppTheme.fwBold,
                      color: AppTheme.inkLight,
                    ),
                  ),
                  if (task.dueDate != null)
                    Text(
                      '${task.dueDate!.month.toString().padLeft(2, '0')}/${task.dueDate!.day.toString().padLeft(2, '0')} ${task.dueDate!.hour.toString().padLeft(2, '0')}:${task.dueDate!.minute.toString().padLeft(2, '0')}',
                      style: TextStyle(
                        fontSize: AppTheme.fsCaption,
                        color: borderColor,
                        fontWeight: AppTheme.fwMedium,
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(width: AppTheme.spaceSmall),
            Text(
              AppUtils().getSizeEmoji(task.size),
              style: const TextStyle(fontSize: 16),
            ),
            const SizedBox(width: AppTheme.spaceSmall),
            AppUtils().getPriorityIcon(task.priority),
          ],
        ),
      ),
    );
  }
}
