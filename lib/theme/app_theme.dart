import 'package:flutter/material.dart';

class AppTheme {
  // ===== CORE PALETTE =====
  static const Color taskColor = Color(0xFF2563EB);
  static const Color focusColor = Color(0xFF16A34A);
  static const Color habitColor = Color(0xFF7C3AED);
  static const Color userColor = Color(0xFFDC2626);
  static const Color warningAccent = Color(0xFFF59E0B);

  static const Color inkLight = Color(0xFF111111);
  static const Color inkDark = Color(0xFFF5F5F5);
  static const Color paperLight = Color(0xFFFFF8EF);
  static const Color paperAltLight = Color(0xFFF6EEDB);
  static const Color paperDark = Color(0xFF151515);
  static const Color paperAltDark = Color(0xFF202020);
  static const Color borderLight = Color(0xFF111111);
  static const Color borderDark = Color(0xFFF2E8D5);
  static const Color shadowColor = Color(0xFF111111);

  // ===== SEMANTIC COLORS =====
  static const Color successColor = focusColor;
  static const Color errorColor = userColor;
  static const Color warningColor = warningAccent;
  static const Color infoColor = taskColor;

  // Types colors
  static const Color typeTaskColor = taskColor;
  static const Color typeHabitColor = habitColor;
  static const Color typeFocusColor = focusColor;

  // Phase type colors
  static const Color phaseFocusColor = focusColor;
  static const Color phaseBreakShortColor = warningAccent;
  static const Color phaseBreakLongColor = warningAccent;
  static const Color phaseRestColor = Color(0xFF6B7280);
  static const Color phaseSnackColor = habitColor;
  static const Color phaseDistractedColor = userColor;

  // Status colors
  static const Color statusCompleted = focusColor;
  static const Color statusOverdue = userColor;
  static const Color statusDueToday = warningAccent;
  static const Color statusDefault = taskColor;

  // Component colors
  static const Color locationPinColor = userColor;
  static const Color wifiOffColor = Color(0xFF6B7280);
  static const Color gaugeTrackLight = Color(0xFF7C7C7C);
  static const Color gaugeBgLight = paperLight;
  static const Color gaugeWhite = Colors.white;
  static const Color gaugeBorderLight = Color(0xFFE5DED1);
  static const Color gaugeBgDark = paperDark;
  static const Color gaugeBorderDark = Color(0xFF4A4A4A);
  static const Color gaugeTrackDark = Color(0xFF8C8C8C);
  static const Color gaugeLabelDark = Color(0xFFD4D4D4);

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
  static const double radiusNeo = 14;

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
  static const BorderRadius brNeo = BorderRadius.all(
    Radius.circular(radiusNeo),
  );

  // ===== DIMENSIONS =====
  static const double gaugeSize = 300;
  static const double gaugeStrokeWidth = 16;
  static const double iconSizeSmall = 18;
  static const double iconSizeMedium = 24;
  static const double iconSizeLarge = 32;
  static const double profileImageSize = 100;
  static const double listSectionIconSize = 12;
  static const double listSectionTitleSize = fsBodyLarge;
  static const double listTileBorderWidth = 2;
  static const double neoBorderWidth = 3;
  static const double listTileTrailingSpacing = 8;
  static const double listTileSwipeIconSize = 24;
  static const EdgeInsets listTileScreenMargin = EdgeInsets.symmetric(
    horizontal: spaceLarge,
    vertical: spaceXSmall,
  );

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

  static Color _backgroundColor(Brightness brightness) {
    return brightness == Brightness.dark ? paperDark : paperLight;
  }

  static Color _surfaceColor(Brightness brightness) {
    return brightness == Brightness.dark ? paperAltDark : Colors.white;
  }

  static Color _surfaceAltColor(Brightness brightness) {
    return brightness == Brightness.dark
        ? const Color(0xFF2A2A2A)
        : paperAltLight;
  }

  static Color _foregroundColor(Brightness brightness) {
    return brightness == Brightness.dark ? inkDark : inkLight;
  }

  static Color _borderColor(Brightness brightness) {
    return brightness == Brightness.dark ? borderDark : borderLight;
  }

