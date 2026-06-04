import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';

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
