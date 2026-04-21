import 'package:flutter/material.dart';
import '../data/task.dart';
import '../theme/app_theme.dart';

class TaskCard extends StatelessWidget {
  final Task task;
  final VoidCallback onCheckedChange;
  final VoidCallback onClick;
  final VoidCallback? onLongClick;
  final bool isSelected;

  const TaskCard({
    super.key,
    required this.task,
    required this.onCheckedChange,
    required this.onClick,
    this.onLongClick,
    this.isSelected = false,
  });

  bool isDueToday(DateTime? dueDate) {
    if (dueDate == null) return false;
    final now = DateTime.now();
    return dueDate.year == now.year &&
        dueDate.month == now.month &&
        dueDate.day == now.day;
  }

  Color getBorderColor(BuildContext context) {
    final semantic = Theme.of(context).extension<TimetySemanticColors>()!;

    if (isSelected) return Theme.of(context).colorScheme.primary;
    if (task.status == TaskStatus.done) return semantic.success;
    if (task.status == TaskStatus.overdue) return semantic.warning;
    if (isDueToday(task.dueDateTime)) return semantic.info;
    return Theme.of(context).colorScheme.primary;
  }

  IconData getPriorityIcon(TaskPriority priority) {
    switch (priority) {
      case TaskPriority.urgent:
        return Icons.keyboard_double_arrow_up;
      case TaskPriority.high:
        return Icons.keyboard_arrow_up;
      case TaskPriority.medium:
        return Icons.remove;
      case TaskPriority.low:
        return Icons.keyboard_arrow_down;
    }
  }

  @override
  Widget build(BuildContext context) {
    final semantic = Theme.of(context).extension<TimetySemanticColors>()!;
    final borderColor = getBorderColor(context);
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      clipBehavior: Clip.antiAlias,
      elevation: isSelected ? 1 : 0,
      color: isSelected
          ? colorScheme.primaryContainer
          : colorScheme.surfaceContainerLow,
      child: InkWell(
        onTap: onClick,
        onLongPress: onLongClick,
        child: Container(
          decoration: BoxDecoration(
            border: Border.all(color: borderColor, width: isSelected ? 3 : 2),
            borderRadius: BorderRadius.circular(24),
          ),
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              if (isSelected)
                Icon(Icons.check_circle, color: semantic.focus)
              else
                Checkbox(
                  value: task.status == TaskStatus.done,
                  onChanged: (_) => onCheckedChange(),
                ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      task.title,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        decoration: task.status == TaskStatus.done
                            ? TextDecoration.lineThrough
                            : null,
                      ),
                    ),
                    if (task.description != null)
                      Text(
                        task.description!,
                        style: Theme.of(context).textTheme.bodySmall,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        _PriorityBadge(
                          icon: getPriorityIcon(task.priority),
                          label: task.priority.label,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          task.priority.label,
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                        const SizedBox(width: 12),
                        _TaskSizeBadge(sizeText: task.size.badgeText),
                        const SizedBox(width: 4),
                        Text(
                          '${task.size.estimatedMinutes}min',
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PriorityBadge extends StatelessWidget {
  final IconData icon;
  final String label;

  const _PriorityBadge({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: colorScheme.primaryContainer.withValues(alpha: 0.65),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Icon(icon, size: 14, color: colorScheme.onPrimaryContainer),
    );
  }
}

class _TaskSizeBadge extends StatelessWidget {
  final String sizeText;

  const _TaskSizeBadge({required this.sizeText});

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        sizeText,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: colorScheme.onSurfaceVariant,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
