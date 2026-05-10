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
    const DarwinInitializationSettings iosSettings =
        DarwinInitializationSettings();

    const InitializationSettings initSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _notificationsPlugin.initialize(settings: initSettings);

    if (Platform.isAndroid) {
      await _notificationsPlugin
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >()
          ?.requestNotificationsPermission();
    } else if (Platform.isIOS) {
      await _notificationsPlugin
          .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin
          >()
          ?.requestPermissions(alert: true, badge: true, sound: true);
    }

    _isInitialized = true;
  }

  // --- HELPER: GENERATE PREDICTABLE IDs ---
  // We use prefixes so a task and a habit with similar data don't collide
  int _generateId(String stringId, String prefix) {
    return ('${prefix}_$stringId').hashCode;
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
        ),
        iOS: DarwinNotificationDetails(),
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

    await _notificationsPlugin.zonedSchedule(
      id: _generateId(habitId, 'habit_time'),
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
        ),
        iOS: DarwinNotificationDetails(),
      ),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      matchDateTimeComponents:
          DateTimeComponents.time, // Repeats daily at this time!
    );
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
      body += "\nDon't forget to $habitList today!";
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
        ),
        iOS: DarwinNotificationDetails(),
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
        ),
        iOS: DarwinNotificationDetails(),
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
    await _notificationsPlugin.cancel(id: _generateId(habitId, 'habit_time'));
  }

  // Shows a pinned notification that natively ticks up or down!
  Future<void> showFocusTimerNotification({
    required String phaseName,
    required DateTime targetTime,
    required bool isStopwatch,
    bool isPaused = false,
    String? pausedText,
  }) async {
    if (kIsWeb) return;

    await _notificationsPlugin.show(
      id: focusTimerId,
      title: isPaused ? 'Focus Paused' : 'Focus Active',
      body: isPaused
          ? 'Phase: $phaseName | $pausedText'
          : 'Current Phase: $phaseName',
      notificationDetails: NotificationDetails(
        android: AndroidNotificationDetails(
          'focus_timer_channel',
          'Active Focus Timer',
          channelDescription:
              'Persistent notification for active focus sessions',
          importance: Importance
              .low, // Low importance so it doesn't pop-up and interrupt
          priority: Priority.low,
          ongoing: true, // User cannot swipe it away
          autoCancel: false,
          usesChronometer: !isPaused, // Android natively ticks the timer
          chronometerCountDown:
              !isStopwatch, // Ticks down for timers, up for stopwatches
          when: targetTime.millisecondsSinceEpoch,
          icon: '@mipmap/ic_launcher',
        ),
        iOS: const DarwinNotificationDetails(),
      ),
    );
  }

  // Removes the pinned notification when stopped
  Future<void> cancelFocusTimerNotification() async {
    if (kIsWeb) return;
    await _notificationsPlugin.cancel(id: focusTimerId);
  }
}
