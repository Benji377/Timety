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

  Future<void> addXp(int amount) async {
    if (_user != null) {
      int newXp = _user!.xp + amount;
      int newLevel = _user!.level;
      // Simple level up logic: 1000 XP per level
      if (newXp >= 1000) {
        newLevel += newXp ~/ 1000;
        newXp %= 1000;
      }
      final updatedUser = _user!.copyWith(xp: newXp, level: newLevel);
      await updateUser(updatedUser);
    }
  }
}
