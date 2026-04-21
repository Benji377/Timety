import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

class DatabaseHelper {
  static final DatabaseHelper _instance = DatabaseHelper._internal();
  factory DatabaseHelper() => _instance;
  DatabaseHelper._internal();

  static Database? _database;

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    String path = join(await getDatabasesPath(), 'timety.db');
    return await openDatabase(
      path,
      version: 1,
      onCreate: _onCreate,
    );
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE user (
        id INTEGER PRIMARY KEY,
        name TEXT,
        xp INTEGER,
        level INTEGER,
        currentStreak INTEGER,
        highestStreak INTEGER,
        dailyFocusTarget INTEGER,
        lastActiveDate INTEGER,
        isDarkMode INTEGER
      )
    ''');

    await db.execute('''
      CREATE TABLE category (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT,
        colorHex TEXT,
        iconName TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE task (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT,
        description TEXT,
        iconName TEXT,
        location TEXT,
        dueDate INTEGER,
        reminderTime INTEGER,
        reminders TEXT,
        categoryId INTEGER,
        durationEst INTEGER,
        status INTEGER,
        priority INTEGER,
        size INTEGER,
        FOREIGN KEY (categoryId) REFERENCES category (id) ON DELETE SET NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE focus_mode (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT,
        isCustom INTEGER,
        steps TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE focus_session (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        categoryId INTEGER,
        taskId INTEGER,
        startTime INTEGER,
        endTime INTEGER,
        duration INTEGER,
        rating INTEGER,
        note TEXT,
        FOREIGN KEY (categoryId) REFERENCES category (id) ON DELETE CASCADE,
        FOREIGN KEY (taskId) REFERENCES task (id) ON DELETE SET NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE daily_event (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER,
        type TEXT,
        description TEXT
      )
    ''');

    // Insert default focus modes
    await db.insert('focus_mode', {
      'title': 'Pomodoro',
      'isCustom': 0,
      'steps': '[{"durationMins":25,"type":1,"behavior":1},{"durationMins":5,"type":2,"behavior":1}]'
    });
    
    // Insert default user
    await db.insert('user', {
      'id': 1,
      'name': 'User',
      'xp': 0,
      'level': 1,
      'currentStreak': 0,
      'highestStreak': 0,
      'dailyFocusTarget': 7200000, // 2 hours
      'lastActiveDate': DateTime.now().millisecondsSinceEpoch,
      'isDarkMode': 0
    });
  }
}
