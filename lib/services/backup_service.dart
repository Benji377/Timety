import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:hive/hive.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../data/focus/focus_models.dart';
import '../data/focus/focus_repository_hive.dart';
import '../data/habit/habit_models.dart';
import '../data/habit/habit_repository_hive.dart';
import '../data/task/task.dart';
import '../data/task/task_repository_hive.dart';
import '../data/user/user.dart';
import '../data/user/user_repository_hive.dart';
import '../widgets/dialogs.dart';

class BackupService {
  static const int _schemaVersion = 1;
  static const String _userDataType = 'user_data';

  // --- USER DATA EXPORT / IMPORT ---
  static Future<void> exportUserData(BuildContext context) async {
    try {
      final payload = await _buildPayload(payloadType: _userDataType);
      final fileName = _buildTimestampedFileName('Timety_Export', 'json');
      final jsonPath = await _writeJsonToTempFile(payload, fileName);

      if (!context.mounted) return;
      await _showFileActions(
        context,
        filePath: jsonPath,
        fileName: fileName,
        dialogTitle: 'Save Export',
        shareSubject: 'Timety Export',
        successLabel: 'Export saved to',
      );
    } catch (e) {
      debugPrint('Error exporting user data: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Export failed: $e')));
      }
    }
  }

  static Future<void> importUserData(BuildContext context) async {
    try {
      final FilePickerResult? result = await FilePicker.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );

      if (result == null || result.files.single.path == null) return;
      if (!context.mounted) return;

      final confirm = await AppDialogs.showConfirmation(
        context: context,
        title: 'Import JSON Data?',
        content:
            'This will OVERWRITE all current user data and settings. This cannot be undone. Are you sure?',
      );

      if (confirm != true) return;

      final payload = await _readJsonPayloadFromFile(result.files.single.path!);
      await _restorePayload(payload);

      if (context.mounted) {
        _showRestoreSuccessDialog(context);
      }
    } catch (e) {
      debugPrint('Error importing user data: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Import failed: Check file format')),
        );
      }
    }
  }

  static Future<void> _showFileActions(
    BuildContext context, {
    required String filePath,
    required String fileName,
    required String dialogTitle,
    required String shareSubject,
    required String successLabel,
  }) async {
    await showModalBottomSheet(
      context: context,
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              title: Text(
                dialogTitle,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.folder_outlined),
              title: const Text('Save to Device'),
              subtitle: const Text('Choose a local folder'),
              onTap: () async {
                Navigator.of(ctx).pop();
                await _saveToDevice(
                  context,
                  filePath: filePath,
                  fileName: fileName,
                  successLabel: successLabel,
                );
              },
            ),
            ListTile(
              leading: const Icon(Icons.share_outlined),
              title: const Text('Share / Upload to Cloud'),
              subtitle: const Text('Send to Drive, Dropbox, WhatsApp…'),
              onTap: () async {
                Navigator.of(ctx).pop();
                await _shareFile(
                  context,
                  filePath: filePath,
                  shareSubject: shareSubject,
                );
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  static Future<void> _saveToDevice(
    BuildContext context, {
    required String filePath,
    required String fileName,
    required String successLabel,
  }) async {
    try {
      final bytes = await File(filePath).readAsBytes();

      final outputPath = await FilePicker.saveFile(
        dialogTitle: 'Choose where to save your file',
        fileName: fileName,
        type: FileType.custom,
        allowedExtensions: ['json'],
        bytes: bytes,
      );

      if (outputPath == null) return;

      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('$successLabel: $outputPath')));
      }

      File(filePath).delete().ignore();
    } catch (e) {
      debugPrint('Error saving file: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Save failed: $e')));
      }
      File(filePath).delete().ignore();
    }
  }

  static Future<void> _shareFile(
    BuildContext context, {
    required String filePath,
    required String shareSubject,
  }) async {
    try {
      final xFile = XFile(filePath, mimeType: 'application/json');
      await SharePlus.instance.share(
        ShareParams(subject: shareSubject, files: [xFile]),
      );

      await Future.delayed(const Duration(seconds: 1));
      File(filePath).delete().ignore();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Share failed: $e')));
      }
      File(filePath).delete().ignore();
    }
  }

  static Future<String> _writeJsonToTempFile(
    Map<String, dynamic> payload,
    String fileName,
  ) async {
    final tempDir = await getTemporaryDirectory();
    final filePath = '${tempDir.path}/$fileName';
    final jsonText = const JsonEncoder.withIndent('  ').convert(payload);
    await File(filePath).writeAsString(jsonText);
    return filePath;
  }

  static Future<Map<String, dynamic>> _readJsonPayloadFromFile(
    String filePath,
  ) async {
    final raw = await File(filePath).readAsString();
    final decoded = jsonDecode(raw);
    if (decoded is! Map) {
      throw const FormatException('JSON payload must be an object.');
    }
    return Map<String, dynamic>.from(decoded);
  }

  static Future<void> _restorePayload(Map<String, dynamic> payload) async {
    final schemaVersion = _readInt(payload['schemaVersion']) ?? 1;
    if (schemaVersion > _schemaVersion) {
      throw FormatException(
        'Unsupported backup schema version $schemaVersion.',
      );
    }

    await Hive.close();
    await _restorePreferences(_readMap(payload['preferences']));
    await _restoreUserProfile(_readMap(payload['userProfile']));

    final tasks = _readMapList(payload['tasks']);
    final habits = _readMapList(payload['habits']);
    final focus = _readMap(payload['focus']);
    final modes = _readMapList(focus['modes']);
    final sessions = _readMapList(focus['sessions']);
    final tags = _readMapList(focus['tags']);

    await _restoreTasks(tasks);
    await _restoreHabits(habits);
    await _restoreFocusModes(modes);
    await _restoreFocusSessions(sessions);
    await _restoreFocusTags(tags);
    await _ensureSystemFocusModes();
  }

  static Future<Map<String, dynamic>> _buildPayload({
    required String payloadType,
  }) async {
    final packageInfo = await PackageInfo.fromPlatform();
    final prefs = await SharedPreferences.getInstance();

    return <String, dynamic>{
      'payloadType': payloadType,
      'schemaVersion': _schemaVersion,
      'appVersion': packageInfo.version,
      'buildNumber': packageInfo.buildNumber,
      'exportedAt': DateTime.now().toIso8601String(),
      'preferences': _exportPreferences(prefs),
      'userProfile': await _exportUserProfile(),
      'tasks': await _exportTasks(),
      'habits': await _exportHabits(),
      'focus': <String, dynamic>{
        'modes': await _exportFocusModes(),
        'sessions': await _exportFocusSessions(),
        'tags': await _exportFocusTags(),
      },
    };
  }

  static Map<String, dynamic> _exportPreferences(SharedPreferences prefs) {
    final prefsMap = <String, dynamic>{};
    for (final key in prefs.getKeys()) {
      prefsMap[key] = prefs.get(key);
    }
    return prefsMap;
  }

  static Future<Map<String, dynamic>?> _exportUserProfile() async {
    final box = await Hive.openBox<UserProfile>(HiveUserRepository.boxName);
    if (box.isEmpty) return null;
    return _userProfileToJson(box.getAt(0)!);
  }

  static Future<List<Map<String, dynamic>>> _exportTasks() async {
    final box = await Hive.openBox<Task>(HiveTaskRepository.boxName);
    return box.values.map(_taskToJson).toList();
  }

  static Future<List<Map<String, dynamic>>> _exportHabits() async {
    final box = await Hive.openBox<Habit>(HiveHabitRepository.boxName);
    return box.values.map(_habitToJson).toList();
  }

  static Future<List<Map<String, dynamic>>> _exportFocusModes() async {
    final box = await Hive.openBox<FocusMode>(HiveFocusRepository.modeBoxName);
    return box.values.map(_focusModeToJson).toList();
  }

  static Future<List<Map<String, dynamic>>> _exportFocusSessions() async {
    final box = await Hive.openBox<FocusSession>(
      HiveFocusRepository.sessionBoxName,
    );
    return box.values.map(_focusSessionToJson).toList();
  }

  static Future<List<Map<String, dynamic>>> _exportFocusTags() async {
    final box = await Hive.openBox<FocusTag>(HiveFocusRepository.tagBoxName);
    return box.values.map(_focusTagToJson).toList();
  }

  static Future<void> _restorePreferences(
    Map<String, dynamic> preferences,
  ) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();

    for (final entry in preferences.entries) {
      final value = entry.value;
      if (value is bool) {
        await prefs.setBool(entry.key, value);
      } else if (value is int) {
        await prefs.setInt(entry.key, value);
      } else if (value is double) {
        await prefs.setDouble(entry.key, value);
      } else if (value is String) {
        await prefs.setString(entry.key, value);
      } else if (value is List) {
        await prefs.setStringList(
          entry.key,
          value.map((item) => item.toString()).toList(),
        );
      }
    }
  }

  static Future<void> _restoreUserProfile(
    Map<String, dynamic> userProfile,
  ) async {
    final box = await Hive.openBox<UserProfile>(HiveUserRepository.boxName);
    await box.clear();

    if (userProfile.isEmpty) {
      await box.add(
        UserProfile(name: 'Bobert', accountCreated: DateTime.now()),
      );
      return;
    }

    await box.add(_userProfileFromJson(userProfile));
  }

  static Future<void> _restoreTasks(List<Map<String, dynamic>> tasks) async {
    final box = await Hive.openBox<Task>(HiveTaskRepository.boxName);
    await box.clear();
    final taskMap = <String, Task>{};

    for (final taskJson in tasks) {
      final task = _taskFromJson(taskJson);
      taskMap[task.id] = task;
    }

    await box.putAll(taskMap);
  }

  static Future<void> _restoreHabits(List<Map<String, dynamic>> habits) async {
    final box = await Hive.openBox<Habit>(HiveHabitRepository.boxName);
    await box.clear();

    for (final habitJson in habits) {
      final habit = _habitFromJson(habitJson);
      await box.put(habit.id, habit);
    }
  }

  static Future<void> _restoreFocusModes(
    List<Map<String, dynamic>> modes,
  ) async {
    final box = await Hive.openBox<FocusMode>(HiveFocusRepository.modeBoxName);
    await box.clear();

    for (final modeJson in modes) {
      final mode = _focusModeFromJson(modeJson);
      await box.put(mode.id, mode);
    }
  }

  static Future<void> _restoreFocusSessions(
    List<Map<String, dynamic>> sessions,
  ) async {
    final box = await Hive.openBox<FocusSession>(
      HiveFocusRepository.sessionBoxName,
    );
    await box.clear();

    for (final sessionJson in sessions) {
      final session = _focusSessionFromJson(sessionJson);
      await box.put(session.id, session);
    }
  }

  static Future<void> _restoreFocusTags(List<Map<String, dynamic>> tags) async {
    final box = await Hive.openBox<FocusTag>(HiveFocusRepository.tagBoxName);
    await box.clear();

    for (final tagJson in tags) {
      final tag = _focusTagFromJson(tagJson);
      await box.put(tag.id, tag);
    }
  }

  static Future<void> _ensureSystemFocusModes() async {
    final box = await Hive.openBox<FocusMode>(HiveFocusRepository.modeBoxName);
    final defaultModes = <FocusMode>[
      FocusMode.stopwatch(),
      FocusMode.flexible(),
      FocusMode.classicPomodoro(),
    ];

    for (final mode in defaultModes) {
      if (!box.containsKey(mode.id)) {
        await box.put(mode.id, mode);
      }
    }
  }

  static Map<String, dynamic> _userProfileToJson(UserProfile profile) {
    return <String, dynamic>{
      'name': profile.name,
      'profileImagePath': profile.profileImagePath,
      'accountCreated': profile.accountCreated.toIso8601String(),
      'unlockedAchievements': profile.unlockedAchievements,
      'totalXp': profile.totalXp,
    };
  }

  static UserProfile _userProfileFromJson(Map<String, dynamic> json) {
    return UserProfile(
      name: _readString(json['name']) ?? 'Bobert',
      profileImagePath: _readString(json['profileImagePath']),
      accountCreated: _readDateTime(json['accountCreated']) ?? DateTime.now(),
      unlockedAchievements: _readStringList(json['unlockedAchievements']),
      totalXp: _readInt(json['totalXp']) ?? 0,
    );
  }

  static Map<String, dynamic> _taskToJson(Task task) {
    return <String, dynamic>{
      'id': task.id,
      'title': task.title,
      'description': task.description,
      'dueDate': task.dueDate?.toIso8601String(),
      'location': task.location,
      'priority': task.priority.name,
      'reminders': task.reminders
          .map((reminder) => reminder.toIso8601String())
          .toList(),
      'category': task.category,
      'size': task.size.name,
      'isCompleted': task.isCompleted,
      'completedAt': task.completedAt?.toIso8601String(),
      'createdAt': task.createdAt.toIso8601String(),
      'subtasks': task.subtasks.map(_subtaskToJson).toList(),
    };
  }

  static Task _taskFromJson(Map<String, dynamic> json) {
    return Task(
      id:
          _readString(json['id']) ??
          DateTime.now().microsecondsSinceEpoch.toString(),
      title: _readString(json['title']) ?? '',
      description: _readString(json['description']) ?? '',
      dueDate: _readDateTime(json['dueDate']),
      location: _readString(json['location']) ?? '',
      priority: _priorityFromName(_readString(json['priority'])),
      reminders: _readDateTimeList(json['reminders']),
      category: _readString(json['category']) ?? '',
      size: _sizeFromName(_readString(json['size'])),
      isCompleted: _readBool(json['isCompleted']) ?? false,
      completedAt: _readDateTime(json['completedAt']),
      createdAt: _readDateTime(json['createdAt']) ?? DateTime.now(),
      subtasks: _readMapList(json['subtasks']).map(_subtaskFromJson).toList(),
    );
  }

  static Map<String, dynamic> _subtaskToJson(Subtask subtask) {
    return <String, dynamic>{
      'id': subtask.id,
      'title': subtask.title,
      'isCompleted': subtask.isCompleted,
    };
  }

  static Subtask _subtaskFromJson(Map<String, dynamic> json) {
    return Subtask(
      id:
          _readString(json['id']) ??
          DateTime.now().microsecondsSinceEpoch.toString(),
      title: _readString(json['title']) ?? '',
      isCompleted: _readBool(json['isCompleted']) ?? false,
    );
  }

  static Map<String, dynamic> _habitToJson(Habit habit) {
    return <String, dynamic>{
      'id': habit.id,
      'name': habit.name,
      'frequency': habit.frequency.name,
      'targetDaysPerWeek': habit.targetDaysPerWeek,
      'targetWeekdays': habit.targetWeekdays,
      'targetTimeMinutes': habit.targetTimeMinutes,
      'completions': habit.completions
          .map((date) => date.toIso8601String())
          .toList(),
      'createdAt': habit.createdAt.toIso8601String(),
      'colorValue': habit.colorValue,
      'notes': habit.notes,
      'iconCodePoint': habit.iconCodePoint,
      'stackName': habit.stackName,
      'stackOrder': habit.stackOrder,
    };
  }

  static Habit _habitFromJson(Map<String, dynamic> json) {
    return Habit(
      id:
          _readString(json['id']) ??
          DateTime.now().microsecondsSinceEpoch.toString(),
      name: _readString(json['name']) ?? '',
      frequency: _habitFrequencyFromName(_readString(json['frequency'])),
      targetDaysPerWeek: _readInt(json['targetDaysPerWeek']),
      targetWeekdays: _readIntList(json['targetWeekdays']),
      targetTimeMinutes: _readInt(json['targetTimeMinutes']),
      completions: _readDateTimeList(json['completions']),
      createdAt: _readDateTime(json['createdAt']) ?? DateTime.now(),
      colorValue: _readInt(json['colorValue']),
      notes: _readString(json['notes']),
      iconCodePoint: _readInt(json['iconCodePoint']),
      stackName: _readString(json['stackName']),
      stackOrder: _readInt(json['stackOrder']),
    );
  }

  static Map<String, dynamic> _focusModeToJson(FocusMode mode) {
    return <String, dynamic>{
      'id': mode.id,
      'name': mode.name,
      'type': mode.type.name,
      'phases': mode.phases.map(_phaseToJson).toList(),
      'isSystem': mode.isSystem,
    };
  }

  static FocusMode _focusModeFromJson(Map<String, dynamic> json) {
    return FocusMode(
      id:
          _readString(json['id']) ??
          DateTime.now().microsecondsSinceEpoch.toString(),
      name: _readString(json['name']) ?? '',
      type: _focusModeTypeFromName(_readString(json['type'])),
      phases: _readMapList(json['phases']).map(_phaseFromJson).toList(),
      isSystem: _readBool(json['isSystem']) ?? false,
    );
  }

  static Map<String, dynamic> _phaseToJson(SessionPhase phase) {
    return <String, dynamic>{
      'type': phase.type.name,
      'durationMinutes': phase.durationMinutes,
    };
  }

  static SessionPhase _phaseFromJson(Map<String, dynamic> json) {
    return SessionPhase(
      type: _phaseTypeFromName(_readString(json['type'])),
      durationMinutes: _readInt(json['durationMinutes']) ?? 0,
    );
  }

  static Map<String, dynamic> _focusTagToJson(FocusTag tag) {
    return <String, dynamic>{
      'id': tag.id,
      'name': tag.name,
      'colorValue': tag.colorValue,
    };
  }

  static FocusTag _focusTagFromJson(Map<String, dynamic> json) {
    return FocusTag(
      id:
          _readString(json['id']) ??
          DateTime.now().microsecondsSinceEpoch.toString(),
      name: _readString(json['name']) ?? '',
      colorValue: _readInt(json['colorValue']) ?? 0,
    );
  }

  static Map<String, dynamic> _focusSessionToJson(FocusSession session) {
    return <String, dynamic>{
      'id': session.id,
      'modeId': session.modeId,
      'startTime': session.startTime.toIso8601String(),
      'endTime': session.endTime?.toIso8601String(),
      'totalSecondsFocused': session.totalSecondsFocused,
      'distractions': session.distractions.map(_distractionToJson).toList(),
      'isCompleted': session.isCompleted,
      'tagId': session.tagId,
      'targetType': session.targetType.name,
      'targetId': session.targetId,
      'targetLabel': session.targetLabel,
    };
  }

  static FocusSession _focusSessionFromJson(Map<String, dynamic> json) {
    return FocusSession(
      id:
          _readString(json['id']) ??
          DateTime.now().microsecondsSinceEpoch.toString(),
      modeId: _readString(json['modeId']) ?? '',
      startTime: _readDateTime(json['startTime']) ?? DateTime.now(),
      endTime: _readDateTime(json['endTime']),
      totalSecondsFocused: _readInt(json['totalSecondsFocused']) ?? 0,
      distractions: _readMapList(
        json['distractions'],
      ).map(_distractionFromJson).toList(),
      isCompleted: _readBool(json['isCompleted']) ?? false,
      tagId: _readString(json['tagId']),
      targetType: _focusTargetTypeFromName(_readString(json['targetType'])),
      targetId: _readString(json['targetId']),
      targetLabel: _readString(json['targetLabel']),
    );
  }

  static Map<String, dynamic> _distractionToJson(Distraction distraction) {
    return <String, dynamic>{
      'time': distraction.time.toIso8601String(),
      'note': distraction.note,
    };
  }

  static Distraction _distractionFromJson(Map<String, dynamic> json) {
    return Distraction(
      time: _readDateTime(json['time']) ?? DateTime.now(),
      note: _readString(json['note']) ?? '',
    );
  }

  static Map<String, dynamic> _readMap(dynamic value) {
    if (value is Map) {
      return Map<String, dynamic>.from(value);
    }
    return <String, dynamic>{};
  }

  static List<Map<String, dynamic>> _readMapList(dynamic value) {
    if (value is! List) return <Map<String, dynamic>>[];
    return value
        .whereType<Map>()
        .map((item) => Map<String, dynamic>.from(item))
        .toList();
  }

  static List<String> _readStringList(dynamic value) {
    if (value is! List) return <String>[];
    return value.map((item) => item.toString()).toList();
  }

  static List<int> _readIntList(dynamic value) {
    if (value is! List) return <int>[];
    return value.map((item) => _readInt(item) ?? 0).toList();
  }

  static List<DateTime> _readDateTimeList(dynamic value) {
    if (value is! List) return <DateTime>[];
    return value.map(_readDateTime).whereType<DateTime>().toList();
  }

  static String? _readString(dynamic value) {
    if (value == null) return null;
    return value.toString();
  }

  static int? _readInt(dynamic value) {
    if (value is int) return value;
    if (value is num) return value.toInt();
    return int.tryParse(value?.toString() ?? '');
  }

  static bool? _readBool(dynamic value) {
    if (value is bool) return value;
    if (value == null) return null;
    final lower = value.toString().toLowerCase();
    if (lower == 'true') return true;
    if (lower == 'false') return false;
    return null;
  }

  static DateTime? _readDateTime(dynamic value) {
    if (value == null) return null;
    if (value is DateTime) return value;
    return DateTime.tryParse(value.toString());
  }

  static Priority _priorityFromName(String? name) {
    return Priority.values.firstWhere(
      (value) => value.name == name,
      orElse: () => Priority.medium,
    );
  }

  static Size _sizeFromName(String? name) {
    return Size.values.firstWhere(
      (value) => value.name == name,
      orElse: () => Size.medium,
    );
  }

  static HabitFrequency _habitFrequencyFromName(String? name) {
    return HabitFrequency.values.firstWhere(
      (value) => value.name == name,
      orElse: () => HabitFrequency.daily,
    );
  }

  static FocusModeType _focusModeTypeFromName(String? name) {
    return FocusModeType.values.firstWhere(
      (value) => value.name == name,
      orElse: () => FocusModeType.stopwatch,
    );
  }

  static PhaseType _phaseTypeFromName(String? name) {
    return PhaseType.values.firstWhere(
      (value) => value.name == name,
      orElse: () => PhaseType.focus,
    );
  }

  static FocusTargetType _focusTargetTypeFromName(String? name) {
    return FocusTargetType.values.firstWhere(
      (value) => value.name == name,
      orElse: () => FocusTargetType.tag,
    );
  }

  static String _buildTimestampedFileName(String prefix, String extension) {
    return '${prefix}_${DateTime.now().millisecondsSinceEpoch}.$extension';
  }

  static Future<void> _showRestoreSuccessDialog(BuildContext context) async {
    if (!context.mounted) return;

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('Restore Successful!'),
        content: const Text(
          'Your data has been restored. Please completely close and restart Timety to load the imported data.',
        ),
        actions: [
          ElevatedButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Got it'),
          ),
        ],
      ),
    );
  }
}
