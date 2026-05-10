import 'user.dart';

abstract class UserRepository {
  Future<void> init();
  UserProfile? getUser();
  Future<void> saveUser(UserProfile user);
  Future<void> updateName(String name);
  Future<void> updateProfileImage(String path);
  Future<void> addXp(int amount);
}
