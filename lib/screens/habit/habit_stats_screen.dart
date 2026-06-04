import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../theme/app_theme.dart';
import '../../utils/date/date_utils.dart';
import '../../utils/logic/stats_utils.dart';
import '../../utils/logic/streak_utils.dart';
import '../../widgets/stat_cards/compact_vertical_stat_card_widget.dart';
import '../../widgets/common/week_navigator_widget.dart';

class HabitStatsScreen extends StatefulWidget {
  const HabitStatsScreen({super.key});

  @override
  State<HabitStatsScreen> createState() => _HabitStatsScreenState();
}

class _HabitStatsScreenState extends State<HabitStatsScreen> {
  DateTime _focusedDate = DateTime.now();

  @override
  Widget build(BuildContext context) {
    final habits = context.watch<HabitProvider>().habits;

    final startOfWeek = AppDateUtils.startOfWeekMonday(_focusedDate);
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59, seconds: 59),
    );
    final bool isCurrentRealWeek = AppDateUtils.isWithinInclusive(
      DateTime.now(),
      startOfWeek,
      endOfWeek,
    );

    // KPI Calculations
    final int totalCompletions = habits.fold(
      0,
      (sum, h) => sum + h.completions.length,
    );
    int allTimeBestStreak = 0;
    for (var h in habits) {
      final int s = StreakCalculator.calculateBestStreak(h.completions);
      if (s > allTimeBestStreak) allTimeBestStreak = s;
    }

    return Scaffold(
      body: habits.isEmpty
          ? const Center(
              child: Text("Create and complete habits to unlock insights!"),
            )
          : ListView(
              padding: AppTheme.paddingScreenVertical,
              children: [
                // --- WEEK NAVIGATOR ---
                WeekNavigator(
                  focusedDate: _focusedDate,
                  onShiftWeek: _changeWeek,
                ),
                const SizedBox(height: AppTheme.spaceLarge),

                // --- COMPACT KPIs ---
                Wrap(
                  alignment: WrapAlignment.center,
                  spacing: AppTheme.spaceMedium,
                  runSpacing: AppTheme.spaceMedium,
                  children: [
                    CompactVerticalStatCard(
                      title: "Total Logged",
                      value: "$totalCompletions",
                      icon: Icons.timeline,
                      color: AppTheme.habitColor,
                    ),
                    CompactVerticalStatCard(
                      title: "Active Habits",
                      value: "${habits.length}",
                      icon: Icons.all_inclusive,
                      color: AppTheme.successColor,
                    ),
                    CompactVerticalStatCard(
                      title: "Best Streak",
                      value: "$allTimeBestStreak",
                      icon: Icons.military_tech,
                      color: AppTheme.warningColor,
                    ),
                  ],
                ),
                const SizedBox(height: AppTheme.space3XLarge),

                // --- WEEKLY VOLUME CHART ---
                const Text(
                  "Weekly Velocity",
                  style: TextStyle(
                    fontSize: AppTheme.fsHeadingSmall,
                    fontWeight: AppTheme.fwBold,
                  ),
                ),
                const Text(
                  "Total habit completions per day",
                  style: TextStyle(
                    fontSize: AppTheme.fsBodySmall,
                    color: Colors.grey,
                  ),
                ),
                const SizedBox(height: AppTheme.spaceLarge),
                SizedBox(
                  height: 200,
                  child: _buildBarChart(
                    habits,
                    startOfWeek,
                    endOfWeek,
                    isCurrentRealWeek,
                  ),
                ),
                const SizedBox(height: AppTheme.space3XLarge),

                // --- TIME OF DAY BREAKDOWN ---
                _buildTimeOfDayBreakdownCard(habits),
                const SizedBox(height: AppTheme.space3XLarge),

                // --- LEADERBOARD ---
                const Text(
                  "Current Streaks",
                  style: TextStyle(
                    fontSize: AppTheme.fsHeadingSmall,
                    fontWeight: AppTheme.fwBold,
                  ),
                ),
                const SizedBox(height: AppTheme.spaceMedium),
                ...habits.map((habit) {
                  final int currentStreak =
                      StreakCalculator.calculateCurrentStreak(
                        habit.completions,
                      );
                  final int bestStreak = StreakCalculator.calculateBestStreak(
                    habit.completions,
                  );

                  return Card(
                    margin: const EdgeInsets.only(bottom: AppTheme.spaceSmall),
                    shape: const RoundedRectangleBorder(
                      side: BorderSide(
                        color: AppTheme.habitColor,
                        width: AppTheme.neoBorderWidth,
                      ),
                      borderRadius: AppTheme.brNeo,
                    ),
                    child: ListTile(
                      leading: const Icon(
                        Icons.stars,
                        color: AppTheme.habitColor,
                      ),
                      title: Text(
                        habit.name,
                        style: const TextStyle(fontWeight: AppTheme.fwBold),
                      ),
                      trailing: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                "$currentStreak",
                                style: const TextStyle(
                                  fontSize: 16,
                                  fontWeight: AppTheme.fwExtraBold,
                                  color: AppTheme.warningColor,
                                ),
                              ),
                              const SizedBox(width: 4),
                              const Icon(
                                Icons.whatshot,
                                size: 16,
                                color: AppTheme.warningColor,
                              ),
                            ],
                          ),
                          Text(
                            "Best: $bestStreak",
                            style: const TextStyle(
                              fontSize: 10,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
                const SizedBox(height: AppTheme.space3XLarge),
              ],
            ),
    );
  }

  // --- CHART BUILDERS ---
  Widget _buildBarChart(
    List<Habit> habits,
    DateTime startOfWeek,
    DateTime endOfWeek,
    bool isCurrentRealWeek,
  ) {
    final dailyCounts = _getCompletionsForWeek(habits, startOfWeek, endOfWeek);
    final todayIndex = DateTime.now().weekday - 1;
    final weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    final maxY = StatsUtils.maxValue(dailyCounts, minimum: 5);

    return BarChart(
      BarChartData(
        alignment: BarChartAlignment.spaceAround,
        maxY: maxY + 1,
        barTouchData: const BarTouchData(enabled: false),
        titlesData: FlTitlesData(
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (double value, TitleMeta meta) {
                final int dayIndex = value.toInt();
                final bool isToday =
                    isCurrentRealWeek && (dayIndex == todayIndex);
                return Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Text(
                    weekdays[dayIndex],
                    style: TextStyle(
                      fontSize: AppTheme.fsBodySmall,
                      fontWeight: isToday ? AppTheme.fwBold : AppTheme.fwNormal,
                      color: isToday ? AppTheme.habitColor : Colors.grey,
                    ),
                  ),
                );
              },
            ),
          ),
          leftTitles: const AxisTitles(),
          topTitles: const AxisTitles(),
          rightTitles: const AxisTitles(),
        ),
        gridData: const FlGridData(show: false),
        borderData: FlBorderData(show: false),
        barGroups: List.generate(7, (index) {
          final bool isToday = isCurrentRealWeek && (index == todayIndex);
          return BarChartGroupData(
            x: index,
            barRods: [
              BarChartRodData(
                toY: dailyCounts[index].toDouble(),
                color: isToday
                    ? AppTheme.habitColor
                    : AppTheme.habitColor.withValues(
                        alpha: AppTheme.opacityMedium,
                      ),
                width: 16,
                borderRadius: BorderRadius.circular(4),
                backDrawRodData: BackgroundBarChartRodData(
                  show: true,
                  toY: maxY + 1,
                  color: Colors.grey.withValues(alpha: 0.1),
                ),
              ),
            ],
          );
        }),
      ),
    );
  }

  void _changeWeek(int days) {
    setState(() {
      _focusedDate = _focusedDate.add(Duration(days: days));
    });
  }

  // --- DATA PROCESSING ---
  List<int> _getCompletionsForWeek(
    List<Habit> habits,
    DateTime startOfWeek,
    DateTime endOfWeek,
  ) {
    final List<int> dailyCounts = List.filled(7, 0);
    for (var habit in habits) {
      for (var c in habit.completions) {
        if (c.isAfter(startOfWeek.subtract(const Duration(seconds: 1))) &&
            c.isBefore(endOfWeek)) {
          dailyCounts[c.weekday - 1]++;
        }
      }
    }
    return dailyCounts;
  }

  Map<String, int> _getTimeOfDayData(List<Habit> habits) {
    int morning = 0; // 5 AM - 12 PM
    int afternoon = 0; // 12 PM - 5 PM
    int evening = 0; // 5 PM - 9 PM
    int night = 0; // 9 PM - 5 AM

    for (var habit in habits) {
      for (var c in habit.completions) {
        if (c.hour >= 5 && c.hour < 12) {
          morning++;
        } else if (c.hour >= 12 && c.hour < 17) {
          afternoon++;
        } else if (c.hour >= 17 && c.hour < 21) {
          evening++;
        } else {
          night++;
        }
      }
    }
    return {
      'Morning': morning,
      'Afternoon': afternoon,
      'Evening': evening,
      'Night': night,
    };
  }

  String _formatHabitCount(int count) {
    if (count == 1) return '1 completion';
    return '$count completions';
  }

  Widget _buildTimeOfDayBreakdownCard(List<Habit> habits) {
    final data = _getTimeOfDayData(habits);
    final total = data.values.fold<int>(0, (sum, value) => sum + value);

    final buckets = [
      TimeOfDayBucket(
        label: 'Morning',
        subtitle: '5 AM - 12 PM',
        count: data['Morning'] ?? 0,
        color: AppTheme.warningColor,
        icon: Icons.wb_sunny_outlined,
      ),
      TimeOfDayBucket(
        label: 'Afternoon',
        subtitle: '12 PM - 5 PM',
        count: data['Afternoon'] ?? 0,
        color: AppTheme.taskColor,
        icon: Icons.sunny,
      ),
      TimeOfDayBucket(
        label: 'Evening',
        subtitle: '5 PM - 9 PM',
        count: data['Evening'] ?? 0,
        color: AppTheme.habitColor,
        icon: Icons.nightlight_round,
      ),
      TimeOfDayBucket(
        label: 'Night',
        subtitle: '9 PM - 5 AM',
        count: data['Night'] ?? 0,
        color: AppTheme.userColor,
        icon: Icons.nights_stay_outlined,
      ),
    ];

    // Removed Card and inner background Container to unify layout styles
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'When do you build habits?',
                    style: TextStyle(
                      fontSize: AppTheme.fsHeadingSmall,
                      fontWeight: AppTheme.fwBold,
                    ),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Completion distribution by time of day',
                    style: TextStyle(
                      fontSize: AppTheme.fsBodySmall,
                      color: Colors.grey,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 16),
            Text(
              _formatHabitCount(total),
              style: const TextStyle(
                fontSize: 15,
                fontWeight: AppTheme.fwExtraBold,
              ),
            ),
          ],
        ),
        const SizedBox(height: 18),
        ClipRRect(
          borderRadius: BorderRadius.circular(999),
          child: Container(
            height: 16,
            color: Theme.of(context).colorScheme.surfaceContainerHighest,
            child: total == 0
                ? Container(color: Colors.grey.withValues(alpha: 0.12))
                : Row(
                    children: buckets
                        .where((bucket) => bucket.count > 0)
                        .map(
                          (bucket) => Expanded(
                            flex: bucket.count,
                            child: Container(color: bucket.color),
                          ),
                        )
                        .toList(),
                  ),
          ),
        ),
        const SizedBox(height: 18),
        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: buckets.map((bucket) {
            final percent = total == 0
                ? 0
                : ((bucket.count / total) * 100).round();
            return SizedBox(
              width: MediaQuery.of(context).size.width < 420
                  ? double.infinity
                  : (MediaQuery.of(context).size.width - 56) / 2,
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: bucket.color.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(18),
                  border: Border.all(
                    color: bucket.color.withValues(alpha: 0.22),
                  ),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 36,
                      height: 36,
                      decoration: BoxDecoration(
                        color: bucket.color.withValues(alpha: 0.14),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(bucket.icon, color: bucket.color, size: 20),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            bucket.label,
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: AppTheme.fwBold,
                              height: 1.2,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            bucket.subtitle,
                            style: TextStyle(
                              fontSize: 11,
                              color: Theme.of(
                                context,
                              ).colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          '${bucket.count}',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: AppTheme.fwExtraBold,
                          ),
                        ),
                        Text(
                          '$percent%',
                          style: TextStyle(
                            fontSize: 11,
                            color: Theme.of(
                              context,
                            ).colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }
}
