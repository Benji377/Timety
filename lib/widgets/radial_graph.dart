import 'dart:math';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class RadialGraph extends StatelessWidget {
  final double progress;
  final String text;
  final Color? color;
  final double size;

  const RadialGraph({
    super.key,
    required this.progress,
    required this.text,
    this.color,
    this.size = 200,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final semantic = theme.extension<TimetySemanticColors>()!;
    final themeColor = color ?? semantic.focus;

    return Center(
      child: Stack(
        alignment: Alignment.center,
        children: [
          SizedBox(
            width: size,
            height: size,
            child: CustomPaint(
              painter: _RadialPainter(
                progress: progress,
                color: themeColor,
                backgroundColor: theme.colorScheme.surfaceContainerHighest,
                strokeWidth: 14,
              ),
            ),
          ),
          Text(
            text,
            style: theme.textTheme.headlineMedium?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _RadialPainter extends CustomPainter {
  final double progress;
  final Color color;
  final Color backgroundColor;
  final double strokeWidth;

  _RadialPainter({
    required this.progress,
    required this.color,
    required this.backgroundColor,
    required this.strokeWidth,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = (size.shortestSide - strokeWidth) / 2;

    final backgroundPaint = Paint()
      ..color = backgroundColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;

    canvas.drawCircle(center, radius, backgroundPaint);

    final foregroundPaint = Paint()
      ..shader = SweepGradient(
        colors: [
          color.withValues(alpha: 0.9),
          color,
          color.withValues(alpha: 0.75),
        ],
        stops: const [0.0, 0.7, 1.0],
      ).createShader(Rect.fromCircle(center: center, radius: radius))
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius),
      -pi / 2,
      2 * pi * progress,
      false,
      foregroundPaint,
    );
  }

  @override
  bool shouldRepaint(covariant _RadialPainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.color != color ||
        oldDelegate.backgroundColor != backgroundColor ||
        oldDelegate.strokeWidth != strokeWidth;
  }
}
