import 'dart:math';
import 'package:flutter/material.dart';

class InteractiveGauge extends StatefulWidget {
  final int maxMinutes;
  final int initialMinutes;
  final bool isInteractive;
  final ValueChanged<int>? onChanged;
  final String label;

  const InteractiveGauge({
    super.key,
    required this.maxMinutes,
    required this.initialMinutes,
    this.isInteractive = true,
    this.onChanged,
    required this.label,
  });

  @override
  State<InteractiveGauge> createState() => _InteractiveGaugeState();
}

class _InteractiveGaugeState extends State<InteractiveGauge> {
  late double _progress;

  @override
  void initState() {
    super.initState();
    // Prevent divide by zero if maxMinutes is 0 (like in Stopwatch)
    _progress = widget.maxMinutes > 0 ? widget.initialMinutes / widget.maxMinutes : 1.0;
  }

  @override
  void didUpdateWidget(covariant InteractiveGauge oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!widget.isInteractive) {
      _progress = widget.maxMinutes > 0 ? widget.initialMinutes / widget.maxMinutes : 1.0;
    }
  }

  void _handlePan(Offset localPosition, Size size) {
    if (!widget.isInteractive) return;

    final center = Offset(size.width / 2, size.height / 2);
    final dx = localPosition.dx - center.dx;
    final dy = localPosition.dy - center.dy;

    // Calculate angle using arctangent. atan2 returns -pi to pi.
    double angle = atan2(dy, dx);
    
    // Shift the angle so 0 starts at the top (-pi/2)
    angle += pi / 2;
    if (angle < 0) angle += 2 * pi; // Normalize to 0 -> 2*pi

    setState(() {
      _progress = angle / (2 * pi);
      
      // Add a slight snap to 0 and 100% to make it feel premium
      if (_progress < 0.02) _progress = 0.0;
      if (_progress > 0.98) _progress = 1.0;
    });

    if (widget.onChanged != null) {
      widget.onChanged!((_progress * widget.maxMinutes).round());
    }
  }

  String _formatTime(int minutes) {
    int h = minutes ~/ 60;
    int m = minutes % 60;
    if (h > 0) {
      return '${h}h ${m.toString().padLeft(2, '0')}m';
    }
    return '${m}m';
  }

  @override
  Widget build(BuildContext context) {
    final int currentMinutes = (_progress * widget.maxMinutes).round();
    final primaryColor = Theme.of(context).colorScheme.primary;

    return GestureDetector(
      onPanStart: (details) => _handlePan(details.localPosition, const Size(250, 250)),
      onPanUpdate: (details) => _handlePan(details.localPosition, const Size(250, 250)),
      child: SizedBox(
        width: 250,
        height: 250,
        child: CustomPaint(
          painter: _GaugePainter(
            progress: _progress,
            color: primaryColor,
            isInteractive: widget.isInteractive,
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                widget.label,
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.grey.shade600,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                widget.isInteractive ? _formatTime(currentMinutes) : widget.label == "STOPWATCH" ? "Counting..." : _formatTime(widget.initialMinutes),
                style: const TextStyle(fontSize: 56, fontWeight: FontWeight.w300),
              ),
              if (widget.isInteractive)
                const Text("Drag to set time", style: TextStyle(color: Colors.grey, fontSize: 12)),
            ],
          ),
        ),
      ),
    );
  }
}

class _GaugePainter extends CustomPainter {
  final double progress;
  final Color color;
  final bool isInteractive;

  _GaugePainter({required this.progress, required this.color, required this.isInteractive});

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = min(size.width, size.height) / 2;
    final strokeWidth = 16.0;

    // 1. Draw Background Track
    final trackPaint = Paint()
      ..color = Colors.grey.shade200
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;
    canvas.drawCircle(center, radius - strokeWidth, trackPaint);

    // 2. Draw Progress Arc
    final progressPaint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;
    
    // Start at -90 degrees (top), sweep based on progress
    canvas.drawArc(
      Rect.fromCircle(center: center, radius: radius - strokeWidth),
      -pi / 2,
      2 * pi * progress,
      false,
      progressPaint,
    );

    // 3. Draw the Draggable Thumb (Only if interactive)
    if (isInteractive) {
      // Calculate thumb position
      final thumbAngle = (-pi / 2) + (2 * pi * progress);
      final thumbCenter = Offset(
        center.dx + (radius - strokeWidth) * cos(thumbAngle),
        center.dy + (radius - strokeWidth) * sin(thumbAngle),
      );

      // Draw Thumb Shadow/Glow
      final shadowPaint = Paint()
        ..color = color.withValues(alpha: 0.3)
        ..style = PaintingStyle.fill;
      canvas.drawCircle(thumbCenter, 16, shadowPaint);

      // Draw Thumb Core
      final thumbPaint = Paint()
        ..color = Colors.white
        ..style = PaintingStyle.fill;
      canvas.drawCircle(thumbCenter, 12, thumbPaint);
      
      // Draw Thumb Border
      final borderPaint = Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4;
      canvas.drawCircle(thumbCenter, 12, borderPaint);
    }
  }

  @override
  bool shouldRepaint(covariant _GaugePainter oldDelegate) {
    return oldDelegate.progress != progress || oldDelegate.isInteractive != isInteractive;
  }
}