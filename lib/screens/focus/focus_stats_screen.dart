import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart' hide TextDirection;
import '../../data/focus/focus_models.dart';
import '../../providers/focus_provider.dart';
import '../../utils/date_utils.dart';
import '../../widgets/week_navigator.dart';

class FocusStatsScreen extends StatefulWidget {
  const FocusStatsScreen({super.key});

  @override
  State<FocusStatsScreen> createState() => _FocusStatsScreenState();
}

class _FocusStatsScreenState extends State<FocusStatsScreen> {
  DateTime _focusedWeek = DateTime.now();
  DateTime _selectedDayForClock = DateTime.now();

  void _changeWeek(int days) {
    setState(() {
      _focusedWeek = _focusedWeek.add(Duration(days: days));
      // Snap the clock to the start of the newly viewed week
      _selectedDayForClock = AppDateUtils.startOfWeekMonday(_focusedWeek);
    });
  }

  // --- DATA PROCESSORS ---
  List<int> _getFocusMinutesForWeek(
    List<FocusSession> sessions,
    DateTime startOfWeek,
    DateTime endOfWeek,
  ) {
    List<int> dailyMins = List.filled(7, 0);
    for (var s in sessions) {
      if (s.startTime.isAfter(
            startOfWeek.subtract(const Duration(seconds: 1)),
          ) &&
          s.startTime.isBefore(endOfWeek)) {
        dailyMins[s.startTime.weekday - 1] += (s.totalSecondsFocused ~/ 60);
      }
    }
    return dailyMins;
  }

  Map<String, int> _getTagData(
    List<FocusSession> sessions,
    List<FocusTag> tags,
  ) {
    Map<String, int> data = {};
    for (var s in sessions) {
      String tagName = "Untagged";
      if (s.tagId != null) {
        final tag = tags.where((t) => t.id == s.tagId).firstOrNull;
        if (tag != null) tagName = tag.name;
      }
      data[tagName] = (data[tagName] ?? 0) + (s.totalSecondsFocused ~/ 60);
    }
    return data;
  }

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final sessions = focusProvider.history;

    final startOfWeek = AppDateUtils.startOfWeekMonday(_focusedWeek);
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59, seconds: 59),
    );
    bool isCurrentRealWeek = AppDateUtils.isWithinInclusive(
      DateTime.now(),
      startOfWeek,
      endOfWeek,
    );

    // Filter sessions for the 24-hour clock
    final clockSessions = sessions
        .where((s) => AppDateUtils.isSameDay(s.startTime, _selectedDayForClock))
        .toList();
    int clockTotalMins = clockSessions.fold(
      0,
      (sum, s) => sum + (s.totalSecondsFocused ~/ 60),
    );

    return Scaffold(
      body: sessions.isEmpty
          ? const Center(
              child: Text("Complete some focus sessions to see data!"),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // --- WEEK NAVIGATOR ---
                WeekNavigator(
                  focusedDate: _focusedWeek,
                  onShiftWeek: _changeWeek,
                ),
                const SizedBox(height: 24),

                // --- 24 HOUR CLOCK (CIRCULAR GAUGE) ---
                const Text(
                  "Daily Focus Rhythm",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),

                // Day Selector for Clock
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: List.generate(7, (index) {
                      DateTime day = startOfWeek.add(Duration(days: index));
                      bool isSelected = AppDateUtils.isSameDay(
                        day,
                        _selectedDayForClock,
                      );
                      return GestureDetector(
                        onTap: () => setState(() => _selectedDayForClock = day),
                        child: Container(
                          margin: const EdgeInsets.symmetric(horizontal: 4),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 8,
                          ),
                          decoration: BoxDecoration(
                            color: isSelected
                                ? Theme.of(context).colorScheme.primary
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(
                              color: isSelected
                                  ? Theme.of(context).colorScheme.primary
                                  : Colors.grey.shade300,
                            ),
                          ),
                          child: Text(
                            DateFormat('EEE d').format(day),
                            style: TextStyle(
                              color: isSelected
                                  ? Colors.white
                                  : Colors.grey.shade700,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      );
                    }),
                  ),
                ),
                const SizedBox(height: 32),

                // The Clock Painter
                Center(
                  child: SizedBox(
                    width: 250,
                    height: 250,
                    child: CustomPaint(
                      painter: _ClockPainter(
                        sessions: clockSessions,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                      child: Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              "${clockTotalMins ~/ 60}h ${clockTotalMins % 60}m",
                              style: const TextStyle(
                                fontSize: 28,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            Text(
                              "${clockSessions.length} sessions",
                              style: const TextStyle(color: Colors.grey),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 40),

                // --- WEEKLY BAR CHART ---
                const Text(
                  "Weekly Focus Volume",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const Text(
                  "Total focus minutes per day",
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 200,
                  child: _buildBarChart(
                    context,
                    sessions,
                    startOfWeek,
                    endOfWeek,
                    isCurrentRealWeek,
                  ),
                ),
                const SizedBox(height: 40),

                // --- TAG BREAKDOWN (PIE CHART) ---
                const Text(
                  "Time by Tags",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 200,
                  child: _buildTagPieChart(sessions, focusProvider.tags),
                ),
                const SizedBox(height: 40),
              ],
            ),
    );
  }

  Widget _buildBarChart(
    BuildContext context,
    List<FocusSession> sessions,
    DateTime startOfWeek,
    DateTime endOfWeek,
    bool isCurrentRealWeek,
  ) {
    final dailyMins = _getFocusMinutesForWeek(sessions, startOfWeek, endOfWeek);
    final todayIndex = DateTime.now().weekday - 1;
    final weekdays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    double maxY = dailyMins.reduce((a, b) => a > b ? a : b).toDouble();
    if (maxY < 60) maxY = 60; // Show at least 1 hour

    return BarChart(
      BarChartData(
        alignment: BarChartAlignment.spaceAround,
        maxY: maxY * 1.2,
        barTouchData: BarTouchData(
          enabled: true,
          touchTooltipData: BarTouchTooltipData(
            getTooltipItem: (group, groupIndex, rod, rodIndex) {
              return BarTooltipItem(
                "${rod.toY.toInt()} min",
                const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              );
            },
          ),
        ),
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
                      fontSize: 12,
                      fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                      color: isToday
                          ? Theme.of(context).colorScheme.primary
                          : Colors.grey,
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
                toY: dailyMins[index].toDouble(),
                color: isToday
                    ? Theme.of(context).colorScheme.primary
                    : Colors.blue.shade200,
                width: 16,
                borderRadius: BorderRadius.circular(4),
                backDrawRodData: BackgroundBarChartRodData(
                  show: true,
                  toY: maxY * 1.2,
                  color: Colors.grey.withValues(alpha: 0.1),
                ),
              ),
            ],
          );
        }),
      ),
    );
  }

  Widget _buildTagPieChart(List<FocusSession> sessions, List<FocusTag> tags) {
    final tagData = _getTagData(sessions, tags);
    if (tagData.isEmpty || tagData.values.every((v) => v == 0)) {
      return const Center(child: Text("No tagged focus time."));
    }

    List<PieChartSectionData> sections = [];

    tagData.forEach((name, mins) {
      if (mins > 0) {
        // Find color of the tag, default to grey
        Color color = Colors.grey;
        if (name != "Untagged") {
          final tag = tags.where((t) => t.name == name).firstOrNull;
          if (tag != null) color = Color(tag.colorValue);
        }

        sections.add(
          PieChartSectionData(
            color: color,
            value: mins.toDouble(),
            title: '$name\n${mins}m',
            radius: 60,
            titleStyle: const TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
        );
      }
    });

    return PieChart(
      PieChartData(sectionsSpace: 2, centerSpaceRadius: 40, sections: sections),
    );
  }
}

