import 'package:intl/intl.dart';

/// Utility functions for date and time comparisons and manipulations.
class AppDateUtils {
  const AppDateUtils._();

  /// Checks if two DateTimes represent the same calendar day.
  static bool isSameDay(DateTime? a, DateTime? b) {
    if (a == null || b == null) return false;
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  /// Returns the DateTime representing the Monday of the week for the given date.
  static DateTime startOfWeekMonday(DateTime date) {
    final day = DateTime(date.year, date.month, date.day);
    return day.subtract(Duration(days: date.weekday - DateTime.monday));
  }

  /// Checks if a date falls within an inclusive range.
  static bool isWithinInclusive(
    DateTime value,
    DateTime startInclusive,
    DateTime endInclusive,
  ) {
    return !value.isBefore(startInclusive) && !value.isAfter(endInclusive);
  }

  /// Generates a unique string key (YYYY-MM-DD) for a given date.
  static String dayKey(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  /// Converts a weekday integer to its short localized string representation.
  static String weekdayToStringShort(String locale, int weekday) {
    // Jan 1, 2024 was a Monday.
    // This perfectly maps 1 -> Mon, 2 -> Tue, ... 7 -> Sun.
    final dummyDate = DateTime(2024, 1, weekday);

    // .E() gives the abbreviated weekday (Mon, Tue, Wed...)
    return DateFormat.E(locale).format(dummyDate);
  }
}
