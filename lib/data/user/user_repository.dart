import './user.dart';

/// Defines the contract for user data management
abstract class UserRepository {
  /// Initializes the user repository, does nothing if a user already exists
  Future<void> init();

  /// Fetches the current user profile, or null if no user is logged in
  UserProfile? getUser();

  /// Saves the given user profile, replacing any existing profile
  Future<void> saveUser(UserProfile user);

  /// Updates the user's name and saves the profile
  Future<void> updateName(String name);

  /// Updates the user's profile image path and saves the profile
  Future<void> updateProfileImage(String path);

  /// Adds the specified amount of XP to the user's profile and saves it
  Future<void> addXp(int amount);
}
