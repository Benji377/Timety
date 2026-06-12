import 'package:timety/data/focus/focus_models.dart';
import 'package:timety/data/focus/focus_repository.dart';
import 'package:timety/data/habit/habit_models.dart';
import 'package:timety/data/habit/habit_repository.dart';
import 'package:timety/data/task/task.dart';
import 'package:timety/data/task/task_repository.dart';
import 'package:timety/data/user/user.dart';
import 'package:timety/data/user/user_repository.dart';

class FakeTaskRepository implements TaskRepository {
  FakeTaskRepository({List<Task>? initialTasks})
    : _tasks = List<Task>.from(initialTasks ?? []);

  final List<Task> _tasks;
  int fetchCalls = 0;
  int saveCalls = 0;
  List<Task> lastSavedTasks = const [];

  @override
  Future<List<Task>> fetchTasks() async {
    fetchCalls++;
    return List<Task>.from(_tasks);
  }

  @override
  Future<void> saveTask(Task task) async {
    saveCalls++;
    final index = _tasks.indexWhere((existing) => existing.id == task.id);
    if (index == -1) {
      _tasks.add(task);
    } else {
      _tasks[index] = task;
    }
    lastSavedTasks = List<Task>.from(_tasks);
  }

  @override
  Future<void> clearAll() {
    _tasks.clear();
    return Future.value();
  }

  @override
  Future<void> deleteTask(String id) {
    _tasks.removeWhere((task) => task.id == id);
    return Future.value();
  }
}

class FakeHabitRepository implements HabitRepository {
  FakeHabitRepository({List<Habit>? initialHabits})
    : _habits = List<Habit>.from(initialHabits ?? []);

  final List<Habit> _habits;
  int fetchCalls = 0;
  int saveCalls = 0;
  int deleteCalls = 0;
  int clearAllCalls = 0;

  @override
  Future<List<Habit>> fetchHabits() async {
    fetchCalls++;
    return List<Habit>.from(_habits);
  }

  @override
  Future<void> saveHabit(Habit habit) async {
    saveCalls++;
    final index = _habits.indexWhere((existing) => existing.id == habit.id);
    if (index == -1) {
      _habits.add(habit);
    } else {
      _habits[index] = habit;
    }
  }

  @override
  Future<void> deleteHabit(String id) async {
    deleteCalls++;
    _habits.removeWhere((habit) => habit.id == id);
  }

  @override
  Future<void> clearAll() async {
    clearAllCalls++;
    _habits.clear();
  }
}

class FakeFocusRepository implements FocusRepository {
  FakeFocusRepository({
    List<FocusMode>? initialModes,
    List<FocusSession>? initialSessions,
    List<FocusTag>? initialTags,
  }) : _modes = List<FocusMode>.from(initialModes ?? []),
       _sessions = List<FocusSession>.from(initialSessions ?? []),
       _tags = List<FocusTag>.from(initialTags ?? []);

  final List<FocusMode> _modes;
  final List<FocusSession> _sessions;
  final List<FocusTag> _tags;
  int fetchModesCalls = 0;
  int fetchSessionsCalls = 0;
  int fetchTagsCalls = 0;
  final List<String> savedModeIds = [];
  final List<String> deletedModeIds = [];
  final List<String> savedSessionIds = [];
  final List<String> savedTagIds = [];
  final List<String> deletedTagIds = [];

  @override
  Future<List<FocusMode>> fetchModes() async {
    fetchModesCalls++;
    return List<FocusMode>.from(_modes);
  }

  @override
  Future<void> saveMode(FocusMode mode) async {
    savedModeIds.add(mode.id);
    final index = _modes.indexWhere((existing) => existing.id == mode.id);
    if (index == -1) {
      _modes.add(mode);
    } else {
      _modes[index] = mode;
    }
  }

  @override
  Future<void> deleteMode(String id) async {
    deletedModeIds.add(id);
    _modes.removeWhere((mode) => mode.id == id);
  }

  @override
  Future<List<FocusSession>> fetchSessions() async {
    fetchSessionsCalls++;
    return List<FocusSession>.from(_sessions);
  }

  @override
  Future<void> saveSession(FocusSession session) async {
    savedSessionIds.add(session.id);
    final index = _sessions.indexWhere((existing) => existing.id == session.id);
    if (index == -1) {
      _sessions.add(session);
    } else {
      _sessions[index] = session;
    }
  }

  @override
  Future<List<FocusTag>> fetchTags() async {
    fetchTagsCalls++;
    return List<FocusTag>.from(_tags);
  }

  @override
  Future<void> saveTag(FocusTag tag) async {
    savedTagIds.add(tag.id);
    final index = _tags.indexWhere((existing) => existing.id == tag.id);
    if (index == -1) {
      _tags.add(tag);
    } else {
      _tags[index] = tag;
    }
  }

  @override
  Future<void> deleteTag(String id) async {
    deletedTagIds.add(id);
    _tags.removeWhere((tag) => tag.id == id);
  }
}

class FakeUserRepository implements UserRepository {
  FakeUserRepository({UserProfile? initialUser})
    : _user =
          initialUser ??
          UserProfile(name: 'Bobert', accountCreated: DateTime(2024));

  UserProfile? _user;
  int initCalls = 0;
  int saveCalls = 0;
  int addXpCalls = 0;

  @override
  Future<void> init() async {
    initCalls++;
  }

  @override
  UserProfile? getUser() => _user;

  @override
  Future<void> saveUser(UserProfile user) async {
    saveCalls++;
    _user = user;
  }

  @override
  Future<void> updateName(String name) async {
    _user?.name = name;
  }

  @override
  Future<void> updateProfileImage(String path) async {
    _user?.profileImagePath = path;
  }

  @override
  Future<void> addXp(int amount) async {
    addXpCalls++;
    if (_user != null) {
      _user!.totalXp += amount;
    }
  }
}

Future<void> drainEventQueue([int times = 50]) async {
  for (var i = 0; i < times; i++) {
    await Future<void>.delayed(Duration.zero);
  }
}

Habit buildDailyHabit({
  required String id,
  required String name,
  List<DateTime>? completions,
  int? targetTimeMinutes,
  String? stackName,
  int? stackOrder,
  int? colorValue,
  String? notes,
}) {
  return Habit(
    id: id,
    name: name,
    frequency: HabitFrequency.daily,
    completions: completions,
    targetTimeMinutes: targetTimeMinutes,
    stackName: stackName,
    stackOrder: stackOrder,
    colorValue: colorValue,
    notes: notes,
  );
}

Task buildTask({
  required String id,
  required String title,
  String description = '',
  DateTime? dueDate,
  String category = '',
  Priority priority = Priority.medium,
  Size size = Size.medium,
  List<DateTime> reminders = const [],
  bool isCompleted = false,
  DateTime? completedAt,
  List<Subtask> subtasks = const [],
}) {
  return Task(
    id: id,
    title: title,
    description: description,
    dueDate: dueDate,
    category: category,
    priority: priority,
    size: size,
    reminders: reminders,
    isCompleted: isCompleted,
    completedAt: completedAt,
    createdAt: DateTime(2024),
    subtasks: subtasks,
  );
}