  // Factory method to build the app theme with a Neobrutalist look.
  static ThemeData buildTheme({required Brightness brightness}) {
    final isDark = brightness == Brightness.dark;
    final backgroundColor = _backgroundColor(brightness);
    final surfaceColor = _surfaceColor(brightness);
    final surfaceAltColor = _surfaceAltColor(brightness);
    final foregroundColor = _foregroundColor(brightness);
    final borderColor = _borderColor(brightness);

    final colorScheme = isDark
        ? ColorScheme.dark(
            primary: taskColor,
            secondary: focusColor,
            tertiary: habitColor,
            error: userColor,
            surface: surfaceColor,
            onSurface: foregroundColor,
            outline: borderColor,
            surfaceContainerHighest: surfaceAltColor,
            surfaceContainerHigh: surfaceAltColor,
            surfaceContainer: surfaceColor,
          )
        : ColorScheme.light(
            primary: taskColor,
            secondary: focusColor,
            tertiary: habitColor,
            error: userColor,
            surface: surfaceColor,
            onSurface: foregroundColor,
            outline: borderColor,
            surfaceContainerHighest: surfaceAltColor,
            surfaceContainerHigh: surfaceAltColor,
            surfaceContainer: surfaceColor,
          );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: backgroundColor,
      canvasColor: backgroundColor,
      dividerTheme: DividerThemeData(
        color: borderColor,
        thickness: 2,
        space: 24,
      ),
      iconTheme: IconThemeData(color: foregroundColor),
      appBarTheme: AppBarTheme(
        backgroundColor: backgroundColor,
        foregroundColor: foregroundColor,
        centerTitle: true,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
        titleTextStyle: TextStyle(
          color: foregroundColor,
          fontSize: AppTheme.fsHeadingMedium,
          fontWeight: AppTheme.fwExtraBold,
          letterSpacing: AppTheme.lsTight,
        ),
        iconTheme: IconThemeData(color: foregroundColor),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: taskColor,
        foregroundColor: Colors.white,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: AppTheme.brNeo,
          side: BorderSide(color: borderColor, width: AppTheme.neoBorderWidth),
        ),
      ),
      cardTheme: CardThemeData(
        elevation: 6,
        shadowColor: shadowColor,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: AppTheme.brNeo,
          side: BorderSide(color: borderColor, width: AppTheme.neoBorderWidth),
        ),
        color: surfaceColor,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: taskColor,
          foregroundColor: Colors.white,
          elevation: 0,
          shadowColor: shadowColor,
          padding: const EdgeInsets.symmetric(
            horizontal: AppTheme.spaceLarge,
            vertical: AppTheme.spaceMedium,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: AppTheme.brNeo,
            side: BorderSide(
              color: borderColor,
              width: AppTheme.neoBorderWidth,
            ),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: foregroundColor,
          side: BorderSide(color: borderColor, width: AppTheme.neoBorderWidth),
          padding: const EdgeInsets.symmetric(
            horizontal: AppTheme.spaceLarge,
            vertical: AppTheme.spaceMedium,
          ),
          shape: const RoundedRectangleBorder(borderRadius: AppTheme.brNeo),
          elevation: 0,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: foregroundColor,
          padding: const EdgeInsets.symmetric(
            horizontal: AppTheme.spaceMedium,
            vertical: AppTheme.spaceSmall,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: AppTheme.brNeo,
            side: BorderSide(color: borderColor, width: 2),
          ),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: taskColor,
          foregroundColor: Colors.white,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: AppTheme.brNeo,
            side: BorderSide(
              color: borderColor,
              width: AppTheme.neoBorderWidth,
            ),
          ),
          padding: const EdgeInsets.symmetric(
            horizontal: AppTheme.spaceLarge,
            vertical: AppTheme.spaceMedium,
          ),
        ),
      ),
      checkboxTheme: CheckboxThemeData(
        fillColor: const WidgetStatePropertyAll(taskColor),
        checkColor: const WidgetStatePropertyAll(Colors.white),
        side: BorderSide(color: borderColor, width: 2),
        shape: const RoundedRectangleBorder(borderRadius: AppTheme.brSmall),
      ),
      switchTheme: SwitchThemeData(
        thumbColor: const WidgetStatePropertyAll(Colors.white),
        trackColor: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) return taskColor;
          return brightness == Brightness.dark
              ? const Color(0xFF3A3A3A)
              : const Color(0xFFBDBDBD);
        }),
        trackOutlineColor: WidgetStatePropertyAll(borderColor),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: backgroundColor,
        selectedItemColor: taskColor,
        unselectedItemColor: brightness == Brightness.dark
            ? const Color(0xFF9A9A9A)
            : const Color(0xFF666666),
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: surfaceAltColor,
        selectedColor: taskColor,
        secondarySelectedColor: focusColor,
        disabledColor: surfaceAltColor.withValues(alpha: 0.8),
        labelStyle: TextStyle(color: foregroundColor),
        secondaryLabelStyle: const TextStyle(color: Colors.white),
        side: BorderSide(color: borderColor, width: 2),
        shape: const RoundedRectangleBorder(borderRadius: AppTheme.brNeo),
        padding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spaceSmall,
          vertical: AppTheme.spaceXSmall,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surfaceColor,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppTheme.spaceMedium,
          vertical: AppTheme.spaceMedium,
        ),
        border: OutlineInputBorder(
          borderRadius: AppTheme.brNeo,
          borderSide: BorderSide(color: borderColor, width: 2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: AppTheme.brNeo,
          borderSide: BorderSide(color: borderColor, width: 2),
        ),
        focusedBorder: const OutlineInputBorder(
          borderRadius: AppTheme.brNeo,
          borderSide: BorderSide(color: taskColor, width: 3),
        ),
        labelStyle: TextStyle(color: foregroundColor),
        hintStyle: TextStyle(color: foregroundColor.withValues(alpha: 0.65)),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: surfaceColor,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: AppTheme.brNeo,
          side: BorderSide(color: borderColor, width: AppTheme.neoBorderWidth),
        ),
      ),
    );
  }

  static final ThemeData lightTheme = buildTheme(brightness: Brightness.light);
  static final ThemeData darkTheme = buildTheme(brightness: Brightness.dark);

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
