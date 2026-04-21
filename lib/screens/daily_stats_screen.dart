import 'dart:math';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../data/daily_event.dart';
import '../data/focus_session.dart';
import '../providers/stats_provider.dart';
import '../providers/user_provider.dart';
import '../theme/app_theme.dart';

class DailyStatsScreen extends StatefulWidget {
  final DateTime? initialDate;

  const DailyStatsScreen({super.key, this.initialDate});

  @override
  State<DailyStatsScreen> createState() => _DailyStatsScreenState();
}

class _DailyStatsScreenState extends State<DailyStatsScreen> {
  late DateTime _currentDate;

  @override
  void initState() {
    super.initState();
    _currentDate = widget.initialDate ?? DateTime.now();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final semantic = theme.extension<TimetySemanticColors>()!;
    final statsProvider = context.watch<StatsProvider>();
    final userProvider = context.watch<UserProvider>();
    final user = userProvider.user;

    final dailySessions = statsProvider.getSessionsForDay(_currentDate);
    final totalFocusedMillis = dailySessions.fold<int>(
      0,
      (sum, session) => sum + session.duration,
    );
    final dailyTargetMillis = user?.dailyFocusTarget ?? 7200000;
    final progress = totalFocusedMillis / dailyTargetMillis;

    return Scaffold(
      backgroundColor: theme.scaffoldBackgroundColor,
      body: SafeArea(
        child: FutureBuilder<List<DailyEvent>>(
          future: statsProvider.getEventsForDay(_currentDate),
          builder: (context, snapshot) {
            final events = snapshot.data ?? const <DailyEvent>[];
            final timelineItems = [...dailySessions, ...events]
              ..sort((a, b) {
                final timeA = a is FocusSession
                    ? a.startTime
                    : (a as DailyEvent).timestamp;
                final timeB = b is FocusSession
                    ? b.startTime
                    : (b as DailyEvent).timestamp;
                return timeA.compareTo(timeB);
              });

            return ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    IconButton(
                      onPressed: () => setState(
                        () => _currentDate = _currentDate.subtract(
                          const Duration(days: 1),
                        ),
                      ),
                      icon: const Icon(Icons.arrow_back),
                    ),
                    Column(
                      children: [
                        Text(
                          DateFormat('EEEE').format(_currentDate),
                          style: theme.textTheme.titleLarge,
                        ),
                        Text(
                          DateFormat('MMM d, yyyy').format(_currentDate),
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                    IconButton(
                      onPressed: () => setState(
                        () => _currentDate = _currentDate.add(
                          const Duration(days: 1),
                        ),
                      ),
                      icon: const Icon(Icons.arrow_forward),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                _Circular24hGraph(sessions: dailySessions),
                const SizedBox(height: 24),
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: theme.colorScheme.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(
                      color: theme.colorScheme.outlineVariant.withValues(
                        alpha: 0.4,
                      ),
                    ),
                  ),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text('Today', style: theme.textTheme.titleMedium),
                          Text(
                            '${totalFocusedMillis ~/ 60000} / ${dailyTargetMillis ~/ 60000} mins',
                            style: theme.textTheme.labelLarge?.copyWith(
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      ClipRRect(
                        borderRadius: BorderRadius.circular(999),
                        child: LinearProgressIndicator(
                          value: progress.clamp(0.0, 1.0),
                          minHeight: 10,
                          backgroundColor:
                              theme.colorScheme.surfaceContainerHighest,
                          valueColor: AlwaysStoppedAnimation<Color>(
                            semantic.focus,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const Divider(height: 40),
                Text('Timeline', style: theme.textTheme.titleMedium),
                const SizedBox(height: 16),
                if (timelineItems.isEmpty)
                  _EmptyState(theme: theme)
                else
                  ...timelineItems.map((item) {
                    if (item is FocusSession) {
                      return _TimelineSessionItem(session: item);
                    }
                    return _TimelineEventItem(event: item as DailyEvent);
                  }),
                const SizedBox(height: 24),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _Circular24hGraph extends StatelessWidget {
  final List<FocusSession> sessions;

  const _Circular24hGraph({required this.sessions});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final semantic = theme.extension<TimetySemanticColors>()!;

    return Center(
      child: Stack(
        alignment: Alignment.center,
        children: [
          SizedBox(
            width: 250,
            height: 250,
            child: CustomPaint(
              painter: _Circular24hPainter(
                sessions: sessions,
                primaryColor: semantic.focus,
                secondaryColor: theme.colorScheme.surfaceContainerHighest,
                labelColor: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                '24h',
                style: theme.textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
              Text(
                'Focus Distribution',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Circular24hPainter extends CustomPainter {
  final List<FocusSession> sessions;
  final Color primaryColor;
  final Color secondaryColor;
  final Color labelColor;

  _Circular24hPainter({
    required this.sessions,
    required this.primaryColor,
    required this.secondaryColor,
    required this.labelColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.shortestSide / 2;
    const strokeWidth = 20.0;

    final basePaint = Paint()
      ..color = secondaryColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;

    canvas.drawCircle(center, radius - strokeWidth / 2, basePaint);

    final markPaint = Paint()
      ..color = labelColor.withValues(alpha: 0.7)
      ..strokeWidth = 2;

    for (int i = 0; i < 24; i++) {
      final angle = (i * 15 - 90) * pi / 180;
      final inner = Offset(
        center.dx + (radius - strokeWidth) * cos(angle),
        center.dy + (radius - strokeWidth) * sin(angle),
      );
      final outer = Offset(
        center.dx + radius * cos(angle),
        center.dy + radius * sin(angle),
      );
      canvas.drawLine(inner, outer, markPaint);
    }

    final sessionPaint = Paint()
      ..color = primaryColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    for (final session in sessions) {
      final startDate = DateTime.fromMillisecondsSinceEpoch(session.startTime);
      final endDate = DateTime.fromMillisecondsSinceEpoch(session.endTime);

      final startHour = startDate.hour + startDate.minute / 60.0;
      final endHour = endDate.hour + endDate.minute / 60.0;

      final startAngle = (startHour * 15 - 90) * pi / 180;
      final sweepAngle = (endHour - startHour) * 15 * pi / 180;

      canvas.drawArc(
        Rect.fromCircle(center: center, radius: radius - strokeWidth / 2),
        startAngle,
        sweepAngle,
        false,
        sessionPaint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _Circular24hPainter oldDelegate) {
    return oldDelegate.sessions != sessions ||
        oldDelegate.primaryColor != primaryColor ||
        oldDelegate.secondaryColor != secondaryColor ||
        oldDelegate.labelColor != labelColor;
  }
}

class _TimelineSessionItem extends StatelessWidget {
  final FocusSession session;

  const _TimelineSessionItem({required this.session});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final startStr = DateFormat(
      'HH:mm',
    ).format(DateTime.fromMillisecondsSinceEpoch(session.startTime));
    final endStr = DateFormat(
      'HH:mm',
    ).format(DateTime.fromMillisecondsSinceEpoch(session.endTime));

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 58,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  startStr,
                  style: theme.textTheme.labelLarge?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Text(
                  endStr,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Card(
              color: theme.colorScheme.surfaceContainerLow,
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Focus Session',
                      style: theme.textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${session.duration ~/ 60000} minutes',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                    if (session.note != null && session.note!.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text(
                        session.note!,
                        style: theme.textTheme.bodySmall?.copyWith(
                          fontStyle: FontStyle.italic,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _TimelineEventItem extends StatelessWidget {
  final DailyEvent event;

  const _TimelineEventItem({required this.event});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final timeStr = DateFormat(
      'HH:mm',
    ).format(DateTime.fromMillisecondsSinceEpoch(event.timestamp));

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          SizedBox(
            width: 58,
            child: Text(
              timeStr,
              style: theme.textTheme.labelLarge?.copyWith(
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Card(
              color: theme.colorScheme.secondaryContainer,
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Row(
                  children: [
                    Icon(
                      Icons.notifications,
                      size: 16,
                      color: theme.colorScheme.onSecondaryContainer,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        event.type,
                        style: theme.textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                          color: theme.colorScheme.onSecondaryContainer,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final ThemeData theme;

  const _EmptyState({required this.theme});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 24),
      child: Center(
        child: Text(
          'No focus sessions or events today',
          style: theme.textTheme.bodyMedium?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}
