/// Utility functions for consistent date and time formatting across the app
class AppDateFormatUtils {
  const AppDateFormatUtils._();

  /// Formats time in HH:MM format
  ///
  /// Example: DateTime(2024, 1, 15, 14, 30) → "14:30"
  static String formatTime(DateTime time) {
    return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
  }

  /// Formats duration in MM:SS format
  ///
  /// Useful for displaying timer values
  /// Example: 125 seconds → "02:05"
  static String formatDuration(int totalSeconds) {
    final int minutes = totalSeconds ~/ 60;
    final int seconds = totalSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  /// Formats duration in HH:MM:SS format
  ///
  /// Example: 3665 seconds → "01:01:05"
  static String formatDurationLong(int totalSeconds) {
    final int hours = totalSeconds ~/ 3600;
    final int minutes = (totalSeconds % 3600) ~/ 60;
    final int seconds = totalSeconds % 60;
    return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  /// Converts seconds to minutes with optional decimal places
  ///
  /// Example: 125 seconds, decimals=1 → "2.1"
  static String secondsToMinutes(int totalSeconds, {int decimals = 0}) {
    final minutes = totalSeconds / 60;
    return minutes.toStringAsFixed(decimals);
  }

  /// Converts seconds to hours with optional decimal places
  static String secondsToHours(int totalSeconds, {int decimals = 1}) {
    final hours = totalSeconds / 3600;
    return hours.toStringAsFixed(decimals);
  }
}
