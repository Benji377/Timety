import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:flutter/foundation.dart';
import 'dart:io';

class NotificationService {
  // Singleton pattern
  static final NotificationService instance = NotificationService._internal();
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notificationsPlugin =
      FlutterLocalNotificationsPlugin();
  bool _isInitialized = false;

  Future<void> init() async {
    if (_isInitialized) return;

    // --- WEB GUARD ---
    // If we are running on Chrome/Web for UI testing, skip the mobile notification setup
    if (kIsWeb) {
      _isInitialized = true;
      return;
    }

    // 1. Initialize Timezones
    tz.initializeTimeZones();
    final TimezoneInfo timeZoneName = await FlutterTimezone.getLocalTimezone();
    tz.setLocalLocation(tz.getLocation(timeZoneName.identifier));

    // 2. Android Initialization Settings
    const AndroidInitializationSettings androidSettings =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    // 3. iOS Initialization Settings
    const DarwinInitializationSettings iosSettings =
        DarwinInitializationSettings(
          requestAlertPermission: true,
          requestBadgePermission: true,
          requestSoundPermission: true,
        );

    const InitializationSettings initSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _notificationsPlugin.initialize(settings: initSettings);

    // Request permissions for Android 13+
    if (Platform.isAndroid) {
      await _notificationsPlugin
          .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin
          >()
          ?.requestNotificationsPermission();
    }

    _isInitialized = true;
  }

  // --- MODULAR CAPABILITIES ---

  /// Schedules a specific reminder for a task
  Future<void> scheduleTaskReminder({
    required int notificationId, // Plugin requires int IDs
    required String title,
    required String body,
    required DateTime scheduledTime,
  }) async {
    // --- WEB GUARD ---
    if (kIsWeb) return;

    // Don't schedule in the past
    if (scheduledTime.isBefore(DateTime.now())) return;

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

  /// Cancels a specific notification
  Future<void> cancelNotification(int notificationId) async {
    // --- WEB GUARD ---
    if (kIsWeb) return;

    await _notificationsPlugin.cancel(id: notificationId);
  }

  /// Future implementation: Daily Motivation
  Future<void> scheduleDailyMotivation() async {
    // --- WEB GUARD ---
    if (kIsWeb) return;

    // 1. Set the time you want it to fire (e.g., 8:00 AM)
    final now = tz.TZDateTime.now(tz.local);
    tz.TZDateTime scheduledDate = tz.TZDateTime(
      tz.local,
      now.year,
      now.month,
      now.day,
      8,
      0,
    );

    // If it's already past 8:00 AM today, schedule for tomorrow
    if (scheduledDate.isBefore(now)) {
      scheduledDate = scheduledDate.add(const Duration(days: 1));
    }

    // 2. A fun list of rotating quotes!
    final quotes = [
      "Conquer your day! 🚀",
      "Small steps lead to big results. Keep going! 💪",
      "What's on the agenda today? Let's make it happen.",
      "Clear your mind, organize your tasks.",
      "You have a 100% track record of surviving bad days.",
    ];
    // Use the day of the year to pick a pseudo-random quote that changes daily
    final quote = quotes[now.day % quotes.length];

    // 3. Schedule the recurring alarm
    await _notificationsPlugin.zonedSchedule(
      id: 9999, // Reserved ID for daily motivation
      title: 'Good Morning!',
      body: quote,
      scheduledDate: scheduledDate,
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          'daily_motivation_channel',
          'Daily Motivation',
          channelDescription: 'Your daily morning boost',
          importance: Importance
              .defaultImportance, // Doesn't need to be MAX like a task alarm
          priority: Priority.defaultPriority,
          icon: '@mipmap/ic_launcher',
        ),
        iOS: DarwinNotificationDetails(),
      ),
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
      matchDateTimeComponents: DateTimeComponents.time,
    );
  }
}
