import 'package:flutter/material.dart';

class AppTheme {
  // Factory method to build theme dynamically based on seedColor and brightness
  static ThemeData buildTheme({
    required Color seedColor,
    required Brightness brightness,
  }) {
    return ThemeData(
      useMaterial3: true,
      // This generates a color scheme based on your seed color
      colorScheme: ColorScheme.fromSeed(
        seedColor: seedColor,
        brightness: brightness,
      ),
      // You can customize specific components here
      appBarTheme: AppBarTheme(
        backgroundColor: seedColor,
        foregroundColor: Colors.white,
        centerTitle: true,
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: seedColor,
        foregroundColor: Colors.white,
      ),
    );
  }

  // Default light theme with blue seed color
  static final ThemeData lightTheme = buildTheme(
    seedColor: Colors.blue,
    brightness: Brightness.light,
  );
}
