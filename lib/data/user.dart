class User {
  final int id;
  final String name;
  final int xp;
  final int level;
  final int currentStreak;
  final int highestStreak;
  final int dailyFocusTarget; // Milliseconds
  final int lastActiveDate; // Milliseconds
  final bool isDarkMode;

  User({
    this.id = 1,
    required this.name,
    this.xp = 0,
    this.level = 1,
    this.currentStreak = 0,
    this.highestStreak = 0,
    required this.dailyFocusTarget,
    required this.lastActiveDate,
    this.isDarkMode = false,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'xp': xp,
      'level': level,
      'currentStreak': currentStreak,
      'highestStreak': highestStreak,
      'dailyFocusTarget': dailyFocusTarget,
      'lastActiveDate': lastActiveDate,
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
      dailyFocusTarget: map['dailyFocusTarget'] ?? 7200000,
      lastActiveDate: map['lastActiveDate'] ?? 0,
      isDarkMode: (map['isDarkMode'] ?? 0) == 1,
    );
  }

  User copyWith({
    String? name,
    int? xp,
    int? level,
    int? currentStreak,
    int? highestStreak,
    int? dailyFocusTarget,
    int? lastActiveDate,
    bool? isDarkMode,
  }) {
    return User(
      id: id,
      name: name ?? this.name,
      xp: xp ?? this.xp,
      level: level ?? this.level,
      currentStreak: currentStreak ?? this.currentStreak,
      highestStreak: highestStreak ?? this.highestStreak,
      dailyFocusTarget: dailyFocusTarget ?? this.dailyFocusTarget,
      lastActiveDate: lastActiveDate ?? this.lastActiveDate,
      isDarkMode: isDarkMode ?? this.isDarkMode,
    );
  }
}
