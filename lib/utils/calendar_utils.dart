/// Utility functions for calendar-related logic
class CalendarUtils {
  const CalendarUtils._();

  /// Generates a list of weeks for a given month
  ///
  /// Each week starts on Monday and contains all 7 days.
  /// The returned structure includes days from adjacent months to complete the first and last weeks.
  ///
  /// Example: For May 2026, returns 6 weeks with days from April, May, and June as needed
  static List<List<DateTime>> generateWeeks(DateTime month) {
    final firstDayOfMonth = DateTime(month.year, month.month);
    final lastDayOfMonth = DateTime(month.year, month.month + 1, 0);

    // Calculate offset to Monday of the first week
    final int offsetToMonday = firstDayOfMonth.weekday - DateTime.monday;
    DateTime currentDay = firstDayOfMonth.subtract(
      Duration(days: offsetToMonday),
    );

    final List<List<DateTime>> weeks = [];

    // Generate weeks until we pass the last day of month AND reach Monday
    while (currentDay.isBefore(lastDayOfMonth) ||
        currentDay.weekday != DateTime.monday) {
      final List<DateTime> week = [];
      for (int i = 0; i < 7; i++) {
        week.add(currentDay);
        currentDay = currentDay.add(const Duration(days: 1));
      }
      weeks.add(week);
    }

    return weeks;
  }

  /// Checks if a date is in the current month
  static bool isInMonth(DateTime date, DateTime month) {
    return date.year == month.year && date.month == month.month;
  }

  /// Gets the number of days in a month
  static int daysInMonth(DateTime date) {
    return DateTime(date.year, date.month + 1, 0).day;
  }

  /// Checks if two dates are in the same week (Monday to Sunday)
  static bool isInSameWeek(DateTime date1, DateTime date2) {
    final week1Start = date1.subtract(
      Duration(days: date1.weekday - DateTime.monday),
    );
    final week2Start = date2.subtract(
      Duration(days: date2.weekday - DateTime.monday),
    );
    return week1Start.year == week2Start.year &&
        week1Start.month == week2Start.month &&
        week1Start.day == week2Start.day;
  }
}
