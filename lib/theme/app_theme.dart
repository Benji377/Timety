import 'package:flutter/material.dart';

class AppTheme {
  // ===== SEMANTIC COLORS =====
  static const Color successColor = Colors.green;
  static const Color errorColor = Colors.red;
  static const Color warningColor = Colors.amber;
  static const Color infoColor = Colors.blue;

  // Types colors
  static const Color typeTaskColor = Colors.blue;
  static const Color typeHabitColor = Colors.purple;
  static const Color typeFocusColor = Colors.green;

  // Phase type colors
  static const Color phaseFocusColor = Colors.blue;
  static const Color phaseBreakShortColor = Colors.orange;
  static const Color phaseBreakLongColor = Colors.orange;
  static const Color phaseRestColor = Colors.grey;
  static const Color phaseSnackColor = Colors.green;
  static const Color phaseDistractedColor = Colors.red;

  // Status colors
  static const Color statusCompleted = Colors.green;
  static const Color statusOverdue = Colors.red;
  static const Color statusDueToday = Color(
    0xFFFBC02D,
  ); // Colors.amber.shade600 equivalent
  static const Color statusDefault = Colors.blue;

  // Component colors
  static const Color locationPinColor = Colors.red;
  static const Color wifiOffColor = Colors.grey;
  static const Color gaugeTrackLight = Color(
    0xFF9E9E9E,
  ); // Colors.grey.shade600
  static const Color gaugeBgLight = Color(0xFFF5F5F5); // Colors.grey.shade100
  static const Color gaugeWhite = Colors.white;
  static const Color gaugeBorderLight = Color(
    0xFFE0E0E0,
  ); // Colors.grey.shade300
  // Dark mode variants (use AppTheme constants instead of hardcoded colors)
  static const Color gaugeBgDark = Color(0xFF121212);
  static const Color gaugeBorderDark = Color(0xFF424242);
  static const Color gaugeTrackDark = Color(0xFF757575);
  static const Color gaugeLabelDark = Color(0xFFBDBDBD);

  // ===== TYPOGRAPHY =====
  // Font sizes for common components
  static const double fsHeadingLarge = 24;
  static const double fsHeadingMedium = 20;
  static const double fsHeadingSmall = 18;
  static const double fsBodyLarge = 16;
  static const double fsBodyMedium = 14;
  static const double fsBodySmall = 12;
  static const double fsCaption = 10;
  static const double fsGaugeDisplay = 60;
  static const double fsGaugeLabel = 20;
  static const double fsLargeNumber = 28;
  static const double fsTimeDisplay = 15;
  static const double fsPhaseTime = 18;
  static const double fsLabel = 12;

  // Font weights
  static const FontWeight fwLight = FontWeight.w300;
  static const FontWeight fwNormal = FontWeight.normal;
  static const FontWeight fwMedium = FontWeight.w500;
  static const FontWeight fwBold = FontWeight.bold;
  static const FontWeight fwExtraBold = FontWeight.w900;

  // Letter spacing
  static const double lsNormal = 0;
  static const double lsWide = 1.2;
  static const double lsExtraWide = 1.5;
  static const double lsTight = -0.5;

  // ===== SPACING =====
  // Padding/Margin constants
  static const double spaceTiny = 2;
  static const double spaceXSmall = 4;
  static const double spaceSmall = 8;
  static const double spaceMedium = 12;
  static const double spaceLarge = 16;
  static const double spaceXLarge = 24;
  static const double space2XLarge = 32;
  static const double space3XLarge = 40;

  // Common padding configurations
  static const EdgeInsets paddingScreenHorizontal = EdgeInsets.symmetric(
    horizontal: spaceLarge,
  );
  static const EdgeInsets paddingScreenVertical = EdgeInsets.all(spaceLarge);
  static const EdgeInsets paddingCard = EdgeInsets.all(spaceLarge);
  static const EdgeInsets paddingSection = EdgeInsets.fromLTRB(
    spaceXLarge,
    spaceXLarge,
    spaceXLarge,
    spaceSmall,
  );

  // ===== BORDER RADIUS =====
  static const double radiusSmall = 4;
  static const double radiusMedium = 8;
  static const double radiusLarge = 12;
  static const double radiusXLarge = 16;
  static const double radiusCircle = 20;
  static const double radiusCard = 25;

  // BorderRadius objects
  static const BorderRadius brSmall = BorderRadius.all(
    Radius.circular(radiusSmall),
  );
  static const BorderRadius brMedium = BorderRadius.all(
    Radius.circular(radiusMedium),
  );
  static const BorderRadius brLarge = BorderRadius.all(
    Radius.circular(radiusLarge),
  );
  static const BorderRadius brXLarge = BorderRadius.all(
    Radius.circular(radiusXLarge),
  );
  static const BorderRadius brCircle = BorderRadius.all(
    Radius.circular(radiusCircle),
  );
  static const BorderRadius brCard = BorderRadius.all(
    Radius.circular(radiusCard),
  );

