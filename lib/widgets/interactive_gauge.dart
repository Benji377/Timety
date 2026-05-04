import 'dart:math';
import 'package:flutter/material.dart';

class InteractiveGauge extends StatefulWidget {
  final int maxMinutes;
  final int initialMinutes;
  final bool isInteractive;
  final ValueChanged<int>? onChanged;
  
  final String label;
  final String centerText;
  final String bottomText; 
  final Color bottomTextColor;
  final VoidCallback? onBottomTextTapped;

  const InteractiveGauge({
    super.key,
    required this.maxMinutes,
    required this.initialMinutes,
    required this.centerText,
    required this.bottomText,
    required this.bottomTextColor,
    this.onBottomTextTapped,
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

    double angle = atan2(dy, dx);
    angle += pi / 2;
    if (angle < 0) angle += 2 * pi;

    setState(() {
      _progress = angle / (2 * pi);
      if (_progress < 0.02) _progress = 0.0;
      if (_progress > 0.98) _progress = 1.0;
    });

    if (widget.onChanged != null) {
      widget.onChanged!((_progress * widget.maxMinutes).round());
    }
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;

    return GestureDetector(
      // INCREASED SIZE
      onPanStart: (details) => _handlePan(details.localPosition, const Size(300, 300)),
      onPanUpdate: (details) => _handlePan(details.localPosition, const Size(300, 300)),
      child: SizedBox(
        width: 300,
        height: 300,
        child: CustomPaint(
          painter: _GaugePainter(
            progress: _progress,
            color: primaryColor,
            isInteractive: widget.isInteractive,
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const SizedBox(height: 20),
              Text(
                widget.label,
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1.5,
                  color: Colors.grey.shade600,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                widget.centerText,
                style: const TextStyle(fontSize: 60, fontWeight: FontWeight.w300),
              ),
              const SizedBox(height: 4),
              GestureDetector(
                onTap: widget.onBottomTextTapped,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                  decoration: BoxDecoration(
                    color: Colors.grey.shade100,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    "< ${widget.bottomText} >",
                    style: TextStyle(
                      color: widget.bottomTextColor, 
                      fontSize: 16, 
                      fontWeight: FontWeight.bold
                    ),
                  ),
                ),
              ),
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
    final strokeWidth = 16.0; // Slightly thicker
    final innerRadius = radius - strokeWidth - 14;

    final innerBackgroundPaint = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;
    
    canvas.drawShadow(Path()..addOval(Rect.fromCircle(center: center, radius: innerRadius)), Colors.black26, 4.0, true);
    canvas.drawCircle(center, innerRadius, innerBackgroundPaint);

    final innerBorderPaint = Paint()
      ..color = Colors.grey.shade300 
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2;
    canvas.drawCircle(center, innerRadius, innerBorderPaint);

    // DARKER EMPTY TRACK FOR BETTER VISIBILITY
    final trackPaint = Paint()
      ..color = Colors.grey.shade300
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
        ..color = color.withValues(alpha: 0.3)
        ..style = PaintingStyle.fill;
      canvas.drawCircle(thumbCenter, 20, shadowPaint);

      final thumbPaint = Paint()
        ..color = Colors.white
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
  bool shouldRepaint(covariant _GaugePainter oldDelegate) {
    return oldDelegate.progress != progress || oldDelegate.isInteractive != isInteractive;
  }
}