import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import '../l10n/app_localizations.dart';
import '../theme/app_theme.dart';
import '../data/task/task.dart';
import '../providers/task_provider.dart';
import '../providers/focus_provider.dart';
import '../providers/settings_provider.dart';
import '../utils/datetime/date_utils.dart';
import '../widgets/stats/kpi_stat_card.dart';

class OverviewStatsScreen extends StatelessWidget {
  const OverviewStatsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final taskProvider = context.watch<TaskProvider>();
    final focusProvider = context.watch<FocusProvider>();
    final settings = context.watch<SettingsProvider>();

    final l10n = AppLocalizations.of(context)!;

    final now = DateTime.now();
    final int tasksCompletedToday = _getTasksForDay(taskProvider.tasks, now);
    final int focusMinsToday = _getFocusForDay(focusProvider, now);
    final int focusTarget = settings.dailyGoalMins;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(
          l10n.statsLabelSummary,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 16),

        // --- KPI CARDS ---
        Row(
          children: [
            Expanded(
              child: KpiStatCard(
                title: l10n.statsLabelTasksDone,
                value: "$tasksCompletedToday",
                icon: Icons.task_alt,
                color: AppTheme.taskColor,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: KpiStatCard(
                title: l10n.statsLabelFocus,
                value: "${focusMinsToday}m",
                icon: Icons.timer,
                color: AppTheme.focusColor,
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        KpiStatCard(
          title: l10n.statsLabelFocusGoal,
          value:
              "${((focusMinsToday / focusTarget).clamp(0.0, 1.0) * 100).toInt()}%",
          icon: Icons.track_changes,
          color: AppTheme.warningColor,
        ),

        const SizedBox(height: 40),

        // --- SYNERGY HEADER ---
        Text(
          l10n.statsLabelProductivity,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 4),
        Text(
          l10n.statsLabelSynergy,
          style: const TextStyle(fontSize: 12, color: Colors.grey),
        ),
        const SizedBox(height: 16),

        // --- LEGEND ---
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.circle, size: 12, color: AppTheme.focusColor),
            const SizedBox(width: 4),
            Text(
              l10n.statsLabelFocusMins,
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(width: 24),
            const Icon(Icons.circle, size: 12, color: AppTheme.taskColor),
            const SizedBox(width: 4),
            Text(
              l10n.statsLabelTasksDone,
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
        const SizedBox(height: 24),

        // --- CHART ---
        SizedBox(
          height: 250,
          child: _buildSynergyChart(
            context,
            taskProvider,
            focusProvider,
            settings,
          ),
        ),
        const SizedBox(height: 40),
      ],
    );
  }

  // --- SYNERGY CHART BUILDER ---
  Widget _buildSynergyChart(
    BuildContext context,
    TaskProvider taskProvider,
    FocusProvider focusProvider,
    SettingsProvider settings,
  ) {
    final l10n = AppLocalizations.of(context)!;
    final now = DateTime.now();
    // Generate the last 7 days ending in today
    final last7Days = List.generate(
      7,
      (i) => now.subtract(Duration(days: 6 - i)),
    );

    // Extract actual real-world data
    final dailyFocus = last7Days
        .map((d) => _getFocusForDay(focusProvider, d))
        .toList();
    final dailyTasks = last7Days
        .map((d) => _getTasksForDay(taskProvider.tasks, d))
        .toList();

    // Normalization logic: find the highest value in both lists
    double maxFocus = dailyFocus.reduce(max).toDouble();
    if (maxFocus < 1) maxFocus = 1; // Prevent divide by zero

    double maxTasks = dailyTasks.reduce(max).toDouble();
    if (maxTasks < 1) maxTasks = 1;

    // Convert real data to a relative 0-10 scale so both lines are clearly visible
    final List<FlSpot> focusSpots = [];
    final List<FlSpot> taskSpots = [];
    for (int i = 0; i < 7; i++) {
      focusSpots.add(FlSpot(i.toDouble(), (dailyFocus[i] / maxFocus) * 10));
      taskSpots.add(FlSpot(i.toDouble(), (dailyTasks[i] / maxTasks) * 10));
    }

    return LineChart(
      LineChartData(
        minY: 0,
        maxY: 11, // Gives a little headroom above the max value of 10
        gridData: const FlGridData(show: false),
        borderData: FlBorderData(show: false),
        // Custom Tooltip to reverse the normalization and show real data
        lineTouchData: LineTouchData(
          touchTooltipData: LineTouchTooltipData(
            getTooltipItems: (touchedSpots) {
              return touchedSpots.map((LineBarSpot touchedSpot) {
                final dayIndex = touchedSpot.x.toInt();
                if (touchedSpot.barIndex == 0) {
                  return LineTooltipItem(
                    l10n.nMinutesCount(dailyFocus[dayIndex]),
                    const TextStyle(
                      color: AppTheme.focusColor,
                      fontWeight: FontWeight.bold,
                    ),
                  );
                } else {
                  return LineTooltipItem(
                    l10n.nTasksCount(dailyTasks[dayIndex]),
                    const TextStyle(
                      color: AppTheme.taskColor,
                      fontWeight: FontWeight.bold,
                    ),
                  );
                }
              }).toList();
            },
          ),
        ),
        titlesData: FlTitlesData(
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              reservedSize: 40,
              interval: 1,
              getTitlesWidget: (double value, TitleMeta meta) {
                final day = last7Days[value.toInt()];
                final isToday = value.toInt() == 6;
                return Padding(
                  padding: const EdgeInsets.only(top: 12.0),
                  child: Text(
                    settings.getFormattedWeekday(day),
                    style: TextStyle(
                      fontSize: 12,
                      color: isToday
                          ? Theme.of(context).colorScheme.primary
                          : Theme.of(context).colorScheme.onSurfaceVariant,
                      fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                    ),
                  ),
                );
              },
            ),
          ),
          leftTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              reservedSize: 30,
              interval: 5,
              getTitlesWidget: (double value, TitleMeta meta) {
                // Only draw text for our specific intervals
                if (value == 0 || value == 5 || value == 10) {
                  return Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: Text(
                      value.toInt().toString(),
                      style: const TextStyle(
                        fontSize: 12,
                        color: Colors.grey,
                        fontWeight: FontWeight.bold,
                      ),
                      textAlign: TextAlign.right,
                    ),
                  );
                }
                return const SizedBox.shrink();
              },
            ),
          ),
          rightTitles: const AxisTitles(),
          topTitles: const AxisTitles(),
        ),
        lineBarsData: [
          // FOCUS LINE
          LineChartBarData(
            spots: focusSpots,
            isCurved: true,
            preventCurveOverShooting: true,
            color: AppTheme.focusColor,
            barWidth: 3,
            isStrokeCapRound: true,
            belowBarData: BarAreaData(
              show: true,
              color: AppTheme.focusColor.withValues(alpha: 0.1),
            ),
          ),
          // TASK LINE
          LineChartBarData(
            spots: taskSpots,
            isCurved: true,
            preventCurveOverShooting: true,
            color: AppTheme.taskColor,
            barWidth: 3,
            isStrokeCapRound: true,
            belowBarData: BarAreaData(
              show: true,
              color: AppTheme.taskColor.withValues(alpha: 0.1),
            ),
          ),
        ],
      ),
    );
  }

  // --- DATA EXTRACTORS ---
  int _getTasksForDay(List<Task> tasks, DateTime day) {
    return tasks
        .where(
          (t) =>
              t.isCompleted &&
              t.completedAt != null &&
              AppDateUtils.isSameDay(t.completedAt!, day),
        )
        .length;
  }

  int _getFocusForDay(FocusProvider provider, DateTime day) {
    int totalSeconds = 0;
    for (var session in provider.history) {
      if (AppDateUtils.isSameDay(session.startTime, day)) {
        totalSeconds += session.totalSecondsFocused;
      }
    }
    // Include actively running session time if checking today
    if (AppDateUtils.isSameDay(DateTime.now(), day) && provider.isRunning) {
      totalSeconds += provider.currentSecondsFocussed;
    }
    return totalSeconds ~/ 60;
  }
}
