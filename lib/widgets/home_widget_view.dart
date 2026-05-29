import 'package:flutter/material.dart';
import '../data/task/task.dart';
import '../theme/app_theme.dart';
import '../utils/priority_utils.dart';

class HomeWidgetView extends StatelessWidget {
  final List<Task> tasks;

  const HomeWidgetView({super.key, required this.tasks});

  Color _getBorderColor(Task task) {
    if (task.isCompleted) return AppTheme.successColor;
    
    final now = DateTime.now();
    
    if (task.dueDate != null) {
      // If due date has passed, it's overdue (Red)
      if (task.dueDate!.isBefore(now)) return AppTheme.errorColor;
      
      // If it's still today but not yet overdue, it's due today (Orange)
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
    final displayedTasks = tasks.take(3).toList();

    return Container(
      width: 400, // Fixed width for rendering
      height: 250, // Fixed height for rendering
      padding: const EdgeInsets.all(AppTheme.spaceMedium),
      decoration: const BoxDecoration(
        color: AppTheme.paperLight,
        // No border on the outer container to look cleaner on home screen
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(
                Icons.check_circle_outline,
                color: AppTheme.warningColor,
                size: AppTheme.iconSizeMedium,
              ),
              const SizedBox(width: AppTheme.spaceSmall),
              Text(
                'Tasks Due (${tasks.length})',
                style: const TextStyle(
                  fontSize: AppTheme.fsHeadingSmall,
                  fontWeight: AppTheme.fwBold,
                  color: AppTheme.warningColor,
                  letterSpacing: AppTheme.lsTight,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppTheme.spaceMedium),
          if (tasks.isEmpty)
            const Expanded(
              child: Center(
                child: Text(
                  "You're all caught up!",
                  style: TextStyle(
                    fontSize: AppTheme.fsBodyMedium,
                    color: AppTheme.inkLight,
                    fontWeight: AppTheme.fwMedium,
                  ),
                ),
              ),
            )
          else ...[
            for (var i = 0; i < displayedTasks.length; i++) ...[
              _buildTaskItem(displayedTasks[i]),
              if (i < displayedTasks.length - 1)
                const SizedBox(height: AppTheme.spaceSmall),
            ],
            if (tasks.length > 3) ...[
              const SizedBox(height: AppTheme.spaceSmall),
              Align(
                alignment: Alignment.centerRight,
                child: Text(
                  '+ ${tasks.length - 3} more tasks',
                  style: const TextStyle(
                    fontSize: AppTheme.fsCaption,
                    color: AppTheme.inkLight,
                    fontWeight: AppTheme.fwBold,
                    fontStyle: FontStyle.italic,
                  ),
                ),
              ),
            ],
          ],
        ],
      ),
    );
  }

  Widget _buildTaskItem(Task task) {
    final borderColor = _getBorderColor(task);

    return Container(
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
    );
  }
}
