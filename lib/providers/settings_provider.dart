import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;

class SettingsProvider extends ChangeNotifier {
  SharedPreferences? _prefs;

  // Default Values
  ThemeMode _themeMode = ThemeMode.system;
  TimeOfDay _notificationTime = const TimeOfDay(hour: 8, minute: 0);
  int _dailyGoalMins = 90;
  int _maxStopwatchMins = 120;
  int _maxNodeMins = 240;
  TimeOfDay _endOfDayTime = const TimeOfDay(hour: 20, minute: 0);
  String _locationApiEndpoint = 'https://photon.komoot.io/api/';

  // Getters
  ThemeMode get themeMode => _themeMode;
  TimeOfDay get notificationTime => _notificationTime;
  TimeOfDay get endOfDayTime => _endOfDayTime;
  int get dailyGoalMins => _dailyGoalMins;
  int get maxStopwatchMins => _maxStopwatchMins;
  int get maxNodeMins => _maxNodeMins;
  String get locationApiEndpoint => _locationApiEndpoint;

  SettingsProvider() {
    _loadSettings();
  }

  // --- INIT / LOAD ---
  Future<void> _loadSettings() async {
    _prefs = await SharedPreferences.getInstance();

    // Theme
    final themeIndex = _prefs?.getInt('themeMode') ?? ThemeMode.system.index;
    _themeMode = ThemeMode.values[themeIndex];

    // Notifications
    final notifHour = _prefs?.getInt('notificationHour') ?? 8;
    final notifMin = _prefs?.getInt('notificationMin') ?? 0;
    _notificationTime = TimeOfDay(hour: notifHour, minute: notifMin);
    final eodHour = _prefs?.getInt('endOfDayHour') ?? 20;
    final eodMin = _prefs?.getInt('endOfDayMin') ?? 0;
    _endOfDayTime = TimeOfDay(hour: eodHour, minute: eodMin);

    // Limits
    _dailyGoalMins = _prefs?.getInt('dailyGoalMins') ?? 90;
    _maxStopwatchMins = _prefs?.getInt('maxStopwatchMins') ?? 120;
    _maxNodeMins = _prefs?.getInt('maxNodeMins') ?? 240;

    // API & Services
    _locationApiEndpoint =
        _prefs?.getString('locationApiEndpoint') ??
        'https://photon.komoot.io/api/';

    notifyListeners();
  }

  // --- SETTERS / SAVE ---
  void setThemeMode(ThemeMode mode) {
    _themeMode = mode;
    _prefs?.setInt('themeMode', mode.index);
    notifyListeners();
  }

  void setNotificationTime(TimeOfDay time) {
    _notificationTime = time;
    _prefs?.setInt('notificationHour', time.hour);
    _prefs?.setInt('notificationMin', time.minute);
    notifyListeners();
  }

  void setEndOfDayTime(TimeOfDay time) {
    _endOfDayTime = time;
    _prefs?.setInt('endOfDayHour', time.hour);
    _prefs?.setInt('endOfDayMin', time.minute);
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

  void setLocationApiEndpoint(String endpoint) {
    _locationApiEndpoint = endpoint;
    _prefs?.setString('locationApiEndpoint', endpoint);
    notifyListeners();
  }

  Future<bool> validateLocationApiEndpoint(String url) async {
    if (url.isEmpty) return false;
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      return false;
    }

    try {
      final testUrl = Uri.parse(
        '${url.endsWith('/') ? url : '$url/'}?q=test&limit=1',
      );
      final response = await http.get(
        testUrl,
        headers: {
          'User-Agent': 'timety/1.0 (io.github.benji377.timety)',
          'Accept': 'application/json',
        }
      ).timeout(
            const Duration(seconds: 3),
            onTimeout: () => http.Response('timeout', 408),
          );
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }
}
