import 'package:flutter_test/flutter_test.dart';
import 'package:timety/data/user/user.dart';

void main() {
  test('UserProfile stores provided values and mutable state', () {
    final user = UserProfile(
      name: 'Avery',
      profileImagePath: '/tmp/avatar.png',
      accountCreated: DateTime(2024),
      unlockedAchievements: ['first_task'],
      totalXp: 250,
    );

    expect(user.name, 'Avery');
    expect(user.profileImagePath, '/tmp/avatar.png');
    expect(user.accountCreated, DateTime(2024));
    expect(user.unlockedAchievements, ['first_task']);
    expect(user.totalXp, 250);

    user.name = 'Morgan';
    user.profileImagePath = null;
    user.totalXp += 50;

    expect(user.name, 'Morgan');
    expect(user.profileImagePath, isNull);
    expect(user.totalXp, 300);
  });
}
