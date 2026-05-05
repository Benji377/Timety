import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';

class OverviewStatsScreen extends StatelessWidget {
  const OverviewStatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final focusProvider = context.watch<FocusProvider>();

    // Basic KPIs for "Today"
    final now = DateTime.now();
    int tasksCompletedToday = taskProvider.tasks.where((t) => 
      t.isCompleted && t.completedAt != null && 
      t.completedAt!.year == now.year && t.completedAt!.month == now.month && t.completedAt!.day == now.day
    ).length;

    int focusMinsToday = focusProvider.getMinutesFocusedToday();
    int focusTarget = focusProvider.dailyTargetMinutes;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        const Text("Today's Summary", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 16),
        
        Row(
          children: [
            _buildKpiCard(context, "Tasks Done", "$tasksCompletedToday", Icons.task_alt, Colors.green),
            const SizedBox(width: 16),
            _buildKpiCard(context, "Focus Time", "${focusMinsToday}m", Icons.timer, Colors.blue),
          ],
        ),
        const SizedBox(height: 16),
        _buildKpiCard(
          context, 
          "Daily Focus Goal", 
          "${((focusMinsToday / focusTarget).clamp(0.0, 1.0) * 100).toInt()}%", 
          Icons.track_changes, 
          Colors.orange
        ),
        
        const SizedBox(height: 40),
        const Text("Productivity Synergy", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
        Card(
          elevation: 0,
          color: Theme.of(context).colorScheme.surfaceContainerHighest.withValues(alpha: 0.4),
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                Icon(Icons.balance, size: 48, color: Theme.of(context).colorScheme.primary),
                const SizedBox(height: 16),
                const Text(
                  "Overview charts coming soon!\nHere we will correlate your focus hours directly with your task completion rate.",
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey),
                )
              ],
            ),
          ),
        )
      ],
    );
  }

  Widget _buildKpiCard(BuildContext context, String title, String value, IconData icon, Color color) {
    return Expanded(
      child: Card(
        elevation: 0,
        shape: RoundedRectangleBorder(
          side: BorderSide(color: color.withValues(alpha: 0.3), width: 1),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: color),
              const SizedBox(height: 12),
              Text(value, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
              Text(title, style: const TextStyle(fontSize: 12, color: Colors.grey)),
            ],
          ),
        ),
      ),
    );
  }
}