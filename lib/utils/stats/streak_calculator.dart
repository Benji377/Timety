import '../datetime/date_utils.dart';

class StreakCalculator {
  const StreakCalculator._();

  static int calculateBestStreak(Iterable<DateTime> completions) {
    final sortedUniqueDates = _sortedUniqueDayKeys(completions);
    if (sortedUniqueDates.isEmpty) return 0;

    int highest = 1;
    int currentRun = 1;

    for (int i = 1; i < sortedUniqueDates.length; i++) {
      final prev = DateTime.parse(sortedUniqueDates[i - 1]);
      final current = DateTime.parse(sortedUniqueDates[i]);

      if (current.difference(prev).inDays == 1) {
        currentRun++;
        if (currentRun > highest) highest = currentRun;
      } else {
        currentRun = 1;
      }
    }

    return highest;
  }

  static int calculateCurrentStreak(Iterable<DateTime> completions) {
    final dayKeys = completions.map(AppDateUtils.dayKey).toSet();
    if (dayKeys.isEmpty) return 0;

    var checkDate = DateTime.now();

    if (!dayKeys.contains(AppDateUtils.dayKey(checkDate))) {
      final yesterday = checkDate.subtract(const Duration(days: 1));
      if (!dayKeys.contains(AppDateUtils.dayKey(yesterday))) return 0;
      checkDate = yesterday;
    }

    int current = 0;
    while (dayKeys.contains(AppDateUtils.dayKey(checkDate))) {
      current++;
      checkDate = checkDate.subtract(const Duration(days: 1));
    }

    return current;
  }

  static ({int current, int highest}) calculateBoth(Iterable<DateTime> dates) {
    return (
      current: calculateCurrentStreak(dates),
      highest: calculateBestStreak(dates),
    );
  }

  static List<String> _sortedUniqueDayKeys(Iterable<DateTime> dates) {
    final sorted = dates.map(AppDateUtils.dayKey).toSet().toList();
    sorted.sort();
    return sorted;
  }
}
