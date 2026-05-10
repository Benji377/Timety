import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../data/focus/focus_models.dart';

class ModeTimeline extends StatelessWidget {
  final List<SessionPhase> phases;
  final int currentPhaseIndex;
  final bool isRunning;

  const ModeTimeline({
    super.key,
    required this.phases,
    required this.currentPhaseIndex,
    required this.isRunning,
  });

  @override
  Widget build(BuildContext context) {
    if (phases.isEmpty) return const SizedBox.shrink();

    List<Widget> nodes = [];
    bool isCompleted = currentPhaseIndex >= phases.length;

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

    // Helper to build the connecting line
    Widget buildLine(bool isPast) {
      return Container(
        width: 24,
        height: 3,
        // Only darken past lines if the timer is actually running
        color: (isRunning && isPast)
            ? AppTheme.borderLight.withValues(alpha: 0.6)
            : AppTheme.borderLight.withValues(alpha: 0.2),
      );
    }

    // 1. Start Node
    nodes.add(
      buildDot(
        AppTheme.borderLight.withValues(alpha: 0.7),
        isRunning && currentPhaseIndex == 0 && !isCompleted,
      ),
    );

    // 2. Phase Nodes
    for (int i = 0; i < phases.length; i++) {
      nodes.add(buildLine(currentPhaseIndex > i));

      // ONLY set as active if the timer is running
      bool isActive = isRunning && currentPhaseIndex == i && !isCompleted;
      Color dotColor = phases[i].type == PhaseType.focus
          ? AppTheme.focusColor
          : AppTheme.warningColor;

      // ONLY fade future nodes if the timer is running (otherwise show full preview)
      if (isRunning && currentPhaseIndex < i) {
        dotColor = dotColor.withValues(alpha: 0.3);
      }

      nodes.add(buildDot(dotColor, isActive));
    }

    // 3. End Node
    nodes.add(buildLine(isCompleted));
    nodes.add(
      buildDot(
        AppTheme.borderLight.withValues(alpha: 0.7),
        isRunning && isCompleted,
      ),
    );

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
