import 'package:hive/hive.dart';

part 'user.g.dart';

@HiveType(typeId: 40)
class UserProfile extends HiveObject {
  @HiveField(0)
  String name;

  @HiveField(1)
  String? profileImagePath;

  @HiveField(2)
  DateTime accountCreated;

  @HiveField(3)
  List<String> unlockedAchievements;

  @HiveField(4)
  int totalXp;

  UserProfile({
    required this.name,
    this.profileImagePath,
    required this.accountCreated,
    this.unlockedAchievements = const [],
    this.totalXp = 0,
  });
}
