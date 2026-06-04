import 'dart:math';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';


class GaugePainter extends CustomPainter {
  final double progress;
  final Color color;
  final bool isInteractive;
  final bool isDark;

  GaugePainter({
    required this.progress,
    required this.color,
    required this.isInteractive,
    required this.isDark,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = min(size.width, size.height) / 2;
    const strokeWidth = AppTheme.gaugeStrokeWidth;
    final innerRadius = radius - strokeWidth - 14;

    final innerBackgroundPaint = Paint()
      ..color = isDark ? AppTheme.gaugeBgDark : AppTheme.gaugeWhite
      ..style = PaintingStyle.fill;

    canvas.drawShadow(
      Path()..addOval(Rect.fromCircle(center: center, radius: innerRadius)),
      (isDark ? AppTheme.gaugeBorderDark : AppTheme.gaugeBorderLight)
          .withValues(alpha: 0.3),
      4.0,
      true,
    );

    canvas.drawCircle(center, innerRadius, innerBackgroundPaint);

    final innerBorderPaint = Paint()
      ..color = isDark ? AppTheme.gaugeBorderDark : AppTheme.gaugeBorderLight
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    canvas.drawCircle(center, innerRadius, innerBorderPaint);

    final trackPaint = Paint()
      ..color = isDark ? AppTheme.gaugeTrackDark : AppTheme.gaugeBorderLight
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;
    canvas.drawCircle(center, radius - strokeWidth / 2, trackPaint);

    final progressPaint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius - strokeWidth / 2),
      -pi / 2,
      2 * pi * progress,
      false,
      progressPaint,
    );

    if (isInteractive) {
      final thumbAngle = (-pi / 2) + (2 * pi * progress);
      final thumbCenter = Offset(
        center.dx + (radius - strokeWidth / 2) * cos(thumbAngle),
        center.dy + (radius - strokeWidth / 2) * sin(thumbAngle),
      );

      final shadowPaint = Paint()
        ..color = color.withValues(alpha: AppTheme.opacityLight)
        ..style = PaintingStyle.fill;
      canvas.drawCircle(thumbCenter, 20, shadowPaint);

      final thumbPaint = Paint()
        ..color = AppTheme.gaugeWhite
        ..style = PaintingStyle.fill;
      canvas.drawCircle(thumbCenter, 14, thumbPaint);

      final borderPaint = Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4;
      canvas.drawCircle(thumbCenter, 14, borderPaint);
    }
  }

  @override
  bool shouldRepaint(covariant GaugePainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.isInteractive != isInteractive ||
        oldDelegate.isDark != isDark;
  }
}