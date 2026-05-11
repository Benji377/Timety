import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:timety/theme/app_theme.dart';

void main() {
  group('AppTheme', () {
    test('builds light and dark themes with the expected palette', () {
      final lightTheme = AppTheme.buildTheme(brightness: Brightness.light);
      final darkTheme = AppTheme.buildTheme(brightness: Brightness.dark);

      expect(lightTheme.brightness, Brightness.light);
      expect(darkTheme.brightness, Brightness.dark);
      expect(lightTheme.colorScheme.primary, AppTheme.taskColor);
      expect(darkTheme.colorScheme.secondary, AppTheme.focusColor);
      expect(lightTheme.scaffoldBackgroundColor, AppTheme.paperLight);
      expect(darkTheme.scaffoldBackgroundColor, AppTheme.paperDark);
    });
  });
}
