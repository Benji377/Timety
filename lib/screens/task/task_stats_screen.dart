import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../theme/app_theme.dart';
import '../../data/task/task.dart';
import '../../providers/task_provider.dart';
import '../../utils/date_utils.dart';
import '../../widgets/week_navigator.dart';

class TaskStatsScreen extends StatefulWidget {
  const TaskStatsScreen({super.key});

  @override
  State<TaskStatsScreen> createState() => _TaskStatsScreenState();
}

class _TaskStatsScreenState extends State<TaskStatsScreen> {
  // This tracks which week we are currently viewing
  DateTime _focusedDate = DateTime.now();

  // Helper to jump weeks
  void _changeWeek(int days) {
    setState(() {
      _focusedDate = _focusedDate.add(Duration(days: days));
    });
  }

  List<List<int>> _getVelocityForWeek(
    List<Task> tasks,
    DateTime startOfWeek,
    DateTime endOfWeek,
  ) {
    final List<List<int>> dailyCounts = List.generate(7, (_) => [0, 0]);

    for (var task in tasks) {
      if (task.createdAt.isAfter(
            startOfWeek.subtract(const Duration(seconds: 1)),
          ) &&
          task.createdAt.isBefore(endOfWeek)) {
        dailyCounts[task.createdAt.weekday - 1][0]++;
      }
      if (task.isCompleted && task.completedAt != null) {
        if (task.completedAt!.isAfter(
              startOfWeek.subtract(const Duration(seconds: 1)),
            ) &&
            task.completedAt!.isBefore(endOfWeek)) {
          dailyCounts[task.completedAt!.weekday - 1][1]++;
        }
      }
    }
    return dailyCounts;
  }

  List<int> _getTasksCompletedForWeek(
    List<Task> tasks,
    DateTime startOfWeek,
    DateTime endOfWeek,
  ) {
    final List<int> dailyCounts = List.filled(7, 0);

    for (var task in tasks) {
      if (task.isCompleted && task.completedAt != null) {
        if (task.completedAt!.isAfter(
              startOfWeek.subtract(const Duration(seconds: 1)),
            ) &&
            task.completedAt!.isBefore(endOfWeek)) {
          dailyCounts[task.completedAt!.weekday - 1]++;
        }
      }
    }
    return dailyCounts;
  }

  // Category data remains "All Time" to show overall distribution
  Map<String, int> _getCategoryData(List<Task> tasks) {
    final Map<String, int> categoryCounts = {};
    for (var task in tasks) {
      final String cat = task.category.isEmpty
          ? "Uncategorized"
          : task.category;
      categoryCounts[cat] = (categoryCounts[cat] ?? 0) + 1;
    }
    return categoryCounts;
  }

  String _formatCount(int count) {
    return count == 1 ? '1 task' : '$count tasks';
  }

