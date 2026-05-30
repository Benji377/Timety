import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart' hide TextDirection;
import '../../theme/app_theme.dart';
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

  Map<String, IconData> get _distractionIcons => const {
    'Distracted': Icons.warning_amber,
    'Hydrated / Drink': Icons.water_drop,
    'Stretched': Icons.accessibility_new,
    'Snack': Icons.restaurant,
    'Restroom': Icons.wc,
  };

  String _resolveSessionTargetName(FocusSession session, List<FocusTag> tags) {
    final tagById = {for (final tag in tags) tag.id: tag};

    switch (session.targetType) {
      case FocusTargetType.task:
      case FocusTargetType.habit:
        return session.targetLabel?.trim().isNotEmpty == true
            ? session.targetLabel!.trim()
            : 'No Target';
      case FocusTargetType.tag:
        if (session.tagId != null) {
          return tagById[session.tagId!]?.name ??
              session.targetLabel ??
              'Untagged';
        }
        return session.targetLabel?.trim().isNotEmpty == true
            ? session.targetLabel!.trim()
            : 'Untagged';
    }
  }

  List<_DistractionEntry> _getDistractionEntries(
    List<FocusSession> sessions,
    List<FocusTag> tags,
  ) {
    final entries = <_DistractionEntry>[];
    for (final session in sessions) {
      final sessionTargetName = _resolveSessionTargetName(session, tags);

      for (final distraction in session.distractions) {
        entries.add(
          _DistractionEntry(
            distraction: distraction,
            targetName: sessionTargetName,
          ),
        );
      }
    }

    entries.sort((a, b) => b.distraction.time.compareTo(a.distraction.time));
    return entries;
  }

  IconData _iconForDistraction(String note) {
    return _distractionIcons[note] ?? Icons.warning_amber;
  }

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
    final List<int> dailyMins = List.filled(7, 0);
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

  List<_TargetTypeStat> _getTargetTypeStats(List<FocusSession> sessions) {
    final totals = <FocusTargetType, int>{
      FocusTargetType.tag: 0,
      FocusTargetType.task: 0,
      FocusTargetType.habit: 0,
    };

    for (final session in sessions) {
      totals[session.targetType] =
          (totals[session.targetType] ?? 0) +
          (session.totalSecondsFocused ~/ 60);
    }

    return [
      _TargetTypeStat(
        type: FocusTargetType.tag,
        label: 'Tags',
        color: Colors.green,
        icon: Icons.local_offer_outlined,
        minutes: totals[FocusTargetType.tag] ?? 0,
      ),
      _TargetTypeStat(
        type: FocusTargetType.task,
        label: 'Tasks',
        color: Colors.blue,
        icon: Icons.task_alt,
        minutes: totals[FocusTargetType.task] ?? 0,
      ),
      _TargetTypeStat(
        type: FocusTargetType.habit,
        label: 'Habits',
        color: Colors.purple,
        icon: Icons.favorite_outline,
        minutes: totals[FocusTargetType.habit] ?? 0,
      ),
    ];
  }

  String _formatFocusMinutes(int minutes) {
    if (minutes < 60) return '${minutes}m';
    final hours = minutes ~/ 60;
    final remainingMinutes = minutes % 60;
    if (remainingMinutes == 0) return '${hours}h';
    return '${hours}h ${remainingMinutes}m';
  }

  Widget _buildTargetBreakdownCard(List<FocusSession> sessions) {
    final stats = _getTargetTypeStats(sessions);
    final totalMinutes = stats.fold<int>(0, (sum, stat) => sum + stat.minutes);

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      clipBehavior: Clip.antiAlias,
      child: Container(
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
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Focus by target type',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Tagged sessions, task-linked sessions, and habit-linked sessions',
                        style: TextStyle(
                          fontSize: 12,
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                Text(
                  _formatFocusMinutes(totalMinutes),
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w900,
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
                child: totalMinutes == 0
                    ? Container(color: Colors.grey.withValues(alpha: 0.15))
                    : Row(
                        children: stats
                            .where((stat) => stat.minutes > 0)
                            .map(
                              (stat) => Expanded(
                                flex: stat.minutes,
                                child: Container(color: stat.color),
                              ),
                            )
                            .toList(),
                      ),
              ),
            ),
            const SizedBox(height: 18),
            ...stats.map(
              (stat) => Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Row(
                  children: [
                    Container(
                      width: 14,
                      height: 14,
                      decoration: BoxDecoration(
                        color: stat.color,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        stat.label,
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    Text(
                      _formatFocusMinutes(stat.minutes),
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Text(
                      totalMinutes == 0
                          ? '0%'
                          : '${((stat.minutes / totalMinutes) * 100).round()}%',
                      style: TextStyle(
                        fontSize: 12,
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final focusProvider = context.watch<FocusProvider>();
    final sessions = focusProvider.history;
    final distractionEntries = _getDistractionEntries(
      sessions,
      focusProvider.tags,
    );
    final selectedDayDistractions = distractionEntries
        .where(
          (entry) => AppDateUtils.isSameDay(
            entry.distraction.time,
            _selectedDayForClock,
          ),
        )
        .toList();

    final startOfWeek = AppDateUtils.startOfWeekMonday(_focusedWeek);
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59, seconds: 59),
    );
    final bool isCurrentRealWeek = AppDateUtils.isWithinInclusive(
      DateTime.now(),
      startOfWeek,
      endOfWeek,
    );

    // Filter sessions for the 24-hour clock
    final clockSessions = sessions
        .where((s) => AppDateUtils.isSameDay(s.startTime, _selectedDayForClock))
        .toList();
    final int clockTotalMins = clockSessions.fold(
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
                      final DateTime day = startOfWeek.add(
                        Duration(days: index),
                      );
                      final bool isSelected = AppDateUtils.isSameDay(
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
                                ? AppTheme.taskColor
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(
                              color: isSelected
                                  ? AppTheme.taskColor
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
                        color: AppTheme.taskColor,
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

                // --- DISTRACTIONS LIST ---
                const Text(
                  "Distractions",
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
                const Text(
                  "Selected day, newest distractions first",
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
                const SizedBox(height: 16),
                selectedDayDistractions.isEmpty
                    ? const Padding(
                        padding: EdgeInsets.symmetric(vertical: 24),
                        child: Center(
                          child: Text("No distractions logged for this day."),
                        ),
                      )
                    : ListView.separated(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        itemCount: selectedDayDistractions.length,
                        separatorBuilder: (context, index) =>
                            const Divider(height: 1),
                        itemBuilder: (context, index) {
                          final entry = selectedDayDistractions[index];
                          return ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: CircleAvatar(
                              backgroundColor: AppTheme.warningAccent
                                  .withValues(alpha: 0.12),
                              child: Icon(
                                _iconForDistraction(entry.distraction.note),
                                color: AppTheme.warningAccent,
                              ),
                            ),
                            title: Text(
                              entry.distraction.note.isEmpty
                                  ? 'Distraction'
                                  : entry.distraction.note,
                              style: const TextStyle(
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                            subtitle: Text(
                              '${DateFormat('hh:mm:ss').format(entry.distraction.time)} | ${entry.targetName}',
                            ),
                          );
                        },
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

                // --- TARGET BREAKDOWN ---
                _buildTargetBreakdownCard(sessions),
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
          final bool isToday = isCurrentRealWeek && (index == todayIndex);
          return BarChartGroupData(
            x: index,
            barRods: [
              BarChartRodData(
                toY: dailyMins[index].toDouble(),
                color: isToday
                    ? AppTheme.taskColor
                    : AppTheme.taskColor.withValues(alpha: 0.25),
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
}

class _TargetTypeStat {
  final FocusTargetType type;
  final String label;
  final Color color;
  final IconData icon;
  final int minutes;

  const _TargetTypeStat({
    required this.type,
    required this.label,
    required this.color,
    required this.icon,
    required this.minutes,
  });
}

class _DistractionEntry {
  final Distraction distraction;
  final String targetName;

  _DistractionEntry({required this.distraction, required this.targetName});
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
    const strokeWidth = 24.0;

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
      final double startHour =
          session.startTime.hour + (session.startTime.minute / 60.0);
      final double startAngle = (startHour / 24.0) * 2 * pi - (pi / 2);

      // Calculate end angle
      final DateTime end = session.endTime ?? DateTime.now();
      double endHour = end.hour + (end.minute / 60.0);

      // Handle sessions spanning past midnight (clamp at 24 for this view)
      if (!session.startTime.isAtSameMomentAs(end) &&
          end.day != session.startTime.day) {
        endHour = 24.0;
      }

      final double sweepAngle = ((endHour - startHour) / 24.0) * 2 * pi;
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
