import 'package:flutter/material.dart';
import '../../theme/app_theme.dart';
import '../../data/focus/focus_models.dart';

class ModeTimeline extends StatelessWidget {
  final List<SessionPhase> phases;
  final int currentPhaseIndex;
  final bool isRunning;
  final bool awaitingContinue;

  const ModeTimeline({
    super.key,
    required this.phases,
    required this.currentPhaseIndex,
    required this.isRunning,
    this.awaitingContinue = false,
  });

  @override
  Widget build(BuildContext context) {
    if (phases.isEmpty) return const SizedBox.shrink();

    final List<Widget> nodes = [];
    final bool isCompleted = currentPhaseIndex >= phases.length;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final completionFill = isDark
        ? AppTheme.paperLight
        : AppTheme.paperAltLight;
    final bool isRunningOrAwaiting = isRunning || awaitingContinue;

    // Helper to build a node (dot)
    Widget buildDot(Color color, bool isActive) {
      return Container(
        width: isActive ? 18 : 12,
        height: isActive ? 18 : 12,
        decoration: BoxDecoration(
          color: color,
          shape: BoxShape.circle,
          border: isActive
              ? Border.all(
                  color: Theme.of(context).colorScheme.primary,
                  width: 3,
                )
              : null,
          boxShadow: isActive
              ? [
                  BoxShadow(
                    color: color.withValues(alpha: AppTheme.opacityLight),
                    blurRadius: 8,
                    spreadRadius: 2,
                  ),
                ]
              : [],
        ),
      );
    }

    Widget buildCompletionNode(bool isActive) {
      return Container(
        width: isActive ? 22 : 16,
        height: isActive ? 22 : 16,
        decoration: BoxDecoration(
          color: completionFill,
          shape: BoxShape.circle,
          border: Border.all(
            color: Theme.of(context).colorScheme.outline,
            width: isActive ? 3 : 2,
          ),
          boxShadow: [
            BoxShadow(
              color: Theme.of(
                context,
              ).colorScheme.shadow.withValues(alpha: 0.12),
              blurRadius: 6,
              spreadRadius: 1,
            ),
          ],
        ),
      );
    }

    // Helper to build the connecting line
    Widget buildLine(bool isPast) {
      final lineColor = isDark ? AppTheme.borderDark : AppTheme.borderLight;
      return Container(
        width: 24,
        height: 3,
        // Only darken past lines if the timer is actually running or awaiting
        color: (isRunningOrAwaiting && isPast)
            ? lineColor.withValues(alpha: 0.6)
            : lineColor.withValues(alpha: 0.2),
      );
    }

    // Start Node
    nodes.add(
      buildCompletionNode(
        isRunningOrAwaiting && currentPhaseIndex == 0 && !isCompleted,
      ),
    );

    // Phase Nodes
    for (int i = 0; i < phases.length; i++) {
      nodes.add(buildLine(currentPhaseIndex > i));

      // ONLY set as active if the timer is running or awaiting continuation
      final bool isActive =
          isRunningOrAwaiting && currentPhaseIndex == i && !isCompleted;
      Color dotColor = phases[i].type == PhaseType.focus
          ? AppTheme.focusColor
          : AppTheme.warningColor;

      // ONLY fade future nodes if the timer is running or awaiting (otherwise show full preview)
      if (isRunningOrAwaiting && currentPhaseIndex < i) {
        dotColor = dotColor.withValues(alpha: 0.3);
      }

      nodes.add(buildDot(dotColor, isActive));
    }

    // End Node
    nodes.add(buildLine(isCompleted));
    nodes.add(buildCompletionNode(isRunningOrAwaiting && isCompleted));

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      physics: const BouncingScrollPhysics(),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spaceXLarge,
          vertical: AppTheme.spaceMedium,
        ),
        child: Row(mainAxisSize: MainAxisSize.min, children: nodes),
      ),
    );
  }
}
