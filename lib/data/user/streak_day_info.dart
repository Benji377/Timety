class StreakDayInfo {
  final DateTime date;
  final bool isToday;
  final bool inCurrentStreak;
  final bool hasTask;
  final bool hasFocus;
  final bool hasHabit;

  const StreakDayInfo({
    required this.date,
    required this.isToday,
    required this.inCurrentStreak,
    required this.hasTask,
    required this.hasFocus,
    required this.hasHabit,
  });
}
