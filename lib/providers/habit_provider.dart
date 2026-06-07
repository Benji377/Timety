import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import '../providers/settings_provider.dart';
import '../data/habit/habit_models.dart';
import '../data/habit/habit_repository.dart';
import '../services/notification_service.dart';
import '../utils/datetime/date_utils.dart';
import '../utils/ui/l10n_utils.dart';
import '../utils/stats/xp_calculator.dart';
import '../services/android_widgets/habit_widget_service.dart';
import 'user_provider.dart';

class HabitProvider extends ChangeNotifier {
  final HabitRepository repository;
  List<Habit> _habits = [];
  SettingsProvider? _settings;

  List<Habit> get habits => _habits;

  HabitProvider({required this.repository});

  // Helper to notify listeners and update home widget
  void _notifyAndSync() {
    notifyListeners();

    final locale =
        _settings?.appLocale ?? ui.PlatformDispatcher.instance.locale;

    HabitWidgetService.updateHabitWidget(_habits, this, locale);
  }

  void updateSettings(SettingsProvider settings) {
    _settings = settings;
    syncNotifications();
    _notifyAndSync();
  }

  Future<void> loadHabits() async {
    _habits = await repository.fetchHabits();
    syncNotifications();
    _notifyAndSync();
  }

  Future<void> saveHabit(Habit habit) async {
    await repository.saveHabit(habit);
    final index = _habits.indexWhere((h) => h.id == habit.id);
    if (index != -1) {
      _habits[index] = habit;
    } else {
      _habits.add(habit);
    }
    syncNotifications();
    _notifyAndSync();
  }

  Future<void> deleteHabit(String id) async {
    await repository.deleteHabit(id);
    _habits.removeWhere((h) => h.id == id);
    syncNotifications();
    _notifyAndSync();
  }

  void syncNotifications() {
    if (_settings == null) return;
    final l10n = getL10n(settings: _settings);

    final todaysHabits = getHabitsForDay(
      DateTime.now(),
    ).map((h) => h.name).toList();

    NotificationService.instance.scheduleDailyMotivation(
      time: _settings!.notificationTime,
      todaysHabits: todaysHabits,
      l10n: l10n,
    );

    NotificationService.instance.scheduleEndOfDayCheckup(
      time: _settings!.endOfDayTime,
      l10n: l10n,
    );

    for (var habit in _habits) {
      if (habit.targetTime != null) {
        NotificationService.instance.scheduleHabitReminder(
          habitId: habit.id,
          habitName: habit.name,
          title: l10n.notificationHabitTitle,
          body: l10n.notificationHabitBody(habit.name),
          time: habit.targetTime!,
          targetWeekdays: habit.targetWeekdays,
          l10n: l10n,
        );
      } else {
        NotificationService.instance.cancelHabitReminder(habit.id);
      }
    }
  }

  // --- CORE HABIT LOGIC ---

  // Checks if a habit was completed on a specific day
  bool isCompletedOn(Habit habit, DateTime date) {
    return habit.completions.any(
      (completion) => AppDateUtils.isSameDay(completion, date),
    );
  }

  // Toggles completion for today
  Future<void> toggleCompletionToday(
    Habit habit, {
    UserProvider? userProvider,
  }) async {
    final today = DateTime.now();
    final completed = isCompletedOn(habit, today);

    if (completed) {
      habit.completions.removeWhere((c) => AppDateUtils.isSameDay(c, today));
      userProvider?.addXp(-ExperienceEngine.xpPerHabit);
    } else {
      habit.completions.add(today);
      userProvider?.addXp(ExperienceEngine.xpPerHabit);
    }

    await saveHabit(habit);
  }

  Future<void> markHabitCompletedToday(
    String id, {
    UserProvider? userProvider,
  }) async {
    final habit = getHabitById(id);
    if (habit == null || isCompletedOn(habit, DateTime.now())) return;
    await markCompletionOnDate(
      habit,
      DateTime.now(),
      userProvider: userProvider,
    );
  }

  Habit? getHabitById(String id) {
    for (final habit in _habits) {
      if (habit.id == id) return habit;
    }
    return null;
  }

  int getCompletionsThisWeek(Habit habit, {bool includeToday = true}) {
    final now = DateTime.now();
    final startOfWeek = DateTime(
      now.year,
      now.month,
      now.day,
    ).subtract(Duration(days: now.weekday - 1));
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59),
    );

    final today = DateTime(now.year, now.month, now.day);

    return habit.completions.where((c) {
      final isInWeek = c.isAfter(startOfWeek) && c.isBefore(endOfWeek);
      if (!isInWeek) return false;
      if (includeToday) return true;
      return !AppDateUtils.isSameDay(c, today);
    }).length;
  }

  List<Habit> getHabitsForDay(DateTime date) {
    return _habits.where((habit) {
      if (habit.frequency == HabitFrequency.daily) return true;
      if (habit.frequency == HabitFrequency.weeklyFlexible) {
        return true;
      }
      if (habit.frequency == HabitFrequency.weeklyExact) {
        return habit.targetWeekdays?.contains(date.weekday) ?? false;
      }
      return false;
    }).toList();
  }

  Future<void> markCompletionOnDate(
    Habit habit,
    DateTime date, {
    UserProvider? userProvider,
  }) async {
    if (!isCompletedOn(habit, date)) {
      habit.completions.add(date);
      userProvider?.addXp(ExperienceEngine.xpPerHabit);
      await saveHabit(habit);
    }
  }

  // Unmark a habit completion on a specific date
  Future<void> unmarkCompletionOnDate(
    Habit habit,
    DateTime date, {
    UserProvider? userProvider,
  }) async {
    if (isCompletedOn(habit, date)) {
      habit.completions.removeWhere((c) => AppDateUtils.isSameDay(c, date));
      userProvider?.addXp(-ExperienceEngine.xpPerHabit);
      await saveHabit(habit);
    }
  }

  // Get completion history sorted by date (most recent first)
  List<DateTime> getCompletionHistory(Habit habit, {int daysBack = 90}) {
    final cutoffDate = DateTime.now().subtract(Duration(days: daysBack));
    return habit.completions.where((c) => c.isAfter(cutoffDate)).toList()
      ..sort((a, b) => b.compareTo(a)); // Most recent first
  }
}
