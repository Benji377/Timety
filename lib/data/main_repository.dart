import 'package:sqflite/sqflite.dart';
import 'database_helper.dart';
import 'user.dart';
import 'task.dart';
import 'category.dart';
import 'focus_session.dart';
import 'focus_mode.dart';
import 'daily_event.dart';

class MainRepository {
  final DatabaseHelper _dbHelper = DatabaseHelper();

  // User
  Future<User?> getUser() async {
    final db = await _dbHelper.database;
    final List<Map<String, dynamic>> maps = await db.query('user', where: 'id = 1');
    if (maps.isEmpty) return null;
    return User.fromMap(maps.first);
  }

  Future<void> insertOrUpdateUser(User user) async {
    final db = await _dbHelper.database;
    await db.insert(
      'user',
      user.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  // Tasks
  Future<List<Task>> getAllTasks() async {
    final db = await _dbHelper.database;
    final List<Map<String, dynamic>> maps = await db.query('task');
    return maps.map((m) => Task.fromMap(m)).toList();
  }

  Future<List<Task>> getTasksByStatus(TaskStatus status) async {
    final db = await _dbHelper.database;
    final List<Map<String, dynamic>> maps = await db.query(
      'task',
      where: 'status = ?',
      whereArgs: [status.index],
    );
    return maps.map((m) => Task.fromMap(m)).toList();
  }

  Future<List<Task>> getTasksInRange(int startMillis, int endMillis) async {
    final db = await _dbHelper.database;
    final List<Map<String, dynamic>> maps = await db.query(
      'task',
      where: 'dueDate >= ? AND dueDate <= ?',
      whereArgs: [startMillis, endMillis],
    );
    return maps.map((m) => Task.fromMap(m)).toList();
  }

  Future<int> insertTask(Task task) async {
    final db = await _dbHelper.database;
    return await db.insert('task', task.toMap());
  }

  Future<Task?> getTaskById(int taskId) async {
    final db = await _dbHelper.database;
    final maps = await db.query('task', where: 'id = ?', whereArgs: [taskId]);
    if (maps.isEmpty) return null;
    return Task.fromMap(maps.first);
  }

  Future<void> updateTaskStatus(int taskId, TaskStatus status) async {
    final db = await _dbHelper.database;
    await db.update(
      'task',
      {'status': status.index},
      where: 'id = ?',
      whereArgs: [taskId],
    );
  }

  Future<void> updateTask(Task task) async {
    final db = await _dbHelper.database;
    await db.update(
      'task',
      task.toMap(),
      where: 'id = ?',
      whereArgs: [task.id],
    );
  }

  Future<void> deleteTask(int taskId) async {
    final db = await _dbHelper.database;
    await db.delete('task', where: 'id = ?', whereArgs: [taskId]);
  }

  Future<void> updateOverdueTasks(int nowMillis) async {
    final db = await _dbHelper.database;
    await db.update(
      'task',
      {'status': TaskStatus.overdue.index},
      where: 'dueDate < ? AND status = ?',
      whereArgs: [nowMillis, TaskStatus.todo.index],
    );
  }

  // Categories
  Future<List<Category>> getAllCategories() async {
    final db = await _dbHelper.database;
    final List<Map<String, dynamic>> maps = await db.query('category');
    return maps.map((m) => Category.fromMap(m)).toList();
  }

  Future<int> insertCategory(Category category) async {
    final db = await _dbHelper.database;
    return await db.insert('category', category.toMap());
  }

  Future<void> updateCategory(Category category) async {
    final db = await _dbHelper.database;
    await db.update(
      'category',
      category.toMap(),
      where: 'id = ?',
      whereArgs: [category.id],
    );
  }

  Future<void> deleteCategory(int categoryId) async {
    final db = await _dbHelper.database;
    await db.delete('category', where: 'id = ?', whereArgs: [categoryId]);
  }

  // Focus Sessions
  Future<List<FocusSession>> getAllSessions() async {
    final db = await _dbHelper.database;
    final List<Map<String, dynamic>> maps = await db.query('focus_session');
    return maps.map((m) => FocusSession.fromMap(m)).toList();
  }

  Future<List<FocusSession>> getSessionsForTask(int taskId) async {
    final db = await _dbHelper.database;
    final maps = await db.query('focus_session', where: 'taskId = ?', whereArgs: [taskId]);
    return maps.map((m) => FocusSession.fromMap(m)).toList();
  }

  Future<List<FocusSession>> getSessionsForDay(int startMillis, int endMillis) async {
    final db = await _dbHelper.database;
    final maps = await db.query(
      'focus_session',
      where: 'startTime >= ? AND startTime <= ?',
      whereArgs: [startMillis, endMillis],
    );
    return maps.map((m) => FocusSession.fromMap(m)).toList();
  }

  Future<int> insertSession(FocusSession session) async {
    final db = await _dbHelper.database;
    return await db.insert('focus_session', session.toMap());
  }

  // Focus Modes
  Future<List<FocusMode>> getAllFocusModes() async {
    final db = await _dbHelper.database;
    final maps = await db.query('focus_mode');
    return maps.map((m) => FocusMode.fromMap(m)).toList();
  }

  Future<int> insertFocusMode(FocusMode focusMode) async {
    final db = await _dbHelper.database;
    return await db.insert('focus_mode', focusMode.toMap());
  }

  Future<void> updateFocusMode(FocusMode focusMode) async {
    final db = await _dbHelper.database;
    await db.update(
      'focus_mode',
      focusMode.toMap(),
      where: 'id = ?',
      whereArgs: [focusMode.id],
    );
  }

  Future<void> deleteFocusMode(int id) async {
    final db = await _dbHelper.database;
    await db.delete('focus_mode', where: 'id = ?', whereArgs: [id]);
  }

  // Daily Events
  Future<List<DailyEvent>> getEventsForDay(int startMillis, int endMillis) async {
    final db = await _dbHelper.database;
    final maps = await db.query(
      'daily_event',
      where: 'timestamp >= ? AND timestamp <= ?',
      whereArgs: [startMillis, endMillis],
    );
    return maps.map((m) => DailyEvent.fromMap(m)).toList();
  }

  Future<int> insertEvent(DailyEvent event) async {
    final db = await _dbHelper.database;
    return await db.insert('daily_event', event.toMap());
  }
}
