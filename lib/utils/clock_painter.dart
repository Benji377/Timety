import 'dart:math';
import 'package:flutter/material.dart';
import '../../data/focus/focus_models.dart';

class ClockPainter extends CustomPainter {
  final List<FocusSession> sessions;
  final Color color;

  ClockPainter({required this.sessions, required this.color});

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
  bool shouldRepaint(covariant ClockPainter oldDelegate) => true;
}
