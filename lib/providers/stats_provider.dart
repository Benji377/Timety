import 'package:flutter/material.dart';
import '../data/main_repository.dart';
import '../data/focus_session.dart';
import '../data/daily_event.dart';
import '../data/category.dart';
import '../utils/insights_generator.dart';

class StatsProvider with ChangeNotifier {
  final MainRepository _repository;
  
  List<FocusSession> _allSessions = [];
  final List<DailyEvent> _allEvents = [];
  List<Category> _categories = [];
  
  StatsProvider(this._repository) {
    refreshAll();
  }

  List<FocusSession> get allSessions => _allSessions;
  List<DailyEvent> get allEvents => _allEvents;
  List<Category> get categories => _categories;

  Future<void> refreshAll() async {
    _allSessions = await _repository.getAllSessions();
    _categories = await _repository.getAllCategories();
    // For simplicity, we'll fetch events as needed or just all of them
    notifyListeners();
  }

  List<FocusSession> getSessionsForDay(DateTime day) {
    final start = DateTime(day.year, day.month, day.day).millisecondsSinceEpoch;
    final end = DateTime(day.year, day.month, day.day, 23, 59, 59).millisecondsSinceEpoch;
    return _allSessions.where((s) => s.startTime >= start && s.startTime <= end).toList();
  }

  Future<List<DailyEvent>> getEventsForDay(DateTime day) async {
    final start = DateTime(day.year, day.month, day.day).millisecondsSinceEpoch;
    final end = DateTime(day.year, day.month, day.day, 23, 59, 59).millisecondsSinceEpoch;
    return await _repository.getEventsForDay(start, end);
  }

  Map<String, int> getWeeklyFocusData() {
    final now = DateTime.now();
    final data = <String, int>{};
    final dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    for (int i = 6; i >= 0; i--) {
      final day = now.subtract(Duration(days: i));
      final daySessions = getSessionsForDay(day);
      final total = daySessions.fold(0, (sum, s) => sum + s.duration);
      data[dayNames[day.weekday - 1]] = total;
    }
    return data;
  }

  Map<int, int> getCategoryDistribution() {
    final dist = <int, int>{};
    for (var session in _allSessions) {
      dist[session.categoryId] = (dist[session.categoryId] ?? 0) + session.duration;
    }
    return dist;
  }

  List<String> getInsights() {
    return InsightsGenerator.generateInsights(_allSessions);
  }
}
