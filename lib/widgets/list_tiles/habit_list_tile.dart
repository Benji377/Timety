import 'package:flutter/material.dart';
import '../../data/habit/habit_models.dart';
import '../../theme/app_theme.dart';
import '../app_dialogs.dart';

class HabitListTile extends StatelessWidget {
  final Habit habit;
  final bool isCompleted;
  final VoidCallback onToggleCompleted;
  final VoidCallback onTap;
  final VoidCallback? onDelete;
  final bool enableDismissible;
  final String subtitleText;
  final double? progressValue;
  final EdgeInsetsGeometry margin;
  final String deleteTitle;
  final String deleteContent;
  final bool isStacked;
  final bool isLocked;

  const HabitListTile({
    super.key,
    required this.habit,
    required this.isCompleted,
    required this.onToggleCompleted,
    required this.onTap,
    required this.subtitleText,
    this.onDelete,
    this.enableDismissible = true,
    this.progressValue,
    this.isStacked = false,
    this.isLocked = false,
    this.margin = AppTheme.listTileScreenMargin,
    this.deleteTitle = 'Delete Habit',
    this.deleteContent = 'Are you sure you want to delete this habit?',
  });

  Color get _color => Color(habit.colorValue);

  Widget _buildCard(BuildContext context) {
    final color = _color;

    final listTile = ListTile(
      leading: InkWell(
        onTap: isLocked
            ? () {
                // Better UX: Tell them WHY it's locked
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text(
                      'Complete the previous habit in the stack first!',
                    ),
                    duration: Duration(seconds: 2),
                  ),
                );
              }
            : onToggleCompleted,
        borderRadius: AppTheme.brCircle,
        child: Container(
          width: 28,
          height: 28,
          decoration: BoxDecoration(
            color: isCompleted
                ? color
                : (isLocked
                      ? Colors.grey.withValues(alpha: 0.1)
                      : Colors.transparent),
            shape: BoxShape.circle,
            border: Border.all(
              color: isLocked ? Colors.grey.withValues(alpha: 0.5) : color,
              width: 2,
            ),
          ),
          child: isCompleted
              ? const Icon(Icons.check, size: 18, color: Colors.white)
              : (isLocked
                    ? const Icon(
                        Icons.lock_outline,
                        size: 14,
                        color: Colors.grey,
                      )
                    : null),
        ),
      ),
      title: Row(
        children: [
          Icon(
            habit.iconData ?? Icons.circle,
            size: 18,
            color: isCompleted ? Colors.grey : (isLocked ? Colors.grey : color),
          ),
          const SizedBox(width: AppTheme.spaceSmall),
          Expanded(
            child: Text(
              habit.name,
              style: TextStyle(
                fontWeight: AppTheme.fwBold,
                decoration: isCompleted ? TextDecoration.lineThrough : null,
                color: isCompleted || isLocked
                    ? Colors.grey
                    : null,
              ),
            ),
          ),
        ],
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (habit.notes != null && habit.notes!.isNotEmpty)
            Text(
              habit.notes!,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: AppTheme.fsLabel),
            ),
          Text(
            subtitleText,
            style: const TextStyle(
              fontSize: AppTheme.fsLabel,
              color: Colors.grey,
            ),
          ),
          if (progressValue != null && !isCompleted)
            Padding(
              padding: const EdgeInsets.only(top: AppTheme.spaceXSmall),
              child: LinearProgressIndicator(
                value: progressValue!.clamp(0.0, 1.0),
                backgroundColor: color.withValues(
                  alpha: AppTheme.opacityVeryLight,
                ),
                color: color,
                borderRadius: AppTheme.brSmall,
              ),
            ),
        ],
      ),
      onTap: onTap,
    );

    if (isStacked) return listTile;

    return Card(
      margin: margin,
      elevation: 0,
      shape: RoundedRectangleBorder(
        side: BorderSide(
          color: isCompleted
              ? color.withValues(alpha: AppTheme.opacityLight)
              : color,
          width: AppTheme.listTileBorderWidth,
        ),
        borderRadius: AppTheme.brMedium,
      ),
      child: listTile,
    );
  }

  @override
  Widget build(BuildContext context) {
    final card = _buildCard(context);

    if (!enableDismissible || onDelete == null) return card;

    return Dismissible(
      key: ValueKey(habit.id),
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
