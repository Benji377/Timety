import 'package:flutter/material.dart';
import '../data/main_repository.dart';
import '../data/user.dart';

class UserProvider with ChangeNotifier {
  final MainRepository _repository;
  User? _user;

  UserProvider(this._repository) {
    _loadUser();
  }

  User? get user => _user;
  bool get isDarkMode => _user?.isDarkMode ?? false;

  Future<void> _loadUser() async {
    _user = await _repository.getUser();
    notifyListeners();
  }

  Future<void> updateUser(User user) async {
    await _repository.insertOrUpdateUser(user);
    _user = user;
    notifyListeners();
  }

  Future<void> toggleDarkMode() async {
    if (_user != null) {
      final updatedUser = _user!.copyWith(isDarkMode: !_user!.isDarkMode);
      await updateUser(updatedUser);
    }
  }

  /// Calculate XP needed for next level (exponential progression)
  /// Level 1-5: 1000 XP, Level 6-10: 1500 XP, Level 11+: 2000 XP
  int _getXpForLevel(int level) {
    if (level <= 5) return 1000;
    if (level <= 10) return 1500;
    return 2000;
  }

  /// Add XP with streak bonus. Streak bonus = 1% per streak day, capped at 50% (max 50 day streak bonus)
  Future<void> addXp(int amount, {int streakBonus = 0}) async {
    if (_user == null) return;

    // Calculate streak boost (capped at 50% max)
    final streakBoost = (streakBonus / 100).clamp(0, 0.5);
    final totalXp = (amount * (1 + streakBoost)).toInt();

    int newXp = _user!.xp + totalXp;
    int newLevel = _user!.level;

    // Level up logic - each level requires exponentially more XP
    while (newXp >= _getXpForLevel(newLevel)) {
      newXp -= _getXpForLevel(newLevel);
      newLevel++;
    }

    final updatedUser = _user!.copyWith(xp: newXp, level: newLevel);
    await updateUser(updatedUser);
  }

  /// Check and update streak. Returns true if streak requirement was met today.
  /// Requirement: Either a focus session of minStreakMinutes or a completed task
  Future<bool> checkAndUpdateStreak({
    required int todayFocusMinutes,
    required bool completedTaskToday,
  }) async {
    if (_user == null) return false;

    final lastActiveDate = DateTime.fromMillisecondsSinceEpoch(
      _user!.lastActiveDate,
    );
    final today = DateTime.now();
    final dateToday = DateTime(today.year, today.month, today.day);
    final dateLastActive = DateTime(
      lastActiveDate.year,
      lastActiveDate.month,
      lastActiveDate.day,
    );

    // Check if we already updated today
    if (dateLastActive == dateToday) {
      return todayFocusMinutes >= _user!.minStreakMinutes || completedTaskToday;
    }

    final daysSinceLastActive = dateToday.difference(dateLastActive).inDays;
    bool streakMaintained = false;

    if (daysSinceLastActive == 1) {
      // Consecutive day - maintain or break streak
      if (todayFocusMinutes >= _user!.minStreakMinutes || completedTaskToday) {
        streakMaintained = true;
      }
    } else if (daysSinceLastActive <= 2 &&
        _user!.streakFrozenDaysRemaining > 0) {
      // Within grace period (2 days max) - freeze countdown
      int newFrozenDays =
          _user!.streakFrozenDaysRemaining - daysSinceLastActive;
      newFrozenDays = newFrozenDays.clamp(0, 2);

      if (todayFocusMinutes >= _user!.minStreakMinutes || completedTaskToday) {
        // User recovered! Unfreeze and restore streak
        final updatedUser = _user!.copyWith(
          streakFrozenDaysRemaining: 0,
          lastActiveDate: today.millisecondsSinceEpoch,
        );
        await updateUser(updatedUser);
        return true;
      } else {
        // Still no activity - continue freezing
        final updatedUser = _user!.copyWith(
          streakFrozenDaysRemaining: newFrozenDays,
          lastActiveDate: today.millisecondsSinceEpoch,
        );
        await updateUser(updatedUser);
        return false;
      }
    } else {
      // Streak broken - too many days without activity
      final updatedUser = _user!.copyWith(
        currentStreak: 0,
        streakFrozenDaysRemaining: 0,
        lastActiveDate: today.millisecondsSinceEpoch,
      );
      await updateUser(updatedUser);
      return false;
    }

    // Update user if streak maintained
    if (streakMaintained &&
        (todayFocusMinutes >= _user!.minStreakMinutes || completedTaskToday)) {
      final newStreak = _user!.currentStreak + 1;
      final newHighestStreak = newStreak > _user!.highestStreak
          ? newStreak
          : _user!.highestStreak;
      final updatedUser = _user!.copyWith(
        currentStreak: newStreak,
        highestStreak: newHighestStreak,
        streakFrozenDaysRemaining: 0,
        lastActiveDate: today.millisecondsSinceEpoch,
      );
      await updateUser(updatedUser);
      return true;
    } else if (todayFocusMinutes >= _user!.minStreakMinutes ||
        completedTaskToday) {
      // First day of new streak after break
      final updatedUser = _user!.copyWith(
        currentStreak: 1,
        streakFrozenDaysRemaining: 0,
        lastActiveDate: today.millisecondsSinceEpoch,
      );
      await updateUser(updatedUser);
      return true;
    }

    return false;
  }
}
