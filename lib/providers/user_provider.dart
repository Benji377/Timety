import 'package:flutter/material.dart';
import '../data/user/user.dart';
import '../data/user/user_repository.dart';
import '../utils/logic/xp_utils.dart';

class UserProvider extends ChangeNotifier {
  final UserRepository repository;
  UserProfile? _profile;

  UserProvider({required this.repository}) {
    _init();
  }

  Future<void> _init() async {
    await repository.init();
    _profile = repository.getUser();
    notifyListeners();
  }

  // --- GETTERS ---
  String get name => _profile?.name ?? "Bobert";
  String? get profileImagePath => _profile?.profileImagePath;
  int get totalXp => _profile?.totalXp ?? 0;
  DateTime get accountCreated => _profile?.accountCreated ?? DateTime.now();
  int get currentLevel => ExperienceEngine.calculateLevel(totalXp);
  String get levelTitle => ExperienceEngine.getTitle(currentLevel);
  double get levelProgress => ExperienceEngine.getLevelProgress(totalXp);

  // --- ACTIONS ---
  Future<void> updateName(String newName) async {
    await repository.updateName(newName);
    _profile?.name = newName;
    notifyListeners();
  }

  Future<void> updateProfileImage(String path) async {
    await repository.updateProfileImage(path);
    _profile?.profileImagePath = path;
    notifyListeners();
  }

  Future<void> addXp(int amount) async {
    if (_profile == null) return;

    await repository.addXp(amount);

    notifyListeners();
  }
}
