import 'focus_models.dart';

abstract class FocusRepository {
  // Modes
  Future<List<FocusMode>> fetchModes();
  Future<void> saveMode(FocusMode mode);
  Future<void> deleteMode(String id);

  // Sessions (History)
  Future<List<FocusSession>> fetchSessions();
  Future<void> saveSession(FocusSession session);
}