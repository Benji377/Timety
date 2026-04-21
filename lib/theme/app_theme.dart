import 'package:flutter/material.dart';

@immutable
class TimetySemanticColors extends ThemeExtension<TimetySemanticColors> {
  final Color success;
  final Color warning;
  final Color info;
  final Color focus;
  final Color calm;

  const TimetySemanticColors({
    required this.success,
    required this.warning,
    required this.info,
    required this.focus,
    required this.calm,
  });

  @override
  TimetySemanticColors copyWith({
    Color? success,
    Color? warning,
    Color? info,
    Color? focus,
    Color? calm,
  }) {
    return TimetySemanticColors(
      success: success ?? this.success,
      warning: warning ?? this.warning,
      info: info ?? this.info,
      focus: focus ?? this.focus,
      calm: calm ?? this.calm,
    );
  }

  @override
  TimetySemanticColors lerp(
    covariant ThemeExtension<TimetySemanticColors>? other,
    double t,
  ) {
    if (other is! TimetySemanticColors) return this;
    return TimetySemanticColors(
      success: Color.lerp(success, other.success, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      info: Color.lerp(info, other.info, t)!,
      focus: Color.lerp(focus, other.focus, t)!,
      calm: Color.lerp(calm, other.calm, t)!,
    );
  }
}

class TimetyTheme {
  static const Color _seed = Color(0xFF0F766E);

  static ThemeData light() => _buildTheme(Brightness.light);

  static ThemeData dark() => _buildTheme(Brightness.dark);

  static ThemeData _buildTheme(Brightness brightness) {
    final colorScheme =
        ColorScheme.fromSeed(seedColor: _seed, brightness: brightness).copyWith(
          secondary: brightness == Brightness.light
              ? const Color(0xFF2563EB)
              : const Color(0xFF60A5FA),
          tertiary: brightness == Brightness.light
              ? const Color(0xFFF97316)
              : const Color(0xFFFBBF24),
        );

    final semanticColors = TimetySemanticColors(
      success: brightness == Brightness.light
          ? const Color(0xFF16A34A)
          : const Color(0xFF4ADE80),
      warning: brightness == Brightness.light
          ? const Color(0xFFF59E0B)
          : const Color(0xFFFBBF24),
      info: brightness == Brightness.light
          ? const Color(0xFF3B82F6)
          : const Color(0xFF38BDF8),
      focus: colorScheme.primary,
      calm: brightness == Brightness.light
          ? const Color(0xFF8B5CF6)
          : const Color(0xFFA78BFA),
    );

    final baseTextTheme = ThemeData(brightness: brightness).textTheme;
    final textTheme = baseTextTheme.copyWith(
      headlineSmall: baseTextTheme.headlineSmall?.copyWith(
        fontWeight: FontWeight.w700,
        letterSpacing: -0.3,
      ),
      headlineMedium: baseTextTheme.headlineMedium?.copyWith(
        fontWeight: FontWeight.w700,
        letterSpacing: -0.4,
      ),
      titleLarge: baseTextTheme.titleLarge?.copyWith(
        fontWeight: FontWeight.w700,
      ),
      titleMedium: baseTextTheme.titleMedium?.copyWith(
        fontWeight: FontWeight.w600,
      ),
      bodyLarge: baseTextTheme.bodyLarge?.copyWith(height: 1.35),
      bodyMedium: baseTextTheme.bodyMedium?.copyWith(height: 1.35),
    );

    final schemeSurface = colorScheme.surface;
    final schemeContainer = colorScheme.surfaceContainerLow;

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: colorScheme,
      textTheme: textTheme,
      scaffoldBackgroundColor: schemeSurface,
      extensions: <ThemeExtension<dynamic>>[semanticColors],
      appBarTheme: AppBarTheme(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        foregroundColor: colorScheme.onSurface,
        titleTextStyle: textTheme.titleLarge?.copyWith(
          color: colorScheme.onSurface,
        ),
      ),
      cardTheme: CardThemeData(
        color: schemeContainer,
        surfaceTintColor: colorScheme.surfaceTint,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
          side: BorderSide(
            color: colorScheme.outlineVariant.withValues(alpha: 0.5),
          ),
        ),
      ),
      dividerTheme: DividerThemeData(
        color: colorScheme.outlineVariant.withValues(alpha: 0.4),
        thickness: 1,
        space: 24,
      ),
      chipTheme: ChipThemeData(
        backgroundColor: colorScheme.surfaceContainerHighest,
        selectedColor: colorScheme.primaryContainer,
        secondarySelectedColor: colorScheme.primaryContainer,
        labelStyle: textTheme.labelMedium?.copyWith(
          color: colorScheme.onSurface,
        ),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        side: BorderSide(
          color: colorScheme.outlineVariant.withValues(alpha: 0.4),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: colorScheme.primary,
          foregroundColor: colorScheme.onPrimary,
          elevation: 0,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          textStyle: textTheme.labelLarge?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: colorScheme.onSurface,
          side: BorderSide(color: colorScheme.outlineVariant),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          textStyle: textTheme.labelLarge?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: colorScheme.primary,
          textStyle: textTheme.labelLarge?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: colorScheme.primaryContainer,
        foregroundColor: colorScheme.onPrimaryContainer,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colorScheme.surfaceContainerHighest.withValues(alpha: 0.55),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: colorScheme.outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(
            color: colorScheme.outlineVariant.withValues(alpha: 0.5),
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: BorderSide(color: colorScheme.primary, width: 1.5),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 16,
        ),
      ),
      listTileTheme: ListTileThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        iconColor: colorScheme.primary,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: colorScheme.surface.withValues(alpha: 0.94),
        indicatorColor: colorScheme.primaryContainer,
        labelTextStyle: WidgetStatePropertyAll(
          textTheme.labelMedium?.copyWith(fontWeight: FontWeight.w700),
        ),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: colorScheme.surface,
        selectedItemColor: colorScheme.primary,
        unselectedItemColor: colorScheme.onSurfaceVariant,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: colorScheme.primary,
        linearTrackColor: colorScheme.surfaceContainerHighest,
        circularTrackColor: colorScheme.surfaceContainerHighest,
      ),
    );
  }
}
