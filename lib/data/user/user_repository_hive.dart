import 'package:hive_flutter/hive_flutter.dart';
import './user.dart';
import './user_repository.dart';

class HiveUserRepository implements UserRepository {
  static const String boxName = 'userProfileBox';
  late Box<UserProfile> _box;

  @override
  Future<void> init() async {
    _box = await Hive.openBox<UserProfile>(boxName);

    // Check if the box is empty (New User)
    if (_box.isEmpty) {
      final profile = UserProfile(
        name: "Bobert",
        accountCreated: DateTime.now(),
      );

      // Save it to Hive as the one and only profile (Index 0)
      await _box.add(profile);
    }
  }

  @override
  UserProfile? getUser() {
    if (_box.isEmpty) return null;
    return _box.getAt(0); // The profile is always at index 0
  }

  @override
  Future<void> saveUser(UserProfile user) async {
    if (_box.isEmpty) {
      await _box.add(user);
    } else {
      await _box.putAt(0, user);
    }
  }

  @override
  Future<void> updateName(String name) async {
    final user = getUser();
    if (user != null) {
      user.name = name;
      await user.save();
    }
  }

  @override
  Future<void> updateProfileImage(String path) async {
    final user = getUser();
    if (user != null) {
      user.profileImagePath = path;
      await user.save();
    }
  }

  @override
  Future<void> addXp(int amount) async {
    final user = getUser();
    if (user != null) {
      user.totalXp += amount;
      if (user.totalXp < 0) user.totalXp = 0;
      await user.save();
    }
  }
}
