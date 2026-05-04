import 'package:hive/hive.dart';
import 'focus_models.dart';
import 'focus_repository.dart';

class HiveFocusRepository implements FocusRepository {
  static const String modeBoxName = 'focusModesBox';
  static const String sessionBoxName = 'focusSessionsBox';
  static const String tagBoxName = 'focusTagsBox';

  @override
  Future<List<FocusMode>> fetchModes() async {
    final box = await Hive.openBox<FocusMode>(modeBoxName);
    return box.values.toList();
  }

  @override
  Future<void> saveMode(FocusMode mode) async {
    final box = await Hive.openBox<FocusMode>(modeBoxName);
    await box.put(mode.id, mode);
  }

  @override
  Future<void> deleteMode(String id) async {
    final box = await Hive.openBox<FocusMode>(modeBoxName);
    await box.delete(id);
  }

  @override
  Future<List<FocusSession>> fetchSessions() async {
    final box = await Hive.openBox<FocusSession>(sessionBoxName);
    return box.values.toList();
  }

  @override
  Future<void> saveSession(FocusSession session) async {
    final box = await Hive.openBox<FocusSession>(sessionBoxName);
    await box.put(session.id, session);
  }

  @override
  Future<List<FocusTag>> fetchTags() async {
    final box = await Hive.openBox<FocusTag>(tagBoxName);
    return box.values.toList();
  }

  @override
  Future<void> saveTag(FocusTag tag) async {
    final box = await Hive.openBox<FocusTag>(tagBoxName);
    await box.put(tag.id, tag);
  }

  @override
  Future<void> deleteTag(String id) async {
    final box = await Hive.openBox<FocusTag>(tagBoxName);
    await box.delete(id);
  }
}
