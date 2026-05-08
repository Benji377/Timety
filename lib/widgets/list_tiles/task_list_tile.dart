import 'package:flutter/material.dart';
import '../../data/task/task.dart';
import '../../theme/app_theme.dart';
import '../../utils/utils.dart';
import '../app_dialogs.dart';

class TaskListTile extends StatelessWidget {
  final Task task;
  final VoidCallback onToggleCompleted;
  final VoidCallback onTap;
  final VoidCallback? onDelete;
  final bool enableDismissible;
  final bool showDescription;
  final bool showDueDate;
  final bool showTrailing;
  final EdgeInsetsGeometry margin;
  final String deleteTitle;
  final String deleteContent;

  const TaskListTile({
    super.key,
    required this.task,
    required this.onToggleCompleted,
    required this.onTap,
    this.onDelete,
    this.enableDismissible = true,
    this.showDescription = true,
    this.showDueDate = true,
    this.showTrailing = true,
    this.margin = AppTheme.listTileScreenMargin,
    this.deleteTitle = 'Delete Task',
    this.deleteContent = 'Are you sure you want to delete this task?',
  });

  Color _getBorderColor() {
    if (task.isCompleted) return Colors.green;
    if (task.dueDate != null) {
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);
      final dueDay = DateTime(
        task.dueDate!.year,
        task.dueDate!.month,
        task.dueDate!.day,
      );
      if (dueDay.isBefore(today)) return Colors.red;
      if (dueDay.isAtSameMomentAs(today)) return Colors.amber.shade600;
    }
    return Colors.blue;
  }

  Widget _buildCard(BuildContext context) {
    final borderColor = _getBorderColor();

    return Card(
      margin: margin,
      elevation: 0,
      shape: RoundedRectangleBorder(
        side: BorderSide(
          color: borderColor,
          width: AppTheme.listTileBorderWidth,
        ),
        borderRadius: AppTheme.brMedium,
      ),
      child: ListTile(
        leading: Checkbox(
          value: task.isCompleted,
          activeColor: Colors.green,
          onChanged: (_) => onToggleCompleted(),
        ),
        title: Text(
          task.title,
          style: TextStyle(
            decoration: task.isCompleted ? TextDecoration.lineThrough : null,
            color: task.isCompleted ? Colors.grey : null,
            fontWeight: AppTheme.fwBold,
          ),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (showDescription && task.description.isNotEmpty)
              Text(
                task.description,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: AppTheme.fsLabel),
              ),
            if (showDueDate && task.dueDate != null)
              Padding(
                padding: const EdgeInsets.only(top: 4.0),
                child: Row(
                  children: [
                    Icon(Icons.access_time, size: 14, color: borderColor),
                    const SizedBox(width: 4),
                    Text(
                      '${task.dueDate!.month.toString().padLeft(2, '0')}/${task.dueDate!.day.toString().padLeft(2, '0')} ${task.dueDate!.hour.toString().padLeft(2, '0')}:${task.dueDate!.minute.toString().padLeft(2, '0')}',
                      style: TextStyle(
                        fontSize: AppTheme.fsLabel,
                        color: borderColor,
                        fontWeight: AppTheme.fwMedium,
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
        trailing: showTrailing
            ? Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    AppUtils().getSizeEmoji(task.size),
                    style: const TextStyle(fontSize: 18),
                  ),
                  const SizedBox(width: 8),
                  AppUtils().getPriorityIcon(task.priority),
                ],
              )
            : null,
        onTap: onTap,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final card = _buildCard(context);

    if (!enableDismissible || onDelete == null) return card;

    return Dismissible(
      key: ValueKey(task.id),
      background: Container(
        color: AppTheme.errorColor,
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: AppTheme.spaceLarge),
        margin: margin,
        child: const Icon(
          Icons.delete,
          color: Colors.white,
          size: AppTheme.listTileSwipeIconSize,
        ),
      ),
      direction: DismissDirection.endToStart,
      onDismissed: (_) => onDelete!(),
      confirmDismiss: (_) async {
        return await AppDialogs.showConfirmation(
              context: context,
              title: deleteTitle,
              content: deleteContent,
            ) ??
            false;
      },
      child: card,
    );
  }
}
