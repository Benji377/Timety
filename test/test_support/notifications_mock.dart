import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/data/latest_all.dart' as tz;
import 'package:timezone/timezone.dart' as tz;

const MethodChannel localNotificationsChannel = MethodChannel(
  'dexterous.com/flutter/local_notifications',
);

void installLocalNotificationsMock() {
  AndroidFlutterLocalNotificationsPlugin.registerWith();
  TestWidgetsFlutterBinding.ensureInitialized();
  debugDefaultTargetPlatformOverride = TargetPlatform.android;

  tz.initializeTimeZones();
  tz.setLocalLocation(tz.getLocation('UTC'));

  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(localNotificationsChannel, (
        MethodCall methodCall,
      ) async {
        switch (methodCall.method) {
          case 'initialize':
            return true;
          case 'pendingNotificationRequests':
          case 'getActiveNotifications':
            return <Map<String, Object?>>[];
          case 'getNotificationAppLaunchDetails':
            return null;
          default:
            return null;
        }
      });
}

void clearLocalNotificationsMock() {
  TestWidgetsFlutterBinding.ensureInitialized();

  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(localNotificationsChannel, null);
  debugDefaultTargetPlatformOverride = null;
}
