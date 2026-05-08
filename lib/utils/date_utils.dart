class AppDateUtils {
  const AppDateUtils._();

  static bool isSameDay(DateTime? a, DateTime? b) {
    if (a == null || b == null) return false;
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  static DateTime startOfWeekMonday(DateTime date) {
    final day = DateTime(date.year, date.month, date.day);
    return day.subtract(Duration(days: date.weekday - DateTime.monday));
  }

  static bool isWithinInclusive(
    DateTime value,
    DateTime startInclusive,
    DateTime endInclusive,
  ) {
    return !value.isBefore(startInclusive) && !value.isAfter(endInclusive);
  }

  static String dayKey(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }
}
