import 'dart:math';
import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class InteractiveGauge extends StatefulWidget {
  final double progress;
  final bool isInteractive;
  final bool isStopwatch; // Triggers the pulse animation
  final ValueChanged<double>? onChanged; // Returns progress 0.0 to 1.0

  final String label;
  final String centerText;
  final Color? centerTextColor;
  final Color? labelColor;
  final String bottomText;
  final Color bottomTextColor;
  final VoidCallback? onBottomTextTapped;
  final Color? color; // optional override for gauge color

  const InteractiveGauge({
    super.key,
    required this.progress,
    this.isStopwatch = false,
    required this.centerText,
    required this.bottomText,
    required this.bottomTextColor,
    this.onBottomTextTapped,
    this.isInteractive = true,
    this.onChanged,
    this.color,
    this.centerTextColor,
    this.labelColor,
    required this.label,
  });

  @override
  State<InteractiveGauge> createState() => _InteractiveGaugeState();
}

class _InteractiveGaugeState extends State<InteractiveGauge>
    with SingleTickerProviderStateMixin {
  late double _progress;
  late AnimationController _pulseController;

  @override
  void initState() {
    super.initState();
    _progress = widget.progress;

    // The Stopwatch Pulse Animation
    _pulseController = AnimationController(
      vsync: this,
      duration: AppTheme.pulseDuration,
    );
    if (widget.isStopwatch) _pulseController.repeat();
    _pulseController.addListener(() => setState(() {}));
  }

  @override
  void didUpdateWidget(covariant InteractiveGauge oldWidget) {
    super.didUpdateWidget(oldWidget);
    // If interactivity changed (entering or leaving interactive mode) or
    // the incoming progress changed, sync the internal thumb position.
    if (widget.isInteractive != oldWidget.isInteractive ||
        (widget.progress != oldWidget.progress)) {
      _progress = widget.progress;
    }
    // Handle turning stopwatch animation on/off
    if (widget.isStopwatch && !oldWidget.isStopwatch) {
      _pulseController.repeat();
    } else if (!widget.isStopwatch && oldWidget.isStopwatch) {
      _pulseController.stop();
      _pulseController.value = 0.0;
    }
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
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
      widget.onChanged!(_progress);
    }
  }

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).colorScheme.primary;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    // Determine what progress to paint based on mode
    final double paintProgress = widget.isStopwatch
        ? _pulseController.value
        : _progress;

    // Fade the track slightly during stopwatch pulse
    final double trackOpacity = widget.isStopwatch
        ? (1.0 - _pulseController.value).clamp(0.2, 1.0)
        : 1.0;

    // Allow the caller to override the gauge color; fall back to theme primary
    final gaugeColor = widget.color ?? primaryColor;

    return GestureDetector(
      onPanStart: (details) => _handlePan(
        details.localPosition,
        const Size(AppTheme.gaugeSize, AppTheme.gaugeSize),
      ),
      onPanUpdate: (details) => _handlePan(
        details.localPosition,
        const Size(AppTheme.gaugeSize, AppTheme.gaugeSize),
      ),
      child: SizedBox(
        width: AppTheme.gaugeSize,
        height: AppTheme.gaugeSize,
        child: CustomPaint(
          painter: _GaugePainter(
            progress: paintProgress,
            color: gaugeColor.withValues(alpha: trackOpacity),
            isInteractive: widget.isInteractive,
            isDark: isDark,
          ),
          child: Padding(
            // Keeps the text safely inside the inner stroke
            padding: const EdgeInsets.all(40.0),
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const SizedBox(height: AppTheme.fsGaugeLabel),
                  Text(
                    widget.label,
                    style: TextStyle(
                      fontSize: AppTheme.fsGaugeLabel,
                      fontWeight: AppTheme.fwBold,
                      letterSpacing: AppTheme.lsExtraWide,
                      color:
                          widget.labelColor ??
                          (isDark
                              ? AppTheme.gaugeLabelDark
                              : AppTheme.gaugeTrackLight),
                    ),
                  ),
                  const SizedBox(height: AppTheme.spaceXSmall),
                  Text(
                    widget.centerText,
                    style: TextStyle(
                      fontSize: AppTheme.fsGaugeDisplay,
                      fontWeight: AppTheme.fwLight,
                      color: widget.centerTextColor != null
                          ? (isDark
                                ? AppTheme.gaugeWhite
                                : widget.centerTextColor)
                          : Theme.of(context).textTheme.bodyLarge?.color ??
                                AppTheme.gaugeTrackLight,
                    ),
                  ),
                  const SizedBox(height: AppTheme.spaceXSmall),
                  GestureDetector(
                    onTap: widget.onBottomTextTapped,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: AppTheme.spaceLarge,
                        vertical: AppTheme.spaceMedium,
                      ),
                      decoration: BoxDecoration(
                        color: isDark
                            ? AppTheme.gaugeBgDark
                            : AppTheme.gaugeBgLight,
                        borderRadius: BorderRadius.circular(
                          AppTheme.radiusCircle,
                        ),
                        border: Border.all(
                          color: isDark
                              ? AppTheme.gaugeBorderDark
                              : AppTheme.gaugeBorderLight,
                        ),
                      ),
                      child: Text(
                        widget.bottomText,
                        style: TextStyle(
                          color: widget.bottomTextColor,
                          fontSize: AppTheme.fsBodyLarge,
                          fontWeight: AppTheme.fwBold,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
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
  final bool isDark;

  _GaugePainter({
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
  bool shouldRepaint(covariant _GaugePainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.isInteractive != isInteractive ||
        oldDelegate.isDark != isDark;
  }
}
