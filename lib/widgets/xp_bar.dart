import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class XPBar extends StatelessWidget {
  final int currentXp;
  final int maxXp;
  final int level;

  const XPBar({
    super.key,
    required this.currentXp,
    required this.maxXp,
    required this.level,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final semantic = theme.extension<TimetySemanticColors>()!;
    final progress = (currentXp / maxXp).clamp(0.0, 1.0);

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: theme.colorScheme.outlineVariant.withValues(alpha: 0.4),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Level $level', style: theme.textTheme.titleMedium),
              Text('$currentXp / $maxXp XP', style: theme.textTheme.bodySmall),
            ],
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 12,
              backgroundColor: theme.colorScheme.surfaceContainerHighest,
              valueColor: AlwaysStoppedAnimation<Color>(semantic.focus),
            ),
          ),
        ],
      ),
    );
  }
}
