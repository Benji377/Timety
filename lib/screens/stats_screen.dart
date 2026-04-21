import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/stats_provider.dart';
import '../providers/user_provider.dart';
import '../data/category.dart';
import '../widgets/radial_graph.dart';
import '../widgets/xp_bar.dart';
import '../data/user.dart';
import '../theme/app_theme.dart';
import 'daily_stats_screen.dart';

class StatsScreen extends StatelessWidget {
  const StatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final statsProvider = context.watch<StatsProvider>();
    final userProvider = context.watch<UserProvider>();
    final user = userProvider.user;
    final theme = Theme.of(context);

    final todayFocusTime = statsProvider
        .getSessionsForDay(DateTime.now())
        .fold(0, (sum, s) => sum + s.duration);
    final insights = statsProvider.getInsights();
    final weeklyData = statsProvider.getWeeklyFocusData();
    final distribution = statsProvider.getCategoryDistribution();

    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text('Profile & Stats', style: theme.textTheme.headlineSmall),
            const SizedBox(height: 24),
            _buildProfileHeader(context, user),
            const SizedBox(height: 24),
            _buildTodayProgress(context, user, todayFocusTime),
            const SizedBox(height: 24),
            _buildWeeklyChart(context, weeklyData),
            const SizedBox(height: 24),
            if (insights.isNotEmpty) ...[
              Text('Insights', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 12),
              ...insights.map(
                (insight) => Card(
                  color: theme.colorScheme.secondaryContainer,
                  margin: const EdgeInsets.only(bottom: 8),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Text(insight, style: theme.textTheme.bodyMedium),
                  ),
                ),
              ),
              const SizedBox(height: 24),
            ],
            _buildCategoryDistribution(context, distribution, statsProvider),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileHeader(BuildContext context, User? user) {
    if (user == null) return const SizedBox();

    final semantic = Theme.of(context).extension<TimetySemanticColors>()!;
    final streak = user.currentStreak;
    final title = user.userTitle;
    final emoji = user.levelEmoji;
    final xpInLevel = user.xp;

    return Card(
      color: Theme.of(context).colorScheme.surfaceContainerLow,
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
                      Text(
                        user.name,
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '$emoji $title',
                        style: TextStyle(
                          color: semantic.focus,
                          fontSize: Theme.of(
                            context,
                          ).textTheme.bodyMedium?.fontSize,
                        ),
                      ),
                    ],
                  ),
                ),
                Icon(
                  Icons.local_fire_department,
                  color: semantic.warning,
                  size: 32,
                ),
                const SizedBox(width: 8),
                Text('$streak', style: Theme.of(context).textTheme.titleLarge),
              ],
            ),
            const SizedBox(height: 16),
            XPBar(currentXp: xpInLevel, maxXp: 2000, level: user.level),
          ],
        ),
      ),
    );
  }

  Widget _buildTodayProgress(
    BuildContext context,
    User? user,
    int todayFocusTime,
  ) {
    final semantic = Theme.of(context).extension<TimetySemanticColors>()!;
    final target = user?.dailyFocusTarget ?? 7200000;
    final progress = todayFocusTime / target;
    final hours = todayFocusTime ~/ 3600000;
    final minutes = (todayFocusTime % 3600000) ~/ 60000;

    return Column(
      children: [
        Text("Today's Progress", style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 16),
        GestureDetector(
          onTap: () => Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => DailyStatsScreen(initialDate: DateTime.now()),
            ),
          ),
          child: RadialGraph(
            progress: progress.clamp(0.0, 1.0),
            text: '${hours}h ${minutes}m',
            size: 150,
            color: semantic.focus,
          ),
        ),
      ],
    );
  }

  Widget _buildWeeklyChart(BuildContext context, Map<String, int> weeklyData) {
    final semantic = Theme.of(context).extension<TimetySemanticColors>()!;
    final maxDuration = weeklyData.values.fold(
      1,
      (max, d) => d > max ? d : max,
    );

    return Card(
      color: Theme.of(context).colorScheme.surfaceContainerLow,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              "This Week's Focus",
              style: Theme.of(context).textTheme.titleMedium,
            ),
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
                        Text(
                          entry.key,
                          style: Theme.of(context).textTheme.labelMedium,
                        ),
                        Text(
                          '${entry.value ~/ 60000} min',
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    LinearProgressIndicator(
                      value: barProgress.clamp(0.0, 1.0),
                      minHeight: 8,
                      borderRadius: BorderRadius.circular(4),
                      color: semantic.focus,
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

  Widget _buildCategoryDistribution(
    BuildContext context,
    Map<int, int> distribution,
    StatsProvider provider,
  ) {
    if (distribution.isEmpty) return const SizedBox();
    final total = distribution.values.fold(0, (sum, d) => sum + d);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          "Category Distribution",
          style: Theme.of(context).textTheme.titleLarge,
        ),
        const SizedBox(height: 16),
        ...distribution.entries.map((entry) {
          final category = provider.categories.firstWhere(
            (c) => c.id == entry.key,
            orElse: () =>
                Category(name: 'Unknown', colorHex: '#808080', iconName: ''),
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
                color: Color(
                  int.parse(category.colorHex.replaceAll('#', '0xFF')),
                ),
                backgroundColor: Theme.of(
                  context,
                ).colorScheme.surfaceContainerHighest,
              ),
              const SizedBox(height: 12),
            ],
          );
        }),
      ],
    );
  }
}
