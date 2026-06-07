/// Utility functions for consistent date and time formatting across the app
class AppDateFormatUtils {
  const AppDateFormatUtils._();

  /// Formats duration in MM:SS format
  ///
  /// Useful for displaying timer values
  /// Example: 125 seconds → "02:05"
  static String formatDuration(int totalSeconds) {
    final int minutes = totalSeconds ~/ 60;
    final int seconds = totalSeconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }
}