// --- CUSTOM 24-HOUR CLOCK PAINTER ---
class _ClockPainter extends CustomPainter {
  final List<FocusSession> sessions;
  final Color color;

  _ClockPainter({required this.sessions, required this.color});

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = min(size.width, size.height) / 2;
    final strokeWidth = 24.0;

    // Draw background track
    final trackPaint = Paint()
      ..color = Colors.grey.shade200
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;
    canvas.drawCircle(center, radius - (strokeWidth / 2), trackPaint);

    // Draw clock markers (0, 6, 12, 18)
    final textPainter = TextPainter(
      textAlign: TextAlign.center,
      textDirection: TextDirection.ltr,
    );
    void drawMarker(String text, double angle) {
      textPainter.text = TextSpan(
        text: text,
        style: const TextStyle(color: Colors.grey, fontSize: 10),
      );
      textPainter.layout();
      final r = radius - strokeWidth - 10;
      final x = center.dx + r * cos(angle) - textPainter.width / 2;
      final y = center.dy + r * sin(angle) - textPainter.height / 2;
      textPainter.paint(canvas, Offset(x, y));
    }

    drawMarker("0", -pi / 2); // Top
    drawMarker("6", 0); // Right
    drawMarker("12", pi / 2); // Bottom
    drawMarker("18", pi); // Left

    // Draw Sessions
    final sessionPaint = Paint()
      ..color = color.withValues(alpha: 0.8)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    for (var session in sessions) {
      // Calculate start angle (24 hours mapped to 2 * pi)
      double startHour =
          session.startTime.hour + (session.startTime.minute / 60.0);
      double startAngle = (startHour / 24.0) * 2 * pi - (pi / 2);

      // Calculate end angle
      DateTime end = session.endTime ?? DateTime.now();
      double endHour = end.hour + (end.minute / 60.0);

      // Handle sessions spanning past midnight (clamp at 24 for this view)
      if (!session.startTime.isAtSameMomentAs(end) &&
          end.day != session.startTime.day) {
        endHour = 24.0;
      }

      double sweepAngle = ((endHour - startHour) / 24.0) * 2 * pi;
      if (sweepAngle <= 0) continue; // Skip bad data

      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius - (strokeWidth / 2)),
        startAngle,
        sweepAngle,
        false,
        sessionPaint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _ClockPainter oldDelegate) => true;
}
