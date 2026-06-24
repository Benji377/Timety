import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:timety/services/notification_service.dart';
import 'package:timety/l10n/app_localizations.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('NotificationService Tests', () {
    final List<MethodCall> timerLog = <MethodCall>[];
    final List<MethodCall> flutterLocalNotificationsLog = <MethodCall>[];
    late AppLocalizations l10n;

    setUpAll(() async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        const MethodChannel('com.timety/timer_notification'),
        (MethodCall methodCall) async {
          timerLog.add(methodCall);
          return null;
        },
      );

      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        const MethodChannel('dexterous.com/flutter/local_notifications'),
        (MethodCall methodCall) async {
          flutterLocalNotificationsLog.add(methodCall);
          return null;
        },
      );
    });

    setUp(() {
      timerLog.clear();
      flutterLocalNotificationsLog.clear();
      l10n = lookupAppLocalizations(const Locale('en'));
    });

    test('showFocusTimerNotification calls custom MethodChannel', () async {
      final now = DateTime.now();
      await NotificationService.instance.showFocusTimerNotification(
        title: 'Focus',
        body: 'Pomodoro',
        targetTime: now,
        isStopwatch: false,
        notificationColor: Colors.red,
        l10n: l10n,
        isPaused: true,
      );

      expect(timerLog.length, 1);
      final call = timerLog.first;
      expect(call.method, 'showTimer');
      expect(call.arguments['title'], 'Focus');
      expect(call.arguments['body'], 'Pomodoro');
      expect(call.arguments['targetTimeMs'], now.millisecondsSinceEpoch);
      expect(call.arguments['isStopwatch'], false);
      expect(call.arguments['isPaused'], true);
      expect(call.arguments['color'], Colors.red.toARGB32());
    });

    test('cancelFocusTimerNotification calls custom MethodChannel', () async {
      await NotificationService.instance.cancelFocusTimerNotification();

      expect(timerLog.length, 1);
      final call = timerLog.first;
      expect(call.method, 'cancelTimer');
    });

  });
}
