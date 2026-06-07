import 'package:intl/intl.dart';

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

  static String weekdayToStringShort(String locale, int weekday) {
    // Jan 1, 2024 was a Monday. 
    // This perfectly maps 1 -> Mon, 2 -> Tue, ... 7 -> Sun.
    final dummyDate = DateTime(2024, 1, weekday);

    // .E() gives the abbreviated weekday (Mon, Tue, Wed...)
    return DateFormat.E(locale).format(dummyDate);
  }
}