  // ===== DIMENSIONS =====
  static const double gaugeSize = 300;
  static const double gaugeStrokeWidth = 16;
  static const double iconSizeSmall = 18;
  static const double iconSizeMedium = 24;
  static const double iconSizeLarge = 32;
  static const double profileImageSize = 100;

  // ===== DURATIONS =====
  static const Duration animationFast = Duration(milliseconds: 150);
  static const Duration animationNormal = Duration(milliseconds: 300);
  static const Duration animationSlow = Duration(milliseconds: 500);
  static const Duration pulseDuration = Duration(seconds: 2);
  static const Duration snackBarDuration = Duration(seconds: 2);

  // ===== SETTINGS DEFAULTS (from SettingsProvider) =====
  static const int maxNodeMins = 240;
  static const int maxStopwatchMins = 120;
  static const int dailyGoalMinsDefault = 90;

  // ===== OPACITY VALUES =====
  static const double opacityMedium = 0.5;
  static const double opacityLight = 0.3;
  static const double opacityVeryLight = 0.1;
  static const double opacityXLight = 0.2;

  // Factory method to build theme dynamically based on seedColor and brightness
  static ThemeData buildTheme({
    required Color seedColor,
    required Brightness brightness,
  }) {
    final isDark = brightness == Brightness.dark;

    return ThemeData(
      useMaterial3: true,
      // This generates a color scheme based on your seed color
      colorScheme: ColorScheme.fromSeed(
        seedColor: seedColor,
        brightness: brightness,
      ),
      // AppBar theming
      appBarTheme: AppBarTheme(
        backgroundColor: seedColor,
        foregroundColor: Colors.white,
        centerTitle: true,
        elevation: 0,
      ),
      // FloatingActionButton theming
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: seedColor,
        foregroundColor: Colors.white,
        elevation: 2,
      ),
      // Card theming
      cardTheme: CardThemeData(
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: AppTheme.brLarge),
        color: isDark ? Colors.grey.shade900 : Colors.white,
      ),
      // ElevatedButton theming
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          padding: const EdgeInsets.symmetric(
            horizontal: AppTheme.spaceLarge,
            vertical: AppTheme.spaceMedium,
          ),
          shape: RoundedRectangleBorder(borderRadius: AppTheme.brMedium),
        ),
      ),
      // OutlinedButton theming
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          padding: const EdgeInsets.symmetric(
            horizontal: AppTheme.spaceLarge,
            vertical: AppTheme.spaceMedium,
          ),
          shape: RoundedRectangleBorder(borderRadius: AppTheme.brMedium),
        ),
      ),
      // TextButton theming
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          padding: const EdgeInsets.symmetric(
            horizontal: AppTheme.spaceMedium,
            vertical: AppTheme.spaceSmall,
          ),
        ),
      ),
      // Input decoration theming
      inputDecorationTheme: InputDecorationTheme(
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spaceMedium,
          vertical: AppTheme.spaceMedium,
        ),
        border: OutlineInputBorder(borderRadius: AppTheme.brMedium),
        enabledBorder: OutlineInputBorder(
          borderRadius: AppTheme.brMedium,
          borderSide: BorderSide(
            color: isDark ? Colors.grey.shade700 : Colors.grey.shade300,
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppTheme.brMedium,
          borderSide: BorderSide(color: seedColor),
        ),
      ),
    );
  }

  // Default light theme with blue seed color
  static final ThemeData lightTheme = buildTheme(
    seedColor: Colors.blue,
    brightness: Brightness.light,
  );

  // Helper method to get phase color
  static Color getPhaseColor(String phaseType) {
    switch (phaseType.toLowerCase()) {
      case 'focus':
        return phaseFocusColor;
      case 'break_short':
        return phaseBreakShortColor;
      case 'break_long':
        return phaseBreakLongColor;
      case 'rest':
        return phaseRestColor;
      case 'snack':
        return phaseSnackColor;
      case 'distracted':
        return phaseDistractedColor;
      default:
        return infoColor;
    }
  }

  // Helper method to get task status color
  static Color getTaskStatusColor(String status) {
    switch (status.toLowerCase()) {
      case 'completed':
        return statusCompleted;
      case 'overdue':
        return statusOverdue;
      case 'due_today':
        return statusDueToday;
      default:
        return statusDefault;
    }
  }
}
