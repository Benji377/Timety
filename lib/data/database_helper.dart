import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import 'dart:convert';

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
      version: 3,
      onCreate: _onCreate,
      onUpgrade: _onUpgrade,
    );
  }

  Future<void> _onUpgrade(Database db, int oldVersion, int newVersion) async {
    if (oldVersion < 2) {
      // Add new columns to task table
      await db.execute('ALTER TABLE task ADD COLUMN dueTime INTEGER');
      await db.execute(
        'ALTER TABLE task ADD COLUMN xpAwarded INTEGER DEFAULT 0',
      );
    }
    if (oldVersion < 3) {
      // Add new columns to user table
      await db.execute(
        'ALTER TABLE user ADD COLUMN streakFrozenDaysRemaining INTEGER DEFAULT 0',
      );
      await db.execute(
        'ALTER TABLE user ADD COLUMN minStreakMinutes INTEGER DEFAULT 1',
      );
      await db.execute(
        'ALTER TABLE user ADD COLUMN maxFocusSessionDuration INTEGER DEFAULT 120',
      );
    }
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
        streakFrozenDaysRemaining INTEGER,
        dailyFocusTarget INTEGER,
        lastActiveDate INTEGER,
        minStreakMinutes INTEGER,
        maxFocusSessionDuration INTEGER,
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
        dueTime INTEGER,
        reminders TEXT,
        categoryId INTEGER,
        durationEst INTEGER,
        status INTEGER,
        priority INTEGER,
        size INTEGER,
        xpAwarded INTEGER DEFAULT 0,
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
    // Pomodoro: start -> 25min focus -> 5min rest (x4 cycles) -> end
    await db.insert('focus_mode', {
      'title': 'Pomodoro Classic',
      'isCustom': 0,
      'steps': jsonEncode([
        {'durationMins': 0, 'type': 0, 'behavior': 0}, // start
        {'durationMins': 25, 'type': 1, 'behavior': 1}, // focus (countDown)
        {'durationMins': 5, 'type': 2, 'behavior': 1}, // rest (countDown)
        {'durationMins': 25, 'type': 1, 'behavior': 1}, // focus
        {'durationMins': 5, 'type': 2, 'behavior': 1}, // rest
        {'durationMins': 25, 'type': 1, 'behavior': 1}, // focus
        {'durationMins': 5, 'type': 2, 'behavior': 1}, // rest
        {'durationMins': 25, 'type': 1, 'behavior': 1}, // focus
        {
          'durationMins': 5,
          'type': 2,
          'behavior': 1,
        }, // rest (final short break)
        {'durationMins': 0, 'type': 3, 'behavior': 0}, // end
      ]),
    });

    // Stopwatch: start -> stopwatch (open-ended) -> end
    await db.insert('focus_mode', {
      'title': 'Stopwatch',
      'isCustom': 0,
      'steps': jsonEncode([
        {'durationMins': 0, 'type': 0, 'behavior': 0}, // start
        {'durationMins': 0, 'type': 5, 'behavior': 0}, // stopwatch (open-ended)
        {'durationMins': 0, 'type': 3, 'behavior': 0}, // end
      ]),
    });

    // Flexible: start -> flexible focus -> end
    await db.insert('focus_mode', {
      'title': 'Flexible',
      'isCustom': 0,
      'steps': jsonEncode([
        {'durationMins': 0, 'type': 0, 'behavior': 0}, // start
        {'durationMins': 0, 'type': 6, 'behavior': 0}, // flexible
        {'durationMins': 0, 'type': 3, 'behavior': 0}, // end
      ]),
    });

    // Insert default user with new fields
    final now = DateTime.now();
    await db.insert('user', {
      'id': 1,
      'name': 'User',
      'xp': 0,
      'level': 1,
      'currentStreak': 0,
      'highestStreak': 0,
      'streakFrozenDaysRemaining': 0,
      'dailyFocusTarget': 7200000, // 2 hours
      'lastActiveDate': now.millisecondsSinceEpoch,
      'minStreakMinutes': 1,
      'maxFocusSessionDuration': 120,
      'isDarkMode': 0,
    });
  }
}
