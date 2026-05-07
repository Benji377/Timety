import 'package:flutter/material.dart';
import 'habit/habit_stats_screen.dart';
import 'overview_stats_screen.dart';
import 'task/task_stats_screen.dart';
import 'focus/focus_stats_screen.dart';

class StatisticsScreen extends StatelessWidget {
  const StatisticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 4,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Insights & Statistics'),
          // Use PreferredSize to heavily customize the tab bar area
          bottom: PreferredSize(
            preferredSize: const Size.fromHeight(64.0),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16.0, 8.0, 16.0, 12.0),
              child: Container(
                height: 44,
                decoration: BoxDecoration(
                  // A subtle background color to contrast against the AppBar
                  color: Theme.of(
                    context,
                  ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(25.0),
                ),
                child: TabBar(
                  // Removes the default grey line at the bottom
                  dividerColor: Colors.transparent,
                  // Makes the indicator fill the whole tab
                  indicatorSize: TabBarIndicatorSize.tab,
                  // The sliding pill-shaped indicator
                  indicator: BoxDecoration(
                    borderRadius: BorderRadius.circular(25.0),
                    color: Theme.of(context).colorScheme.primary,
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.1),
                        blurRadius: 4,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  // Text colors
                  labelColor: Theme.of(context).colorScheme.onPrimary,
                  unselectedLabelColor: Theme.of(
                    context,
                  ).colorScheme.onSurfaceVariant,
                  labelStyle: const TextStyle(fontWeight: FontWeight.bold),
                  tabs: const [
                    Tab(text: 'Overview'),
                    Tab(text: 'Tasks'),
                    Tab(text: 'Focus'),
                    Tab(text: 'Habits'),
                  ],
                ),
              ),
            ),
          ),
        ),
        body: const TabBarView(
          children: [
            OverviewStatsScreen(),
            TaskStatsScreen(),
            FocusStatsScreen(),
            HabitStatsScreen(),
          ],
        ),
      ),
    );
  }
}
