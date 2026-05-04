import 'package:flutter/material.dart';
import '../data/focus_models.dart';

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
          border: isActive ? Border.all(color: Theme.of(context).colorScheme.primary, width: 3) : null,
          boxShadow: isActive 
              ? [BoxShadow(color: color.withValues(alpha: 0.4), blurRadius: 8, spreadRadius: 2)] 
              : [],
        ),
      );
    }

    // Helper to build the connecting line
    Widget buildLine(bool isPast) {
      return Container(
        width: 24,
        height: 3,
        color: isPast ? Colors.grey.shade400 : Colors.grey.shade200,
      );
    }

    // 1. Start Node
    nodes.add(buildDot(Colors.grey.shade500, currentPhaseIndex == 0 && !isCompleted && isRunning));

    // 2. Phase Nodes
    for (int i = 0; i < phases.length; i++) {
      nodes.add(buildLine(currentPhaseIndex > i));
      
      bool isActive = currentPhaseIndex == i && !isCompleted;
      Color dotColor = phases[i].type == PhaseType.focus ? Colors.green : Colors.orange;
      
      // Fade future nodes so the path forward is clear
      if (currentPhaseIndex < i) {
        dotColor = dotColor.withValues(alpha: 0.3);
      }

      nodes.add(buildDot(dotColor, isActive));
    }

    // 3. End Node
    nodes.add(buildLine(isCompleted));
    nodes.add(buildDot(Colors.grey.shade500, isCompleted));

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      physics: const BouncingScrollPhysics(),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 12.0),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: nodes,
        ),
      ),
    );
  }
}