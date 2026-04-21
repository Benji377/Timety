class User {
  final int id;
  final String name;
  final int xp;
  final int level;
  final int currentStreak;
  final int highestStreak;
  final int
  streakFrozenDaysRemaining; // Days remaining before streak resets (0 = not frozen)
  final int lastActiveDate; // Milliseconds - last day user did focus or task
  final int dailyFocusTarget; // Milliseconds
  final int
  minStreakMinutes; // Minutes of focus required for streak (default: 1)
  final int maxFocusSessionDuration; // Minutes (default: 120)
  final bool isDarkMode;

  User({
    this.id = 1,
    required this.name,
    this.xp = 0,
    this.level = 1,
    this.currentStreak = 0,
    this.highestStreak = 0,
    this.streakFrozenDaysRemaining = 0,
    required this.lastActiveDate,
    required this.dailyFocusTarget,
    this.minStreakMinutes = 1,
    this.maxFocusSessionDuration = 120,
    this.isDarkMode = false,
  });

  // Calculate title and emoji based on level
  String get userTitle {
    if (level >= 50) return '🔥 Focus Grandmaster';
    if (level >= 40) return '⭐ Focus Legend';
    if (level >= 30) return '💎 Focus Master';
    if (level >= 20) return '🏆 Focus Champion';
    if (level >= 15) return '👑 Focus King/Queen';
    if (level >= 10) return '🌟 Focus Expert';
    if (level >= 5) return '⚡ Focus Adept';
    return '🚀 Ready to Focus';
  }

  String get levelEmoji {
    if (level >= 50) return '🔥';
    if (level >= 40) return '⭐';
    if (level >= 30) return '💎';
    if (level >= 20) return '🏆';
    if (level >= 15) return '👑';
    if (level >= 10) return '🌟';
    if (level >= 5) return '⚡';
    return '🚀';
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'xp': xp,
      'level': level,
      'currentStreak': currentStreak,
      'highestStreak': highestStreak,
      'streakFrozenDaysRemaining': streakFrozenDaysRemaining,
      'lastActiveDate': lastActiveDate,
      'dailyFocusTarget': dailyFocusTarget,
      'minStreakMinutes': minStreakMinutes,
      'maxFocusSessionDuration': maxFocusSessionDuration,
      'isDarkMode': isDarkMode ? 1 : 0,
    };
  }

  factory User.fromMap(Map<String, dynamic> map) {
    return User(
      id: map['id'] ?? 1,
      name: map['name'] ?? 'User',
      xp: map['xp'] ?? 0,
      level: map['level'] ?? 1,
      currentStreak: map['currentStreak'] ?? 0,
      highestStreak: map['highestStreak'] ?? 0,
      streakFrozenDaysRemaining: map['streakFrozenDaysRemaining'] ?? 0,
      lastActiveDate: map['lastActiveDate'] ?? 0,
      dailyFocusTarget: map['dailyFocusTarget'] ?? 7200000,
      minStreakMinutes: map['minStreakMinutes'] ?? 1,
      maxFocusSessionDuration: map['maxFocusSessionDuration'] ?? 120,
      isDarkMode: (map['isDarkMode'] ?? 0) == 1,
    );
  }

  User copyWith({
    String? name,
    int? xp,
    int? level,
    int? currentStreak,
    int? highestStreak,
    int? streakFrozenDaysRemaining,
    int? lastActiveDate,
    int? dailyFocusTarget,
    int? minStreakMinutes,
    int? maxFocusSessionDuration,
    bool? isDarkMode,
  }) {
    return User(
      id: id,
      name: name ?? this.name,
      xp: xp ?? this.xp,
      level: level ?? this.level,
      currentStreak: currentStreak ?? this.currentStreak,
      highestStreak: highestStreak ?? this.highestStreak,
      streakFrozenDaysRemaining:
          streakFrozenDaysRemaining ?? this.streakFrozenDaysRemaining,
      lastActiveDate: lastActiveDate ?? this.lastActiveDate,
      dailyFocusTarget: dailyFocusTarget ?? this.dailyFocusTarget,
      minStreakMinutes: minStreakMinutes ?? this.minStreakMinutes,
      maxFocusSessionDuration:
          maxFocusSessionDuration ?? this.maxFocusSessionDuration,
      isDarkMode: isDarkMode ?? this.isDarkMode,
    );
  }
}
