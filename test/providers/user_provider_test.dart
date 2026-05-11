import 'package:flutter_test/flutter_test.dart';

import 'package:timety/data/user/user.dart';
import 'package:timety/providers/user_provider.dart';

import '../test_support/fakes.dart';

void main() {
  test('loads a profile and updates derived level state', () async {
    final repository = FakeUserRepository(
      initialUser: UserProfile(
        name: 'Avery',
        accountCreated: DateTime(2024),
        totalXp: 150,
      ),
    );
    final provider = UserProvider(repository: repository);

    await drainEventQueue();

    expect(repository.initCalls, 1);
    expect(provider.name, 'Avery');
    expect(provider.totalXp, 150);
    expect(provider.currentLevel, 2);
    expect(provider.levelTitle, 'Novice Planner');
    expect(provider.levelProgress, closeTo(0.166666, 0.0005));

    await provider.updateName('Morgan');
    await provider.updateProfileImage('/tmp/avatar.png');
    await provider.addXp(50);

    expect(provider.name, 'Morgan');
    expect(provider.profileImagePath, '/tmp/avatar.png');
    expect(provider.totalXp, 200);
  });
}
