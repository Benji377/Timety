import 'package:flutter/material.dart';
import 'overview_stats_screen.dart';
import 'task/task_stats_screen.dart';
import 'focus/focus_stats_screen.dart';

class StatisticsScreen extends StatelessWidget {
  const StatisticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Insights & Statistics'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Overview'),
              Tab(text: 'Tasks'),
              Tab(text: 'Focus'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            OverviewStatsScreen(),
            TaskStatsScreen(),
            FocusStatsScreen(),
          ],
        ),
      ),
    );
  }
}