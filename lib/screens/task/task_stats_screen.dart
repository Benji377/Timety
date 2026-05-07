import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';
import '../../data/task/task.dart';
import '../../providers/task_provider.dart';

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

  // Helper to get the exact start and end of the currently focused week
  DateTime _getStartOfWeek(DateTime date) {
    return DateTime(
      date.year,
      date.month,
      date.day,
    ).subtract(Duration(days: date.weekday - 1));
  }

  // --- DATA PROCESSING HELPERS ---

  String _generateInsights(
    List<Task> tasks,
    DateTime startOfWeek,
    DateTime endOfWeek,
  ) {
    if (tasks.isEmpty) return "Add some tasks to see your insights!";

    int completedThisWeek = 0;
    int createdThisWeek = 0;

    for (var t in tasks) {
      if (t.createdAt.isAfter(
            startOfWeek.subtract(const Duration(seconds: 1)),
          ) &&
          t.createdAt.isBefore(endOfWeek)) {
        createdThisWeek++;
      }
      if (t.isCompleted &&
          t.completedAt != null &&
          t.completedAt!.isAfter(
            startOfWeek.subtract(const Duration(seconds: 1)),
          ) &&
          t.completedAt!.isBefore(endOfWeek)) {
        completedThisWeek++;
      }
    }

    // Check if the focused week is the CURRENT week in the real world
    bool isCurrentRealWeek =
        DateTime.now().isAfter(startOfWeek) &&
        DateTime.now().isBefore(endOfWeek);

    if (completedThisWeek == 0 && createdThisWeek == 0) {
      return isCurrentRealWeek
          ? "A quiet week so far. Time to plan ahead!"
          : "No activity recorded during this week.";
    } else if (completedThisWeek > createdThisWeek) {
      return "Excellent! You are clearing your backlog faster than you are adding to it.";
    } else if (completedThisWeek > 10) {
      return "Incredible focus! You crushed $completedThisWeek tasks this week.";
    } else if (completedThisWeek > 0) {
      return "Steady progress. You finished $completedThisWeek tasks this week.";
    } else {
      return "You added $createdThisWeek tasks this week, but haven't knocked any out yet. You've got this!";
    }
  }

  List<List<int>> _getVelocityForWeek(
    List<Task> tasks,
    DateTime startOfWeek,
    DateTime endOfWeek,
  ) {
    List<List<int>> dailyCounts = List.generate(7, (_) => [0, 0]);

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
    List<int> dailyCounts = List.filled(7, 0);

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
    Map<String, int> categoryCounts = {};
    for (var task in tasks) {
      String cat = task.category.isEmpty ? "Uncategorized" : task.category;
      categoryCounts[cat] = (categoryCounts[cat] ?? 0) + 1;
    }
    return categoryCounts;
  }

  @override
  Widget build(BuildContext context) {
    final tasks = context.watch<TaskProvider>().tasks;

    // Calculate week bounds
    final startOfWeek = _getStartOfWeek(_focusedDate);
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59, seconds: 59),
    );

    // Format the header string (e.g., "May 4 - May 10, 2026")
    String weekRangeLabel =
        "${DateFormat('MMM d').format(startOfWeek)} - ${DateFormat('MMM d, yyyy').format(endOfWeek)}";

    // Check if we are viewing the current real-world week
    bool isCurrentRealWeek =
        DateTime.now().isAfter(startOfWeek) &&
        DateTime.now().isBefore(endOfWeek);

    return Scaffold(
      body: tasks.isEmpty
          ? const Center(child: Text("No data to display yet."))
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // --- WEEK NAVIGATOR ---
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.chevron_left),
                      onPressed: () => _changeWeek(-7),
                    ),
                    Column(
                      children: [
                        Text(
                          isCurrentRealWeek ? "This Week" : "Past Week",
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                        Text(
                          weekRangeLabel,
                          style: const TextStyle(
                            color: Colors.grey,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                    IconButton(
                      icon: const Icon(Icons.chevron_right),
                      // Optionally disable going into the future
                      onPressed: isCurrentRealWeek
                          ? null
                          : () => _changeWeek(7),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // 1. DYNAMIC INSIGHTS CARD
                Card(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  elevation: 0,
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.auto_awesome,
                          size: 32,
                          color: Colors.amber,
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Text(
                            _generateInsights(tasks, startOfWeek, endOfWeek),
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 32),

                // 2. TASK VELOCITY CHART
                const Text(
                  "Task Velocity",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.circle, size: 12, color: Colors.orange.shade300),
                    const SizedBox(width: 4),
                    const Text(
                      "Created",
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                    const SizedBox(width: 16),
                    const Icon(Icons.circle, size: 12, color: Colors.green),
                    const SizedBox(width: 4),
                    const Text(
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

                // 3. PRODUCTIVITY BAR CHART
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

                // 4. CATEGORY PIE CHART (ALL TIME)
                const Text(
                  "All-Time Distribution",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),
                SizedBox(height: 200, child: _buildPieChart(context, tasks)),
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
        barTouchData: BarTouchData(enabled: false),
        titlesData: FlTitlesData(
          show: true,
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (double value, TitleMeta meta) {
                int dayIndex = value.toInt();
                // Only highlight the day if we are actually viewing the current week!
                bool isToday = isCurrentRealWeek && (dayIndex == todayIndex);

                return Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Text(
                    weekdays[dayIndex],
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                      color: isToday ? Colors.blue : Colors.grey,
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
          return BarChartGroupData(
            x: index,
            barsSpace: 4,
            barRods: [
              BarChartRodData(
                toY: velocityData[index][0].toDouble(),
                color: Colors.orange.shade300,
                width: 10,
                borderRadius: BorderRadius.circular(2),
              ),
              BarChartRodData(
                toY: velocityData[index][1].toDouble(),
                color: Colors.green,
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
        barTouchData: BarTouchData(enabled: false),
        titlesData: FlTitlesData(
          show: true,
          bottomTitles: AxisTitles(
            sideTitles: SideTitles(
              showTitles: true,
              getTitlesWidget: (double value, TitleMeta meta) {
                int dayIndex = value.toInt();
                // Only highlight the day if we are actually viewing the current week
                bool isToday = isCurrentRealWeek && (dayIndex == todayIndex);

                return Padding(
                  padding: const EdgeInsets.only(top: 8.0),
                  child: Text(
                    weekdays[dayIndex],
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                      color: isToday ? Colors.green : Colors.grey,
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
                color: isToday ? Colors.green : Colors.blue.shade300,
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

  Widget _buildPieChart(BuildContext context, List<Task> tasks) {
    final categoryData = _getCategoryData(tasks);
    if (categoryData.isEmpty) {
      return const Center(child: Text("No categories used."));
    }

    final colors = [
      Colors.blue,
      Colors.red,
      Colors.green,
      Colors.orange,
      Colors.purple,
      Colors.teal,
    ];
    int colorIndex = 0;

    List<PieChartSectionData> sections = categoryData.entries.map((entry) {
      final color = colors[colorIndex % colors.length];
      colorIndex++;

      return PieChartSectionData(
        color: color,
        value: entry.value.toDouble(),
        title: '${entry.key}\n(${entry.value})',
        radius: 60,
        titleStyle: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        ),
      );
    }).toList();

    return PieChart(
      PieChartData(sectionsSpace: 2, centerSpaceRadius: 40, sections: sections),
    );
  }
}
