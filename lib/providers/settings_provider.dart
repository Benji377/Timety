import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;
import 'package:intl/intl.dart';

class SettingsProvider extends ChangeNotifier {
  SharedPreferences? _prefs;

  // Default Values
  ThemeMode _themeMode = ThemeMode.system;
  TimeOfDay _notificationTime = const TimeOfDay(hour: 8, minute: 0);
  int _dailyGoalMins = 90;
  int _maxStopwatchMins = 120;
  int _maxNodeMins = 240;
  int _upcomingTasksDays = 7;
  TimeOfDay _endOfDayTime = const TimeOfDay(hour: 20, minute: 0);
  String _locationApiEndpoint = 'https://photon.komoot.io/api/';
  bool _autoCompleteFocusTargetOnFinish = false;
  String? _appLocaleCode;
  bool _use24HourFormat = true;
  String? _dateFormatCode;

  // Getters
  ThemeMode get themeMode => _themeMode;
  TimeOfDay get notificationTime => _notificationTime;
  TimeOfDay get endOfDayTime => _endOfDayTime;
  int get dailyGoalMins => _dailyGoalMins;
  int get maxStopwatchMins => _maxStopwatchMins;
  int get maxNodeMins => _maxNodeMins;
  int get upcomingTasksDays => _upcomingTasksDays;
  String get locationApiEndpoint => _locationApiEndpoint;
  bool get autoCompleteFocusTargetOnFinish => _autoCompleteFocusTargetOnFinish;
  Locale? get appLocale => _appLocaleCode != null && _appLocaleCode != 'system'
      ? Locale(_appLocaleCode!)
      : null;
  String get appLocaleCode => _appLocaleCode ?? 'system';
  bool get use24HourFormat => _use24HourFormat;
  String get dateFormatCode => _dateFormatCode ?? 'system';

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
    _upcomingTasksDays = _prefs?.getInt('upcomingTasksDays') ?? 7;
    _autoCompleteFocusTargetOnFinish =
        _prefs?.getBool('autoCompleteFocusTargetOnFinish') ?? false;

    // API & Services
    _locationApiEndpoint =
        _prefs?.getString('locationApiEndpoint') ??
        'https://photon.komoot.io/api/';

    // Locale & Format
    _appLocaleCode = _prefs?.getString('appLocaleCode');
    _use24HourFormat = _prefs?.getBool('use24HourFormat') ?? true;
    _dateFormatCode = _prefs?.getString('dateFormatCode');

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

  void setUpcomingTasksDays(int days) {
    _upcomingTasksDays = days;
    _prefs?.setInt('upcomingTasksDays', days);
    notifyListeners();
  }

  void setLocationApiEndpoint(String endpoint) {
    _locationApiEndpoint = endpoint;
    _prefs?.setString('locationApiEndpoint', endpoint);
    notifyListeners();
  }

  void setAutoCompleteFocusTargetOnFinish(bool enabled) {
    _autoCompleteFocusTargetOnFinish = enabled;
    _prefs?.setBool('autoCompleteFocusTargetOnFinish', enabled);
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
      final response = await http
          .get(
            testUrl,
            headers: {
              'User-Agent': 'timety/1.0 (io.github.benji377.timety)',
              'Accept': 'application/json',
            },
          )
          .timeout(
            const Duration(seconds: 3),
            onTimeout: () => http.Response('timeout', 408),
          );
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  void setAppLocaleCode(String code) {
    _appLocaleCode = code == 'system' ? null : code;
    if (_appLocaleCode == null) {
      _prefs?.remove('appLocaleCode'); // Let OS decide
    } else {
      _prefs?.setString('appLocaleCode', _appLocaleCode!);
    }
    notifyListeners();
  }

  void set24HourFormat(bool use24Hour) {
    _use24HourFormat = use24Hour;
    _prefs?.setBool('use24HourFormat', use24Hour);
    notifyListeners();
  }

  void setDateFormatCode(String code) {
    _dateFormatCode = code == 'system' ? null : code;

    if (_dateFormatCode == null) {
      _prefs?.remove('dateFormatCode');
    } else {
      _prefs?.setString('dateFormatCode', _dateFormatCode!);
    }
    notifyListeners();
  }

  // Helpers
  String getFormattedDate(DateTime date) {
    if (_dateFormatCode != null) {
      // User explicitly picked a format
      return DateFormat(_dateFormatCode).format(date);
    } else {
      // System Default: Grab the current app language/locale and format automatically
      final localeString = _appLocaleCode ?? ui.PlatformDispatcher.instance.locale.toString();
      return DateFormat.yMd(localeString).format(date);
    }
  }
}
