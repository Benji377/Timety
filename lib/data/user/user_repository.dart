import 'user.dart';

abstract class UserRepository {
  /// Initializes the repository and handles any data migrations
  Future<void> init();

  /// Gets the current user profile
  UserProfile? getUser();

  /// Saves the entire user profile
  Future<void> saveUser(UserProfile user);

  /// Helper: Updates just the user's name
  Future<void> updateName(String name);

  /// Helper: Updates just the user's profile image
  Future<void> updateProfileImage(String path);

  /// Helper: Adds XP to the user's total
  Future<void> addXp(int amount);
}