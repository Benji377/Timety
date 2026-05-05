import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingsProvider extends ChangeNotifier {
  SharedPreferences? _prefs;

  // Default Values
  ThemeMode _themeMode = ThemeMode.system;
  Color _seedColor = Colors.blue;
  bool _notificationsEnabled = true;
  TimeOfDay _notificationTime = const TimeOfDay(hour: 8, minute: 0);
  int _dailyGoalMins = 90;
  int _maxStopwatchMins = 120;
  int _maxNodeMins = 240;

  // Getters
  ThemeMode get themeMode => _themeMode;
  Color get seedColor => _seedColor;
  bool get notificationsEnabled => _notificationsEnabled;
  TimeOfDay get notificationTime => _notificationTime;
  int get dailyGoalMins => _dailyGoalMins;
  int get maxStopwatchMins => _maxStopwatchMins;
  int get maxNodeMins => _maxNodeMins;

  SettingsProvider() {
    _loadSettings();
  }

  // --- INIT / LOAD ---
  Future<void> _loadSettings() async {
    _prefs = await SharedPreferences.getInstance();
    
    // Theme
    final themeIndex = _prefs?.getInt('themeMode') ?? ThemeMode.system.index;
    _themeMode = ThemeMode.values[themeIndex];
    
    final colorValue = _prefs?.getInt('seedColor') ?? Colors.blue.value;
    _seedColor = Color(colorValue);

    // Notifications
    _notificationsEnabled = _prefs?.getBool('notificationsEnabled') ?? true;
    final notifHour = _prefs?.getInt('notificationHour') ?? 8;
    final notifMin = _prefs?.getInt('notificationMin') ?? 0;
    _notificationTime = TimeOfDay(hour: notifHour, minute: notifMin);

    // Limits
    _dailyGoalMins = _prefs?.getInt('dailyGoalMins') ?? 90;
    _maxStopwatchMins = _prefs?.getInt('maxStopwatchMins') ?? 120;
    _maxNodeMins = _prefs?.getInt('maxNodeMins') ?? 240;

    notifyListeners();
  }

  // --- SETTERS / SAVE ---
  void setThemeMode(ThemeMode mode) {
    _themeMode = mode;
    _prefs?.setInt('themeMode', mode.index);
    notifyListeners();
  }

  void setSeedColor(Color color) {
    _seedColor = color;
    _prefs?.setInt('seedColor', color.value);
    notifyListeners();
  }

  void setNotificationsEnabled(bool enabled) {
    _notificationsEnabled = enabled;
    _prefs?.setBool('notificationsEnabled', enabled);
    notifyListeners();
  }

  void setNotificationTime(TimeOfDay time) {
    _notificationTime = time;
    _prefs?.setInt('notificationHour', time.hour);
    _prefs?.setInt('notificationMin', time.minute);
    notifyListeners();
  }

  void setDailyGoal(int mins) {
    _dailyGoalMins = mins;
    _prefs?.setInt('dailyGoalMins', mins);
    notifyListeners();
  }

  void setMaxStopwatch(int mins) {
    _maxStopwatchMins = mins;
    _prefs?.setInt('maxStopwatchMins', mins);
    notifyListeners();
  }

  void setMaxNode(int mins) {
    _maxNodeMins = mins;
    _prefs?.setInt('maxNodeMins', mins);
    notifyListeners();
  }
}