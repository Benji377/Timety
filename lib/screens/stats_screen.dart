import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/stats_provider.dart';
import '../providers/user_provider.dart';
import '../data/category.dart';
import '../widgets/radial_graph.dart';
import '../widgets/xp_bar.dart';
import '../data/user.dart';
import 'daily_stats_screen.dart';

class StatsScreen extends StatelessWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final statsProvider = context.watch<StatsProvider>();
    final userProvider = context.watch<UserProvider>();
    final user = userProvider.user;

    final todayFocusTime = statsProvider.getSessionsForDay(DateTime.now()).fold(0, (sum, s) => sum + s.duration);
    final insights = statsProvider.getInsights();
    final weeklyData = statsProvider.getWeeklyFocusData();
    final distribution = statsProvider.getCategoryDistribution();

    return Scaffold(
      appBar: AppBar(title: const Text('Stats & Profile')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildProfileHeader(context, user),
          const SizedBox(height: 24),
          _buildTodayProgress(context, user, todayFocusTime),
          const SizedBox(height: 24),
          _buildWeeklyChart(context, weeklyData),
          const SizedBox(height: 24),
          if (insights.isNotEmpty) ...[
            Text('Insights', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            ...insights.map((insight) => Card(
              color: Theme.of(context).colorScheme.secondaryContainer,
              margin: const EdgeInsets.only(bottom: 8),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Text(insight, style: Theme.of(context).textTheme.bodyMedium),
              ),
            )),
          ],
          const SizedBox(height: 24),
          _buildCategoryDistribution(context, distribution, statsProvider),
        ],
      ),
    );
  }

  Widget _buildProfileHeader(BuildContext context, User? user) {
    final streak = user?.currentStreak ?? 0;
    String title = '🚀 Ready to Focus';
    if (streak >= 30) {
      title = '🔥 Focus Master';
    } else if (streak >= 14) {
      title = '⚡ Unstoppable';
    } else if (streak >= 7) {
      title = '🌟 On Fire';
    } else if (streak >= 3) {
      title = '💪 Building Momentum';
    } else if (streak > 0) {
      title = '🎯 Focused';
    }

    final xp = user?.xp ?? 0;
    final level = user?.level ?? 1;
    final xpInLevel = xp % 100;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(user?.name ?? 'Hero', style: Theme.of(context).textTheme.headlineSmall),
                      Text(title, style: TextStyle(color: Theme.of(context).colorScheme.secondary)),
                    ],
                  ),
                ),
                Icon(Icons.local_fire_department, color: Theme.of(context).colorScheme.error, size: 32),
                Text('$streak', style: Theme.of(context).textTheme.titleLarge),
              ],
            ),
            const SizedBox(height: 16),
            XPBar(currentXp: xpInLevel, maxXp: 100, level: level),
          ],
        ),
      ),
    );
  }

  Widget _buildTodayProgress(BuildContext context, User? user, int todayFocusTime) {
    final target = user?.dailyFocusTarget ?? 7200000;
    final progress = todayFocusTime / target;
    final hours = todayFocusTime ~/ 3600000;
    final minutes = (todayFocusTime % 3600000) ~/ 60000;

    return Column(
      children: [
        Text("Today's Progress", style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 16),
        GestureDetector(
          onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => DailyStatsScreen(initialDate: DateTime.now()))),
          child: RadialGraph(
            progress: progress.clamp(0.0, 1.0),
            text: '${hours}h ${minutes}m',
            size: 150,
          ),
        ),
      ],
    );
  }

  Widget _buildWeeklyChart(BuildContext context, Map<String, int> weeklyData) {
    final maxDuration = weeklyData.values.fold(1, (max, d) => d > max ? d : max);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("This Week's Focus", style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            ...weeklyData.entries.map((entry) {
              final barProgress = entry.value / maxDuration;
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(entry.key, style: Theme.of(context).textTheme.labelMedium),
                        Text('${entry.value ~/ 60000} min', style: Theme.of(context).textTheme.labelSmall),
                      ],
                    ),
                    const SizedBox(height: 4),
                    LinearProgressIndicator(
                      value: barProgress.clamp(0.0, 1.0),
                      minHeight: 8,
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ],
                ),
              );
            }),
          ],
        ),
      ),
    );
  }

  Widget _buildCategoryDistribution(BuildContext context, Map<int, int> distribution, StatsProvider provider) {
    if (distribution.isEmpty) return const SizedBox();
    final total = distribution.values.fold(0, (sum, d) => sum + d);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text("Category Distribution", style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 16),
        ...distribution.entries.map((entry) {
          final category = provider.categories.firstWhere(
            (c) => c.id == entry.key, 
            orElse: () => Category(name: 'Unknown', colorHex: '#808080', iconName: '')
          );
          final duration = entry.value;
          final catHours = duration ~/ 3600000;
          final catMinutes = (duration % 3600000) ~/ 60000;

          return Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(category.name),
                  Text('${catHours}h ${catMinutes}m'),
                ],
              ),
              const SizedBox(height: 4),
              LinearProgressIndicator(
                value: (duration / total).clamp(0.0, 1.0),
                color: Color(int.parse(category.colorHex.replaceAll('#', '0xFF'))),
                backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
              ),
              const SizedBox(height: 12),
            ],
          );
        }),
      ],
    );
  }
}
