import 'package:flutter/material.dart';
import 'package:timety/providers/settings_provider.dart';
import '../data/habit/habit_models.dart';
import '../data/habit/habit_repository.dart';
import '../services/notification_service.dart';

class HabitProvider extends ChangeNotifier {
  final HabitRepository repository;
  List<Habit> _habits = [];
  SettingsProvider? _settings;

  List<Habit> get habits => _habits;

  HabitProvider({required this.repository}) {
    _loadHabits();
  }

  void updateSettings(SettingsProvider settings) {
    _settings = settings;
    syncNotifications();
  }

  Future<void> _loadHabits() async {
    _habits = await repository.fetchHabits();
    syncNotifications();
    notifyListeners();
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
    notifyListeners();
  }

  Future<void> deleteHabit(String id) async {
    await repository.deleteHabit(id);
    _habits.removeWhere((h) => h.id == id);
    syncNotifications();
    notifyListeners();
  }

  void syncNotifications() {
    if (_settings == null) return;

    // 1. General App Notifications
    if (_settings!.notificationsEnabled) {
      // Get today's habit names for the dynamic motivation message
      final todaysHabits = getHabitsForDay(
        DateTime.now(),
      ).map((h) => h.name).toList();

      NotificationService.instance.scheduleDailyMotivation(
        time: _settings!.notificationTime,
        includeHabits: true,
        todaysHabits: todaysHabits,
      );

      // Schedule a static evening checkup at 8:00 PM (20:00)
      NotificationService.instance.scheduleEndOfDayCheckup(
        time: const TimeOfDay(hour: 20, minute: 0),
      );
    } else {
      // User turned off notifications in settings, so wipe them!
      NotificationService.instance.cancelNotification(
        NotificationService.dailyMotivationId,
      );
      NotificationService.instance.cancelNotification(
        NotificationService.endOfDayCheckupId,
      );
    }

    // 2. Specific Habit Reminders
    for (var habit in _habits) {
      // If notifications are ON and the habit has a specific time
      if (_settings!.notificationsEnabled && habit.targetTime != null) {
        NotificationService.instance.scheduleHabitReminder(
          habitId: habit.id,
          habitName: habit.name,
          time: habit.targetTime!,
        );
      } else {
        // Otherwise, make sure no ghost alarms exist for this habit
        NotificationService.instance.cancelHabitReminder(habit.id);
      }
    }
  }

  // --- CORE HABIT LOGIC ---

  bool _isSameDay(DateTime a, DateTime b) {
    return a.year == b.year && a.month == b.month && a.day == b.day;
  }

  // Checks if a habit was completed on a specific day
  bool isCompletedOn(Habit habit, DateTime date) {
    return habit.completions.any((completion) => _isSameDay(completion, date));
  }

  // Toggles completion for today
  Future<void> toggleCompletionToday(Habit habit) async {
    final today = DateTime.now();
    final completed = isCompletedOn(habit, today);

    if (completed) {
      // Remove today's completion
      habit.completions.removeWhere((c) => _isSameDay(c, today));
    } else {
      // Add completion
      habit.completions.add(today);
    }

    await saveHabit(habit);
  }

  // Helper for WeeklyFlexible: How many times have they done it this week?
  int getCompletionsThisWeek(Habit habit) {
    final now = DateTime.now();
    // Get start of the current week (Monday)
    final startOfWeek = DateTime(
      now.year,
      now.month,
      now.day,
    ).subtract(Duration(days: now.weekday - 1));
    final endOfWeek = startOfWeek.add(
      const Duration(days: 6, hours: 23, minutes: 59),
    );

    return habit.completions
        .where((c) => c.isAfter(startOfWeek) && c.isBefore(endOfWeek))
        .length;
  }

  // Helper to filter habits that are scheduled for a specific day
  List<Habit> getHabitsForDay(DateTime date) {
    return _habits.where((habit) {
      if (habit.frequency == HabitFrequency.daily) return true;
      if (habit.frequency == HabitFrequency.weeklyFlexible) {
        return true; // Flexible habits show up every day until weekly goal is met
      }
      if (habit.frequency == HabitFrequency.weeklyExact) {
        return habit.targetWeekdays?.contains(date.weekday) ?? false;
      }
      return false;
    }).toList();
  }
}
