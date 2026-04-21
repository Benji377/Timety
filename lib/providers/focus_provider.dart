import 'package:flutter/material.dart';
import '../data/main_repository.dart';
import '../data/focus_session.dart';
import '../data/focus_mode.dart';

class FocusProvider with ChangeNotifier {
  final MainRepository _repository;
  List<FocusSession> _sessions = [];
  List<FocusMode> _focusModes = [];

  FocusProvider(this._repository) {
    refreshAll();
  }

  List<FocusSession> get sessions => _sessions;
  List<FocusMode> get focusModes => _focusModes;

  Future<void> refreshAll() async {
    _sessions = await _repository.getAllSessions();
    _focusModes = await _repository.getAllFocusModes();
    notifyListeners();
  }

  Future<void> addSession(FocusSession session) async {
    await _repository.insertSession(session);
    await refreshAll();
  }

  Future<void> addFocusMode(FocusMode mode) async {
    await _repository.insertFocusMode(mode);
    await refreshAll();
  }

  Future<void> updateFocusMode(FocusMode mode) async {
    await _repository.updateFocusMode(mode);
    await refreshAll();
  }

  Future<void> deleteFocusMode(int id) async {
    await _repository.deleteFocusMode(id);
    await refreshAll();
  }

  List<FocusSession> getSessionsForTask(int taskId) {
    return _sessions.where((s) => s.taskId == taskId).toList();
  }

  List<FocusSession> getSessionsForDay(DateTime day) {
    final startOfDay = DateTime(day.year, day.month, day.day);
    final endOfDay = DateTime(day.year, day.month, day.day, 23, 59, 59);
    return _sessions.where((s) {
      final start = DateTime.fromMillisecondsSinceEpoch(s.startTime);
      return start.isAfter(startOfDay) && start.isBefore(endOfDay);
    }).toList();
  }
}
