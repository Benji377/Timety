import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_theme.dart';
import '../l10n/app_localizations.dart';

/// Generates a 1080×1920 (9:16) wrap-up PNG using dart:ui canvas drawing.
class WrapUpImageGenerator {
  // Output dimensions
  static const double _w = 1080;
  static const double _h = 1920;

  // Layout constants
  static const double _pad = 88;
  static const double _iconBoxSize = 100;
  static const double _iconSize = 54;
  static const double _statRowHeight = 108;
  static const double _statRowGap = 52;

  // Public entry point
  static Future<Uint8List> generate({
    required AppLocalizations l10n,
    required String name,
    required int level,
    required String levelTitle,
    required int streak,
    required int tasksCompleted,
    required int focusMins,
    required int habitsMet,
  }) async {
    // Load the logo asset into a dart:ui Image so we can draw it on canvas.
    final logoBytes = await rootBundle.load('assets/logo.png');
    final logoCodec = await ui.instantiateImageCodec(
      logoBytes.buffer.asUint8List(),
      targetWidth: 112,
      targetHeight: 112,
    );
    final logoImage = (await logoCodec.getNextFrame()).image;

    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder, const Rect.fromLTWH(0, 0, _w, _h));

    canvas.scale(1.2, 1.2);
    canvas.translate(0, 50);

    _drawBackground(canvas);

    // Track vertical cursor as we paint top-to-bottom.
    double y = _h * 0.085;

    y = _drawLogo(canvas, logoImage, y);
    y = _drawHeader(canvas, l10n, name, level, levelTitle, y);
    _drawStats(canvas, l10n, streak, tasksCompleted, focusMins, habitsMet, y);
    _drawFooter(canvas, l10n);

