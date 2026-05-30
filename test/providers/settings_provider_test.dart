import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:timety/providers/settings_provider.dart';

import '../test_support/fakes.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({
      'themeMode': ThemeMode.dark.index,
      'notificationHour': 9,
      'notificationMin': 45,
      'endOfDayHour': 21,
      'endOfDayMin': 15,
      'dailyGoalMins': 120,
      'maxStopwatchMins': 180,
      'maxNodeMins': 300,
      'autoCompleteFocusTargetOnFinish': true,
    });
  });

  test('loads persisted values and saves later updates', () async {
    final settings = SettingsProvider();
    await drainEventQueue();

    expect(settings.themeMode, ThemeMode.dark);
    expect(settings.notificationTime, const TimeOfDay(hour: 9, minute: 45));
    expect(settings.endOfDayTime, const TimeOfDay(hour: 21, minute: 15));
    expect(settings.dailyGoalMins, 120);
    expect(settings.maxStopwatchMins, 180);
    expect(settings.maxNodeMins, 300);
    expect(settings.autoCompleteFocusTargetOnFinish, isTrue);

    settings.setThemeMode(ThemeMode.light);
    settings.setNotificationTime(const TimeOfDay(hour: 7, minute: 5));
    settings.setEndOfDayTime(const TimeOfDay(hour: 19, minute: 30));
    settings.setDailyGoal(95);
    settings.setMaxStopwatch(90);
    settings.setMaxNode(210);
    settings.setAutoCompleteFocusTargetOnFinish(false);

    final prefs = await SharedPreferences.getInstance();

    expect(prefs.getInt('themeMode'), ThemeMode.light.index);
    expect(prefs.getInt('notificationHour'), 7);
    expect(prefs.getInt('notificationMin'), 5);
    expect(prefs.getInt('endOfDayHour'), 19);
    expect(prefs.getInt('endOfDayMin'), 30);
    expect(prefs.getInt('dailyGoalMins'), 95);
    expect(prefs.getInt('maxStopwatchMins'), 90);
    expect(prefs.getInt('maxNodeMins'), 210);
    expect(prefs.getBool('autoCompleteFocusTargetOnFinish'), isFalse);
  });
}
