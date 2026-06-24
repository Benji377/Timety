import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'dart:io';
import '../l10n/app_localizations.dart';
import 'package:flutter/services.dart';

class NotificationService {
  // Singleton pattern
  static final NotificationService instance = NotificationService._internal();
  NotificationService._internal();

  static const MethodChannel _timerChannel = MethodChannel('com.timety/timer_notification');

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
    required AppLocalizations l10n,
  }) async {
    if (kIsWeb || scheduledTime.isBefore(DateTime.now())) return;

    await _notificationsPlugin.zonedSchedule(
      id: notificationId,
      title: title,
      body: body,
      scheduledDate: tz.TZDateTime.from(scheduledTime, tz.local),
      notificationDetails: NotificationDetails(
        android: AndroidNotificationDetails(
          'task_reminders_channel',
          l10n.notificationChannelTasksName,
          channelDescription: l10n.notificationChannelTasksDesc,
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
  Future<void> scheduleHabitReminder({
    required String habitId,
    required String habitName,
    required String title,
    required String body,
    required TimeOfDay time,
    required AppLocalizations l10n,
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
        title: title,
        body: body,
        scheduledDate: scheduledDate,
        notificationDetails: NotificationDetails(
          android: AndroidNotificationDetails(
            'habit_reminders_channel',
            l10n.notificationChannelHabitsName,
            channelDescription: l10n.notificationChannelHabitsDesc,
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
  Future<void> scheduleDailyMotivation({
    required TimeOfDay time,
    required AppLocalizations l10n,
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

    // Group the localized quotes back into a list
    final quotes = [
      l10n.notificationQuote1,
      l10n.notificationQuote2,
      l10n.notificationQuote3,
    ];
    String body = quotes[now.day % quotes.length];

    // Dynamically append habits
    if (includeHabits && todaysHabits.isNotEmpty) {
      String habitList = todaysHabits.take(2).join(", ");
      if (todaysHabits.length > 2) {
        habitList += l10n.notificationHabitListSuffix;
      }
      body += l10n.notificationHabitReminder(habitList);
    }

    await _notificationsPlugin.zonedSchedule(
      id: dailyMotivationId,
      title: l10n.notificationGoodMorning,
      body: body,
      scheduledDate: scheduledDate,
      notificationDetails: NotificationDetails(
        android: AndroidNotificationDetails(
          'daily_motivation_channel',
          l10n.notificationChannelMotivationName,
          channelDescription: l10n.notificationChannelMotivationDesc,
          styleInformation: const BigTextStyleInformation(''),
          icon: '@mipmap/ic_launcher',
          category: AndroidNotificationCategory.reminder,
        ),
      ),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      matchDateTimeComponents: DateTimeComponents.time,
    );
  }

  // --- END OF DAY CHECKUP ---
  Future<void> scheduleEndOfDayCheckup({
    required TimeOfDay time,
    required AppLocalizations l10n,
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
      id: endOfDayCheckupId,
      title: l10n.notificationEveningTitle,
      body: l10n.notificationEveningBody,
      scheduledDate: scheduledDate,
      notificationDetails: NotificationDetails(
        android: AndroidNotificationDetails(
          'evening_checkup_channel',
          l10n.notificationChannelEveningName,
          channelDescription: l10n.notificationChannelEveningDesc,
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
    required AppLocalizations l10n,
    bool isPaused = false,
  }) async {
    if (kIsWeb) return;

    try {
      await _timerChannel.invokeMethod('showTimer', {
        'title': title,
        'body': body,
        'targetTimeMs': targetTime.millisecondsSinceEpoch,
        'isStopwatch': isStopwatch,
        'isPaused': isPaused,
        'color': notificationColor.toARGB32(),
      });
    } catch (e) {
      debugPrint('Failed to show custom timer notification: $e');
    }
  }

  // Removes the pinned notification when stopped
  Future<void> cancelFocusTimerNotification() async {
    if (kIsWeb) return;
    
    try {
      await _timerChannel.invokeMethod('cancelTimer');
    } catch (e) {
      debugPrint('Failed to cancel custom timer notification: $e');
    }
  }
}
