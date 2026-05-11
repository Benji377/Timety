import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:timety/data/user/user.dart';
import 'package:timety/data/user/user_repository_hive.dart';

import '../test_support/hive_test_utils.dart';

void main() {
  late Directory hiveDir;

  setUpAll(() async {
    hiveDir = await initializeHiveTestDir();
  });

  tearDownAll(() async {
    await disposeHiveTestDir(hiveDir);
  });

  test('creates a default user and mutates the stored profile', () async {
    final repository = HiveUserRepository();
    await repository.init();

    final user = repository.getUser();

    expect(user, isNotNull);
    expect(user!.name, 'Bobert');
    expect(user.totalXp, 0);

    await repository.saveUser(
      UserProfile(
        name: 'Avery',
        profileImagePath: '/tmp/avatar.png',
        accountCreated: DateTime(2024),
        totalXp: 125,
      ),
    );

    final savedUser = repository.getUser();
    expect(savedUser!.name, 'Avery');
    expect(savedUser.profileImagePath, '/tmp/avatar.png');
    expect(savedUser.totalXp, 125);

    await repository.updateName('Morgan');
    await repository.updateProfileImage('/tmp/new-avatar.png');
    await repository.addXp(50);
    await repository.addXp(-500);

    final updatedUser = repository.getUser();
    expect(updatedUser!.name, 'Morgan');
    expect(updatedUser.profileImagePath, '/tmp/new-avatar.png');
    expect(updatedUser.totalXp, 0);

    final box = await Hive.openBox<UserProfile>(HiveUserRepository.boxName);
    expect(box.length, 1);
    expect(box.getAt(0)?.name, 'Morgan');
  });
}
