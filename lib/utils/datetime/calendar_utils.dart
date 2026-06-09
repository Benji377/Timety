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

  /// Gets the number of days in a month
  static int daysInMonth(DateTime date) {
    return DateTime(date.year, date.month + 1, 0).day;
  }
}
