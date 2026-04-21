import 'package:flutter/material.dart';

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
    final progress = (currentXp / maxXp).clamp(0.0, 1.0);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Level $level', style: Theme.of(context).textTheme.titleMedium),
            Text('$currentXp / $maxXp XP', style: Theme.of(context).textTheme.bodySmall),
          ],
        ),
        const SizedBox(height: 8),
        LinearProgressIndicator(
          value: progress,
          minHeight: 12,
          borderRadius: BorderRadius.circular(6),
          backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
          valueColor: AlwaysStoppedAnimation<Color>(Theme.of(context).colorScheme.primary),
        ),
      ],
    );
  }
}
