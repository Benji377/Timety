import './focus_models.dart';

/// Defines the contract for local storage operations regarding focus sessions.
abstract class FocusRepository {
  /// Fetches all available focus nodes, including system-defined and user-defined ones.
  Future<List<FocusMode>> fetchModes();

  /// Saves a focus mode. If the mode has an existing ID, it will be updated; otherwise, a new entry will be created.
  Future<void> saveMode(FocusMode mode);

  /// Deletes a focus mode by its unique identifier. System-defined modes cannot be deleted.
  Future<void> deleteMode(String id);

  /// Fetches all focus sessions, not necessarily in chronological order.
  Future<List<FocusSession>> fetchSessions();

  /// Saves a focus session. If the session has an existing ID, it will be updated; otherwise, a new entry will be created.
  Future<void> saveSession(FocusSession session);

  /// Fetches all focus tags, which can be used for categorizing sessions.
  Future<List<FocusTag>> fetchTags();

  /// Saves a focus tag. If the tag has an existing ID, it will be updated; otherwise, a new entry will be created.
  Future<void> saveTag(FocusTag tag);

  /// Deletes a focus tag by its unique identifier. This will not delete any sessions that were tagged with this tag, but those sessions will no longer reference the deleted tag.
  Future<void> deleteTag(String id);
}
