import 'dart:math';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/stats_provider.dart';
import '../providers/user_provider.dart';
import '../data/focus_session.dart';
import '../data/daily_event.dart';

class DailyStatsScreen extends StatefulWidget {
  final DateTime initialDate;
  const DailyStatsScreen({super.key, required this.initialDate});

  @override
  State<DailyStatsScreen> createState() => _DailyStatsScreenState();
}

class _DailyStatsScreenState extends State<DailyStatsScreen> {
  late DateTime _currentDate;

  @override
  void initState() {
    super.initState();
    _currentDate = widget.initialDate;
  }

  @override
  Widget build(BuildContext context) {
    final statsProvider = context.watch<StatsProvider>();
    final userProvider = context.watch<UserProvider>();
    final user = userProvider.user;

    final dailySessions = statsProvider.getSessionsForDay(_currentDate);
    final totalFocussedMillis = dailySessions.fold(0, (sum, s) => sum + s.duration);
    final dailyTargetMillis = user?.dailyFocusTarget ?? 7200000;
    final progress = totalFocussedMillis / dailyTargetMillis;

    return Scaffold(
      appBar: AppBar(
        title: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_back_ios, size: 16),
              onPressed: () => setState(() => _currentDate = _currentDate.subtract(const Duration(days: 1))),
            ),
            Text(DateFormat('EEEE, MMM d').format(_currentDate), style: const TextStyle(fontSize: 16)),
            IconButton(
              icon: const Icon(Icons.arrow_forward_ios, size: 16),
              onPressed: () => setState(() => _currentDate = _currentDate.add(const Duration(days: 1))),
            ),
          ],
        ),
      ),
      body: FutureBuilder<List<DailyEvent>>(
        future: statsProvider.getEventsForDay(_currentDate),
        builder: (context, snapshot) {
          final events = snapshot.data ?? [];
          final timelineItems = [...dailySessions, ...events]..sort((a, b) {
            final timeA = a is FocusSession ? a.startTime : (a as DailyEvent).timestamp;
            final timeB = b is FocusSession ? b.startTime : (b as DailyEvent).timestamp;
            return timeA.compareTo(timeB);
          });

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              _Circular24hGraph(sessions: dailySessions),
              const SizedBox(height: 32),
              Column(
                children: [
                  Text(
                    '${totalFocussedMillis ~/ 60000} / ${dailyTargetMillis ~/ 60000} mins',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  LinearProgressIndicator(
                    value: progress.clamp(0.0, 1.0),
                    minHeight: 8,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ],
              ),
              const Divider(height: 48),
              Text('Timeline', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 16),
              ...timelineItems.map((item) {
                if (item is FocusSession) {
                  return _TimelineSessionItem(session: item);
                } else {
                  return _TimelineEventItem(event: item as DailyEvent);
                }
              }),
            ],
          );
        },
      ),
    );
  }
}

class _Circular24hGraph extends StatelessWidget {
  final List<FocusSession> sessions;
  const _Circular24hGraph({required this.sessions});

  @override
  Widget build(BuildContext context) {
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
                primaryColor: Theme.of(context).colorScheme.primary,
                secondaryColor: Theme.of(context).colorScheme.surfaceContainerHighest,
              ),
            ),
          ),
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('24h', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold)),
              Text('Focus Distribution', style: Theme.of(context).textTheme.bodySmall),
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

  _Circular24hPainter({
    required this.sessions,
    required this.primaryColor,
    required this.secondaryColor,
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

    // Draw hour marks
    final markPaint = Paint()
      ..color = Colors.white.withValues(alpha: 0.5)
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

    // Draw sessions
    final sessionPaint = Paint()
      ..color = primaryColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    for (var session in sessions) {
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
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}

class _TimelineSessionItem extends StatelessWidget {
  final FocusSession session;
  const _TimelineSessionItem({required this.session});

  @override
  Widget build(BuildContext context) {
    final startStr = DateFormat('HH:mm').format(DateTime.fromMillisecondsSinceEpoch(session.startTime));
    final endStr = DateFormat('HH:mm').format(DateTime.fromMillisecondsSinceEpoch(session.endTime));

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 50,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(startStr, style: const TextStyle(fontWeight: FontWeight.bold)),
                Text(endStr, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Colors.grey)),
              ],
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Card(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Focus Session', style: TextStyle(fontWeight: FontWeight.bold)),
                    Text('${session.duration ~/ 60000} minutes', style: Theme.of(context).textTheme.bodySmall),
                    if (session.note != null && session.note!.isNotEmpty)
                      Text(session.note!, style: const TextStyle(fontStyle: FontStyle.italic, fontSize: 12)),
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
    final timeStr = DateFormat('HH:mm').format(DateTime.fromMillisecondsSinceEpoch(event.timestamp));

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          SizedBox(width: 50, child: Text(timeStr, style: const TextStyle(fontWeight: FontWeight.bold))),
          const SizedBox(width: 16),
          Expanded(
            child: Card(
              color: Theme.of(context).colorScheme.secondaryContainer,
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    const Icon(Icons.notifications, size: 16),
                    const SizedBox(width: 8),
                    Text(event.type, style: const TextStyle(fontWeight: FontWeight.bold)),
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
