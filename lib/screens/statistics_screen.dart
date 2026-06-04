import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import './habit/habit_stats_screen.dart';
import './overview_stats_screen.dart';
import './task/task_stats_screen.dart';
import './focus/focus_stats_screen.dart';

class StatisticsScreen extends StatefulWidget {
  final int initialTabIndex;

  const StatisticsScreen({super.key, this.initialTabIndex = 0});

  @override
  State<StatisticsScreen> createState() => _StatisticsScreenState();
}

class _StatisticsScreenState extends State<StatisticsScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  int _currentTabIndex = 0;

  @override
  void initState() {
    super.initState();
    _currentTabIndex = widget.initialTabIndex;
    _tabController = TabController(
      length: 4,
      vsync: this,
      initialIndex: _currentTabIndex,
    );
    _tabController.addListener(_handleTabSelection);
  }

  void _handleTabSelection() {
    if (_tabController.index != _currentTabIndex) {
      setState(() {
        _currentTabIndex = _tabController.index;
      });
    }
  }

  @override
  void dispose() {
    _tabController.removeListener(_handleTabSelection);
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final activeColor = _getSignatureColor(_currentTabIndex);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Insights & Statistics'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(64.0),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16.0, 8.0, 16.0, 12.0),
            child: Container(
              height: 44,
              decoration: BoxDecoration(
                color: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                borderRadius: BorderRadius.circular(25.0),
              ),
              child: TabBar(
                controller: _tabController,
                dividerColor: Colors.transparent,
                indicatorSize: TabBarIndicatorSize.tab,
                indicator: BoxDecoration(
                  borderRadius: BorderRadius.circular(25.0),
                  color: activeColor,
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.1),
                      blurRadius: 4,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
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
      body: TabBarView(
        controller: _tabController,
        children: const [
          OverviewStatsScreen(),
          TaskStatsScreen(),
          FocusStatsScreen(),
          HabitStatsScreen(),
        ],
      ),
    );
  }

  Color _getSignatureColor(int index) {
    switch (index) {
      case 0:
        return AppTheme.warningColor;
      case 1:
        return AppTheme.taskColor;
      case 2:
        return AppTheme.focusColor;
      case 3:
        return AppTheme.habitColor;
      default:
        return Theme.of(context).colorScheme.primary;
    }
  }
}