  Widget _buildCategoryBreakdownCard(BuildContext context, List<Task> tasks) {
    final categoryData = _getCategoryData(tasks);
    if (categoryData.isEmpty) {
      return const Center(child: Text("No categories used."));
    }

    final totalTasks = categoryData.values.fold<int>(
      0,
      (sum, value) => sum + value,
    );
    final entries = categoryData.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));
    final colors = [
      AppTheme.taskColor,
      AppTheme.errorColor,
      AppTheme.successColor,
      AppTheme.warningColor,
      AppTheme.habitColor,
      Theme.of(context).colorScheme.primary,
      Colors.teal,
      Colors.deepOrange,
    ];

    Color colorForIndex(int index) => colors[index % colors.length];

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      clipBehavior: Clip.antiAlias,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Theme.of(
                context,
              ).colorScheme.surfaceContainerHighest.withValues(alpha: 0.45),
              Theme.of(context).colorScheme.surface,
            ],
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'All-Time Distribution',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            Text(
              'Task categories across your whole workspace',
              style: TextStyle(
                fontSize: 12,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 18),
            ClipRRect(
              borderRadius: BorderRadius.circular(999),
              child: Container(
                height: 16,
                color: Theme.of(context).colorScheme.surfaceContainerHighest,
                child: Row(
                  children: entries.asMap().entries.map((entry) {
                    final index = entry.key;
                    final category = entry.value;
                    return Expanded(
                      flex: category.value,
                      child: Container(color: colorForIndex(index)),
                    );
                  }).toList(),
                ),
              ),
            ),
            const SizedBox(height: 18),
            ...entries.asMap().entries.map((entry) {
              final index = entry.key;
              final category = entry.value;
              final color = colorForIndex(index);
              final percent = ((category.value / totalTasks) * 100).round();

              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Row(
                  children: [
                    Container(
                      width: 14,
                      height: 14,
                      decoration: BoxDecoration(
                        color: color,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        category.key,
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Text(
                      _formatCount(category.value),
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Text(
                      '$percent%',
                      style: TextStyle(
                        fontSize: 12,
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
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

  @override
  Widget build(BuildContext context) {
    final tasks = context.watch<TaskProvider>().tasks;

    // Calculate week bounds
    final startOfWeek = AppDateUtils.startOfWeekMonday(_focusedDate);
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59, seconds: 59),
    );

    // Check if we are viewing the current real-world week
    final bool isCurrentRealWeek = AppDateUtils.isWithinInclusive(
      DateTime.now(),
      startOfWeek,
      endOfWeek,
    );

    return Scaffold(
      body: tasks.isEmpty
          ? const Center(child: Text("No data to display yet."))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // --- WEEK NAVIGATOR ---
                WeekNavigator(
                  focusedDate: _focusedDate,
                  onShiftWeek: _changeWeek,
                ),
                const SizedBox(height: 16),

                // TASK VELOCITY CHART
                const Text(
                  "Task Velocity",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Row(
                  children: [
                    Icon(Icons.circle, size: 12, color: AppTheme.warningColor),
                    SizedBox(width: 4),
                    Text(
                      "Created",
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                    SizedBox(width: 16),
                    Icon(Icons.circle, size: 12, color: AppTheme.successColor),
                    SizedBox(width: 4),
                    Text(
                      "Completed",
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 200,
                  child: _buildVelocityChart(
                    context,
                    tasks,
                    startOfWeek,
                    endOfWeek,
                    isCurrentRealWeek,
                  ),
                ),
                const SizedBox(height: 40),

                // PRODUCTIVITY BAR CHART
                const Text(
                  "Productivity",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                const Text(
                  "Total tasks finished per day",
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 200,
                  child: _buildProductivityChart(
                    context,
                    tasks,
                    startOfWeek,
                    endOfWeek,
                    isCurrentRealWeek,
                  ),
                ),
                const SizedBox(height: 40),

                // CATEGORY BREAKDOWN (ALL TIME)
                _buildCategoryBreakdownCard(context, tasks),
                const SizedBox(height: 40),
              ],
            ),
    );
  }

  // --- CHART BUILDERS ---
  Widget _buildVelocityChart(
    BuildContext context,
    List<Task> tasks,
    DateTime startOfWeek,
    DateTime endOfWeek,
    bool isCurrentRealWeek,
  ) {
    final velocityData = _getVelocityForWeek(tasks, startOfWeek, endOfWeek);
    final todayIndex = DateTime.now().weekday - 1;
    final weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    double maxY = 5;
    for (var day in velocityData) {
      if (day[0] > maxY) maxY = day[0].toDouble();
      if (day[1] > maxY) maxY = day[1].toDouble();
    }

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
                // Only highlight the day if we are actually viewing the current week!
                final bool isToday =
                    isCurrentRealWeek && (dayIndex == todayIndex);

                return Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Text(
                    weekdays[dayIndex],
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                      color: isToday
                          ? AppTheme.taskColor
                          : Theme.of(context).colorScheme.onSurfaceVariant,
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
          return BarChartGroupData(
            x: index,
            barsSpace: 4,
            barRods: [
              BarChartRodData(
                toY: velocityData[index][0].toDouble(),
                color: AppTheme.warningColor,
                width: 10,
                borderRadius: BorderRadius.circular(2),
              ),
              BarChartRodData(
                toY: velocityData[index][1].toDouble(),
                color: AppTheme.successColor,
                width: 10,
                borderRadius: BorderRadius.circular(2),
              ),
            ],
          );
        }),
      ),
    );
  }

  Widget _buildProductivityChart(
    BuildContext context,
    List<Task> tasks,
    DateTime startOfWeek,
    DateTime endOfWeek,
    bool isCurrentRealWeek,
  ) {
    final dailyCounts = _getTasksCompletedForWeek(
      tasks,
      startOfWeek,
      endOfWeek,
    );
    final todayIndex = DateTime.now().weekday - 1;
    final weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    double maxY = dailyCounts.reduce((a, b) => a > b ? a : b).toDouble();
    if (maxY < 5) maxY = 5;

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
                // Only highlight the day if we are actually viewing the current week
                final bool isToday =
                    isCurrentRealWeek && (dayIndex == todayIndex);

                return Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Text(
                    weekdays[dayIndex],
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                      color: isToday
                          ? AppTheme.successColor
                          : Theme.of(context).colorScheme.onSurfaceVariant,
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
                color: isToday ? AppTheme.successColor : AppTheme.taskColor,
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
}
