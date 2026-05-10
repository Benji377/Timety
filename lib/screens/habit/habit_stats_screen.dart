import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../data/habit/habit_models.dart';
import '../../providers/habit_provider.dart';
import '../../theme/app_theme.dart';
import '../../utils/date_utils.dart';
import '../../utils/streak_calculator.dart';
import '../../widgets/stat_cards.dart';
import '../../widgets/week_navigator.dart';

class HabitStatsScreen extends StatefulWidget {
  const HabitStatsScreen({super.key});

  @override
  State<HabitStatsScreen> createState() => _HabitStatsScreenState();
}

class _HabitStatsScreenState extends State<HabitStatsScreen> {
  DateTime _focusedDate = DateTime.now();

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
    List<int> dailyCounts = List.filled(7, 0);
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

  @override
  Widget build(BuildContext context) {
    final habits = context.watch<HabitProvider>().habits;

    final startOfWeek = AppDateUtils.startOfWeekMonday(_focusedDate);
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59, seconds: 59),
    );
    bool isCurrentRealWeek = AppDateUtils.isWithinInclusive(
      DateTime.now(),
      startOfWeek,
      endOfWeek,
    );

    // KPI Calculations
    int totalCompletions = habits.fold(
      0,
      (sum, h) => sum + h.completions.length,
    );
    int allTimeBestStreak = 0;
    for (var h in habits) {
      int s = StreakCalculator.calculateBestStreak(h.completions);
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
                      color: AppTheme.infoColor,
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

                // --- TIME OF DAY PIE CHART ---
                const Text(
                  "When Do You Build Habits?",
                  style: TextStyle(
                    fontSize: AppTheme.fsHeadingSmall,
                    fontWeight: AppTheme.fwBold,
                  ),
                ),
                const Text(
                  "Completion distribution by time of day",
                  style: TextStyle(
                    fontSize: AppTheme.fsBodySmall,
                    color: Colors.grey,
                  ),
                ),
                const SizedBox(height: AppTheme.spaceLarge),
                SizedBox(height: 220, child: _buildTimePieChart(habits)),
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
                  int currentStreak = StreakCalculator.calculateCurrentStreak(
                    habit.completions,
                  );
                  int bestStreak = StreakCalculator.calculateBestStreak(
                    habit.completions,
                  );

                  return Card(
                    margin: const EdgeInsets.only(bottom: AppTheme.spaceSmall),
                    shape: RoundedRectangleBorder(
                      side: BorderSide(
                        color: AppTheme.habitColor,
                        width: AppTheme.neoBorderWidth,
                      ),
                      borderRadius: AppTheme.brNeo,
                    ),
                    child: ListTile(
                      leading: Icon(Icons.stars, color: AppTheme.habitColor),
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
                              Icon(
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

    double maxY = dailyCounts.reduce(max).toDouble();
    if (maxY < 5) maxY = 5;

    return BarChart(
      BarChartData(
        alignment: BarChartAlignment.spaceAround,
        maxY: maxY + 1,
        barTouchData: BarTouchData(enabled: false),
        titlesData: FlTitlesData(
          show: true,
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (double value, TitleMeta meta) {
                int dayIndex = value.toInt();
                bool isToday = isCurrentRealWeek && (dayIndex == todayIndex);
                return Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Text(
                    weekdays[dayIndex],
                    style: TextStyle(
                      fontSize: AppTheme.fsBodySmall,
                      fontWeight: isToday ? AppTheme.fwBold : AppTheme.fwNormal,
                      color: isToday ? AppTheme.infoColor : Colors.grey,
                    ),
                  ),
                );
              },
            ),
          ),
          leftTitles: const AxisTitles(
            sideTitles: SideTitles(showTitles: false),
          ),
          topTitles: const AxisTitles(
            sideTitles: SideTitles(showTitles: false),
          ),
          rightTitles: const AxisTitles(
            sideTitles: SideTitles(showTitles: false),
          ),
        ),
        gridData: const FlGridData(show: false),
        borderData: FlBorderData(show: false),
        barGroups: List.generate(7, (index) {
          bool isToday = isCurrentRealWeek && (index == todayIndex);
          return BarChartGroupData(
            x: index,
            barRods: [
              BarChartRodData(
                toY: dailyCounts[index].toDouble(),
                color: isToday
                    ? AppTheme.infoColor
                    : AppTheme.infoColor.withValues(
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

  Widget _buildTimePieChart(List<Habit> habits) {
    final data = _getTimeOfDayData(habits);
    if (data.values.every((v) => v == 0)) {
      return const Center(child: Text("No completion data yet."));
    }

    final colorMap = {
      'Morning': AppTheme.warningColor,
      'Afternoon': AppTheme.taskColor,
      'Evening': AppTheme.habitColor,
      'Night': AppTheme.userColor,
    };

    List<PieChartSectionData> sections = [];
    data.forEach((key, value) {
      if (value > 0) {
        sections.add(
          PieChartSectionData(
            color: colorMap[key],
            value: value.toDouble(),
            title: '$key\n($value)',
            radius: 70,
            titleStyle: const TextStyle(
              fontSize: 10,
              fontWeight: AppTheme.fwBold,
              color: Colors.white,
            ),
          ),
        );
      }
    });

    return PieChart(
      PieChartData(sectionsSpace: 2, centerSpaceRadius: 30, sections: sections),
    );
  }
}
