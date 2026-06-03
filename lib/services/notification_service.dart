import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'dart:io';

class NotificationService {
  // Singleton pattern
  static final NotificationService instance = NotificationService._internal();
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();
  bool _isInitialized = false;

  // Reserved IDs to prevent collisions
  static const int dailyMotivationId = 9999;
  static const int endOfDayCheckupId = 9998;
  static const int focusTimerId = 9997;

  Future<void> init() async {
    if (_isInitialized) return;

    if (kIsWeb) {
      _isInitialized = true;
      return;
    }

    tz.initializeTimeZones();
    final TimezoneInfo timeZoneName = await FlutterTimezone.getLocalTimezone();
    tz.setLocalLocation(tz.getLocation(timeZoneName.identifier));

    const AndroidInitializationSettings androidSettings =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    const InitializationSettings initSettings = InitializationSettings(
      android: androidSettings,
    );

    await _notificationsPlugin.initialize(settings: initSettings);

    if (Platform.isAndroid) {
      final androidImplementation = _notificationsPlugin
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >();

      await androidImplementation?.requestNotificationsPermission();
      await androidImplementation?.requestExactAlarmsPermission();
    }

    _isInitialized = true;
  }

  // --- HELPER: GENERATE PREDICTABLE IDs ---
  // We use prefixes so a task and a habit with similar data don't collide
  int _generateId(String stringId, String prefix) {
    return ('${prefix}_$stringId').hashCode;
  }

  int _habitReminderId(String habitId, {int? weekday}) {
    final suffix = weekday == null ? 'daily' : 'weekday_$weekday';
    return _generateId('${habitId}_$suffix', 'habit_time');
  }

  tz.TZDateTime _nextReminderDate({
    required tz.TZDateTime now,
    required TimeOfDay time,
    int? weekday,
  }) {
    final scheduledDate = weekday == null
        ? tz.TZDateTime(
            tz.local,
            now.year,
            now.month,
            now.day,
            time.hour,
            time.minute,
          )
        : tz.TZDateTime(
            tz.local,
            now.year,
            now.month,
            now.day + ((weekday - now.weekday + 7) % 7),
            time.hour,
            time.minute,
          );

    if (scheduledDate.isBefore(now)) {
      return weekday == null
          ? scheduledDate.add(const Duration(days: 1))
          : scheduledDate.add(const Duration(days: 7));
    }

    return scheduledDate;
  }