    final picture = recorder.endRecording();
    final image = await picture.toImage(_w.toInt(), _h.toInt());
    final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
    return byteData!.buffer.asUint8List();
  }

  static void _drawBackground(Canvas canvas) {
    final paint = Paint()
      ..shader = LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          AppTheme.paperDark,
          const Color(0xFF2A2418),
          AppTheme.taskColor.withValues(alpha: 0.9),
        ],
      ).createShader(const Rect.fromLTWH(0, 0, _w, _h));
    canvas.drawRect(const Rect.fromLTWH(0, 0, _w, _h), paint);
  }

  /// Returns updated y after drawing the logo.
  static double _drawLogo(Canvas canvas, ui.Image logo, double y) {
    const size = 112.0;
    canvas.drawImageRect(
      logo,
      Rect.fromLTWH(0, 0, logo.width.toDouble(), logo.height.toDouble()),
      Rect.fromLTWH(_pad, y, size, size),
      Paint(),
    );
    return y + size + 36;
  }

  /// Returns updated y after drawing name / level header block.
  static double _drawHeader(
    Canvas canvas,
    AppLocalizations l10n,
    String name,
    int level,
    String levelTitle,
    double y,
  ) {
    // Draws the header decoration line
    _paintText(
      canvas,
      l10n.wrapUpHeaderEyebrow,
      Offset(_pad, y),
      const TextStyle(
        color: AppTheme.taskColor,
        fontSize: 28,
        fontWeight: FontWeight.bold,
        letterSpacing: 5,
      ),
    );
    y += 52;

    // Name
    y = _paintFittedText(
      canvas,
      l10n.wrapUpHeaderTitle(name),
      Offset(_pad, y),
      const TextStyle(
        color: Colors.white,
        fontSize: 82,
        fontWeight: FontWeight.w900,
        height: 1.1,
      ),
      maxWidth: _w - _pad * 2,
    );
    y += 14;

    // Level badge
    _paintText(
      canvas,
      l10n.wrapUpHeaderLevel(level, levelTitle),
      Offset(_pad, y),
      const TextStyle(
        color: AppTheme.warningColor,
        fontSize: 38,
        fontWeight: FontWeight.bold,
      ),
    );
    y += 58;

    return y;
  }

  /// Draws the four stat rows, vertically centred in the remaining canvas space.
  static void _drawStats(
    Canvas canvas,
    AppLocalizations l10n,
    int streak,
    int tasks,
    int focusMins,
    int habits,
    double headerBottom,
  ) {
    // A fixed, comfortable gap right below the "Level x" text
    final startY = headerBottom + 100;

    final rows = [
      _StatRow(
        Icons.local_fire_department,
        AppTheme.warningColor,
        l10n.wrapUpStatStreakValue(streak),
        l10n.wrapUpStatStreakLabel,
      ),
      _StatRow(
        Icons.check_circle,
        AppTheme.taskColor,
        '$tasks',
        l10n.wrapUpStatTasksLabel,
      ),
      _StatRow(
        Icons.timer,
        AppTheme.successColor,
        '$focusMins',
        l10n.wrapUpStatFocusLabel,
      ),
      _StatRow(
        Icons.repeat,
        AppTheme.habitColor,
        '$habits',
        l10n.wrapUpStatHabitsLabel,
      ),
    ];

    double y = startY;
    for (final row in rows) {
      _drawStatRow(canvas, row, y);
      y += _statRowHeight + _statRowGap;
    }
  }

  static void _drawStatRow(Canvas canvas, _StatRow row, double y) {
    // Coloured rounded background box
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(_pad, y, _iconBoxSize, _iconBoxSize),
        const Radius.circular(22),
      ),
      Paint()..color = row.color.withValues(alpha: 0.22),
    );

    // Material icon drawn as a text glyph (icons ARE a font in Flutter)
    final iconPainter = TextPainter(
      text: TextSpan(
        text: String.fromCharCode(row.icon.codePoint),
        style: TextStyle(
          fontSize: _iconSize,
          fontFamily: row.icon.fontFamily,
          color: row.color,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    iconPainter.paint(
      canvas,
      Offset(
        _pad + (_iconBoxSize - iconPainter.width) / 2,
        y + (_iconBoxSize - iconPainter.height) / 2,
      ),
    );

    // Value (large bold number / text)
    const textX = _pad + _iconBoxSize + 30;
    _paintText(
      canvas,
      row.value,
      Offset(textX, y + 2),
      const TextStyle(
        color: Colors.white,
        fontSize: 52,
        fontWeight: FontWeight.bold,
      ),
    );

    // Label (smaller, muted)
    _paintText(
      canvas,
      row.label,
      Offset(textX, y + 58),
      const TextStyle(
        color: Color(0xB3FFFFFF), // white70
        fontSize: 30,
      ),
    );
  }

  static void _drawFooter(Canvas canvas, AppLocalizations l10n) {
    _paintText(
      canvas,
      l10n.wrapUpFooter,
      const Offset(_w / 2 - 100, _h - 550),
      const TextStyle(
        color: Colors.white54,
        fontSize: 30,
        fontWeight: FontWeight.bold,
      ),
      textAlign: TextAlign.center,
      anchorCenter: true,
    );
  }

  /// Paints text and returns the bottom edge (y + measured height).
  static double _paintText(
    Canvas canvas,
    String text,
    Offset position,
    TextStyle style, {
    double maxWidth = _w - _pad * 2,
    TextAlign textAlign = TextAlign.left,
    bool anchorCenter = false,
  }) {
    final painter = TextPainter(
      text: TextSpan(text: text, style: style),
      textDirection: TextDirection.ltr,
      textAlign: textAlign,
    )..layout(maxWidth: maxWidth);

    final dx = anchorCenter ? position.dx - painter.width / 2 : position.dx;
    final dy = anchorCenter ? position.dy - painter.height / 2 : position.dy;
    painter.paint(canvas, Offset(dx, dy));
    return dy + painter.height;
  }

  /// Like [_paintText] but scales the font down until the text fits on one line.
  /// Mirrors the behaviour of Flutter's FittedBox(fit: BoxFit.scaleDown).
  static double _paintFittedText(
    Canvas canvas,
    String text,
    Offset position,
    TextStyle style, {
    required double maxWidth,
  }) {
    double fontSize = style.fontSize!;
    late TextPainter painter;

    while (fontSize >= 20) {
      painter = TextPainter(
        text: TextSpan(
          text: text,
          style: style.copyWith(fontSize: fontSize),
        ),
        textDirection: TextDirection.ltr,
        maxLines: 1,
      )..layout();

      if (painter.width <= maxWidth) break;
      fontSize -= 4;
    }

    painter.paint(canvas, position);
    return position.dy + painter.height;
  }
}

// Internal data class
class _StatRow {
  final IconData icon;
  final Color color;
  final String value;
  final String label;
  const _StatRow(this.icon, this.color, this.value, this.label);
}
