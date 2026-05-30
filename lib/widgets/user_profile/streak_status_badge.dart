import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../theme/app_theme.dart';

class StreakStatusBadge extends StatefulWidget {
  final bool isActive;

  const StreakStatusBadge({super.key, required this.isActive});

  @override
  State<StreakStatusBadge> createState() => _StreakStatusBadgeState();
}

class _StreakStatusBadgeState extends State<StreakStatusBadge>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 2000),
  );

  @override
  void didUpdateWidget(covariant StreakStatusBadge oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isActive && !_controller.isAnimating) {
      _controller.repeat(reverse: true);
    } else if (!widget.isActive && _controller.isAnimating) {
      _controller.stop();
      _controller.value = 0;
    }
  }

  @override
  void initState() {
    super.initState();
    if (widget.isActive) {
      _controller.repeat(reverse: true);
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final flameColor = widget.isActive
        ? AppTheme.warningColor
        : Theme.of(context).colorScheme.onSurfaceVariant.withValues(alpha: 0.6);

    return SizedBox(
      width: 32,
      height: 32,
      child: AnimatedBuilder(
        animation: _controller,
        builder: (context, child) {
          final t = _controller.value;
          final flameRise = widget.isActive ? math.sin(t * math.pi * 2) : 0.0;
          final flicker = widget.isActive
              ? 1.0 + (math.sin(t * math.pi * 4) * 0.05)
              : 1.0;

          return Stack(
            clipBehavior: Clip.none,
            alignment: Alignment.center,
            children: [
              if (widget.isActive)
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: AppTheme.warningColor.withValues(alpha: 0.18),
                    boxShadow: [
                      BoxShadow(
                        color: AppTheme.warningColor.withValues(alpha: 0.2),
                        blurRadius: 8 + (t * 2.5),
                        spreadRadius: 0.5 + (t * 0.2),
                      ),
                    ],
                  ),
                ),
              Transform.translate(
                offset: Offset(0, widget.isActive ? -0.5 * flameRise : 0),
                child: Transform.scale(
                  scale: flicker,
                  child: Icon(
                    Icons.local_fire_department,
                    size: 32,
                    color: widget.isActive
                        ? AppTheme.warningColor.withValues(alpha: 0.6)
                        : flameColor.withValues(alpha: 0.5),
                  ),
                ),
              ),
              if (widget.isActive)
                Transform.translate(
                  offset: Offset(0, -1.0 * flameRise),
                  child: const Align(
                    alignment: Alignment.bottomCenter,
                    child: Icon(
                      Icons.local_fire_department,
                      size: 20,
                      color: Color(0xFFFFE08A),
                    ),
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}
