import 'package:flutter/material.dart';
import '../../data/habit/habit_models.dart';
import '../../theme/app_theme.dart';
import '../dialogs.dart';

class HabitListTile extends StatelessWidget {
  final Habit habit;
  final bool isCompleted;
  final VoidCallback onToggleCompleted;
  final VoidCallback onTap;
  final VoidCallback? onDelete;
  final VoidCallback? onMarkPastCompletion;
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
    this.onMarkPastCompletion,
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
    final habitIcon = habit.iconData ?? Icons.circle;

    final listTile = ListTile(
      leading: InkWell(
        onTap: isLocked
            ? () {
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
          width: 36,
          height: 36,
          decoration: BoxDecoration(
            color: isCompleted
                ? color
                : Theme.of(context).colorScheme.surfaceContainerHighest,
            shape: BoxShape.circle,
            border: Border.all(
              color: isCompleted
                  ? color
                  : (isLocked ? AppTheme.warningColor : AppTheme.habitColor),
              width: 2,
            ),
          ),
          child: isCompleted
              ? Icon(
                  Icons.check_rounded,
                  size: 22,
                  color: Theme.of(context).colorScheme.onPrimary,
                )
              : null,
        ),
      ),
      title: Row(
        children: [
          Icon(
            habitIcon,
            size: 18,
            color: isCompleted
                ? Theme.of(context).colorScheme.onSurfaceVariant
                : (isLocked ? AppTheme.wifiOffColor : AppTheme.habitColor),
          ),
          const SizedBox(width: AppTheme.spaceSmall),
          Expanded(
            child: Text(
              habit.name,
              style: TextStyle(
                fontWeight: AppTheme.fwBold,
                decoration: isCompleted ? TextDecoration.lineThrough : null,
                color: isCompleted || isLocked
                    ? Theme.of(context).colorScheme.onSurfaceVariant
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
            style: TextStyle(
              fontSize: AppTheme.fsLabel,
              color: Theme.of(context).colorScheme.onSurfaceVariant,
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
      trailing: IconButton(
          icon: const Icon(Icons.schedule, size: 20),
          tooltip: 'Mark past completion',
          onPressed: onMarkPastCompletion,
          visualDensity: VisualDensity.compact,
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
