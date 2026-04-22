import 'package:flutter/material.dart';

class AppTheme {
  // We make this static so we can access it via AppTheme.lightTheme
  static final ThemeData lightTheme = ThemeData(
    useMaterial3: true,
    // This generates a color scheme based on your seed color
    colorScheme: ColorScheme.fromSeed(
      seedColor: Colors.deepPurple, 
      brightness: Brightness.light,
    ),
    // You can customize specific components here
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.deepPurple,
      foregroundColor: Colors.white,
      centerTitle: true,
    ),
    floatingActionButtonTheme: const FloatingActionButtonThemeData(
      backgroundColor: Colors.deepPurple,
      foregroundColor: Colors.white,
    ),
  );
}