  // --- TASKS ---
  Future<void> scheduleTaskReminder({
    required int notificationId,
    required String title,
    required String body,
    required DateTime scheduledTime,
  }) async {
    if (kIsWeb || scheduledTime.isBefore(DateTime.now())) return;

    await _notificationsPlugin.zonedSchedule(
      id: notificationId,
      title: title,
      body: body,
      scheduledDate: tz.TZDateTime.from(scheduledTime, tz.local),
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          'task_reminders_channel',
          'Task Reminders',
          channelDescription: 'Notifications for your scheduled tasks',
          importance: Importance.max,
          priority: Priority.high,
          icon: '@mipmap/ic_launcher',
          category: AndroidNotificationCategory.reminder,
        ),
      ),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
    );
  }

  // --- HABIT SPECIFIC TIMES ---
  // Schedules a reminder for a habit at its designated time (e.g., 17:00)
  Future<void> scheduleHabitReminder({
    required String habitId,
    required String habitName,
    required TimeOfDay time,
    List<int>? targetWeekdays,
  }) async {
    if (kIsWeb) return;

    await cancelHabitReminder(habitId);

    final now = tz.TZDateTime.now(tz.local);
    final weekdays = targetWeekdays?.toSet().toList();
    weekdays?.sort();
    final reminderDays = (weekdays == null || weekdays.isEmpty)
        ? <int?>[null]
        : weekdays.cast<int?>();

    for (final weekday in reminderDays) {
      final scheduledDate = _nextReminderDate(
        now: now,
        time: time,
        weekday: weekday,
      );

      await _notificationsPlugin.zonedSchedule(
        id: _habitReminderId(habitId, weekday: weekday),
        title: 'Habit Reminder',
        body: 'Time to: $habitName',
        scheduledDate: scheduledDate,
        notificationDetails: const NotificationDetails(
          android: AndroidNotificationDetails(
            'habit_reminders_channel',
            'Habit Reminders',
            channelDescription: 'Specific time reminders for your habits',
            importance: Importance.high,
            priority: Priority.high,
            icon: '@mipmap/ic_launcher',
            category: AndroidNotificationCategory.reminder,
          ),
        ),
        androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
        matchDateTimeComponents: weekday == null
            ? DateTimeComponents.time
            : DateTimeComponents.dayOfWeekAndTime,
      );
    }
  }

  // --- DAILY MOTIVATION (WITH DYNAMIC HABITS) ---
  // Called every time the app opens or habits change to refresh tomorrow's message
  Future<void> scheduleDailyMotivation({
    required TimeOfDay time,
    bool includeHabits = true,
    List<String> todaysHabits = const [],
  }) async {
    if (kIsWeb) return;

    final now = tz.TZDateTime.now(tz.local);
    tz.TZDateTime scheduledDate = tz.TZDateTime(
      tz.local,
      now.year,
      now.month,
      now.day,
      time.hour,
      time.minute,
    );

    if (scheduledDate.isBefore(now)) {
      scheduledDate = scheduledDate.add(const Duration(days: 1));
    }

    final quotes = [
      "Conquer your day!",
      "Small steps lead to big results. Keep going!",
      "What's on the agenda today? Let's make it happen.",
    ];
    String body = quotes[now.day % quotes.length];

    // Dynamically append habits if requested!
    if (includeHabits && todaysHabits.isNotEmpty) {
      String habitList = todaysHabits.take(2).join(", ");
      if (todaysHabits.length > 2) habitList += " and more";
      body += "\nDon't forget to do $habitList today!";
    }

    await _notificationsPlugin.zonedSchedule(
      id: dailyMotivationId,
      title: 'Good Morning!',
      body: body,
      scheduledDate: scheduledDate,
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          'daily_motivation_channel',
          'Daily Motivation',
          channelDescription: 'Your daily morning boost',
          styleInformation: BigTextStyleInformation(
            '',
          ), // Allows multi-line text
          icon: '@mipmap/ic_launcher',
          category: AndroidNotificationCategory.reminder,
        ),
      ),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      matchDateTimeComponents: DateTimeComponents.time,
    );
  }

  // --- END OF DAY CHECKUP ---
  Future<void> scheduleEndOfDayCheckup({required TimeOfDay time}) async {
    if (kIsWeb) return;

    final now = tz.TZDateTime.now(tz.local);
    tz.TZDateTime scheduledDate = tz.TZDateTime(
      tz.local,
      now.year,
      now.month,
      now.day,
      time.hour,
      time.minute,
    );

    if (scheduledDate.isBefore(now)) {
      scheduledDate = scheduledDate.add(const Duration(days: 1));
    }

    await _notificationsPlugin.zonedSchedule(
      id: endOfDayCheckupId,
      title: 'Evening Check-in',
      body: 'Did you complete all your habits today? Tap to log them!',
      scheduledDate: scheduledDate,
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          'evening_checkup_channel',
          'Evening Checkup',
          channelDescription: 'End of day reminders to log habits',
          icon: '@mipmap/ic_launcher',
          category: AndroidNotificationCategory.reminder,
        ),
      ),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      matchDateTimeComponents: DateTimeComponents.time,
    );
  }

  // --- CANCEL CAPABILITIES ---
  Future<void> cancelNotification(int notificationId) async {
    if (kIsWeb) return;
    await _notificationsPlugin.cancel(id: notificationId);
  }

  Future<void> cancelHabitReminder(String habitId) async {
    if (kIsWeb) return;

    await _notificationsPlugin.cancel(id: _habitReminderId(habitId));
    for (var weekday = 1; weekday <= 7; weekday++) {
      await _notificationsPlugin.cancel(
        id: _habitReminderId(habitId, weekday: weekday),
      );
    }
  }

Future<void> showFocusTimerNotification({
    required String title,
    required String body,
    required DateTime targetTime,
    required bool isStopwatch,
    required Color notificationColor,
    bool isPaused = false,
  }) async {
    if (kIsWeb) return;

    await _notificationsPlugin.show(
      id: focusTimerId,
      title: title,
      body: body,
      notificationDetails: NotificationDetails(
        android: AndroidNotificationDetails(
          'focus_timer_channel',
          'Active Focus Timer',
          channelDescription:
              'Persistent notification for active focus sessions',
          importance: Importance.low,
          priority: Priority.high,
          silent: true,
          ongoing: true,
          autoCancel: false,
          usesChronometer: !isPaused,
          chronometerCountDown: !isStopwatch,
          when: targetTime.millisecondsSinceEpoch,
          color: notificationColor,
          colorized: true,
          icon: '@mipmap/ic_launcher',
          category: isStopwatch
              ? AndroidNotificationCategory.stopwatch
              : AndroidNotificationCategory.status,
          onlyAlertOnce: true,
        ),
      ),
    );
  }

  // Removes the pinned notification when stopped
  Future<void> cancelFocusTimerNotification() async {
    if (kIsWeb) return;
    await _notificationsPlugin.cancel(id: focusTimerId);
  }
}
