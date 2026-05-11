import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

const MethodChannel localNotificationsChannel = MethodChannel(
  'dexterous.com/flutter/local_notifications',
);

void installLocalNotificationsMock() {
  TestWidgetsFlutterBinding.ensureInitialized();

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
}
