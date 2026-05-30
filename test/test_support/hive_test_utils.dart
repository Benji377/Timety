import 'dart:io';

import 'package:hive/hive.dart';
import 'package:timety/data/focus/focus_models.dart';
import 'package:timety/data/habit/habit_models.dart';
import 'package:timety/data/task/task.dart';
import 'package:timety/data/user/user.dart';

Future<Directory> initializeHiveTestDir() async {
  final directory = await Directory.systemTemp.createTemp('timety_hive_test_');
  Hive.init(directory.path);
  registerAllHiveAdapters();
  return directory;
}

Future<void> disposeHiveTestDir(Directory directory) async {
  await Hive.close();
  if (await directory.exists()) {
    await directory.delete(recursive: true);
  }
}

void registerAllHiveAdapters() {
  if (!Hive.isAdapterRegistered(10)) Hive.registerAdapter(TaskAdapter());
  if (!Hive.isAdapterRegistered(11)) Hive.registerAdapter(PriorityAdapter());
  if (!Hive.isAdapterRegistered(12)) Hive.registerAdapter(SizeAdapter());
  if (!Hive.isAdapterRegistered(13)) Hive.registerAdapter(SubtaskAdapter());

  if (!Hive.isAdapterRegistered(20)) {
    Hive.registerAdapter(FocusSessionAdapter());
  }
  if (!Hive.isAdapterRegistered(21)) {
    Hive.registerAdapter(FocusModeTypeAdapter());
  }
  if (!Hive.isAdapterRegistered(22)) Hive.registerAdapter(PhaseTypeAdapter());
  if (!Hive.isAdapterRegistered(23)) {
    Hive.registerAdapter(SessionPhaseAdapter());
  }
  if (!Hive.isAdapterRegistered(24)) Hive.registerAdapter(FocusModeAdapter());
  if (!Hive.isAdapterRegistered(25)) Hive.registerAdapter(DistractionAdapter());
  if (!Hive.isAdapterRegistered(26)) Hive.registerAdapter(FocusTagAdapter());
  if (!Hive.isAdapterRegistered(27)) {
    Hive.registerAdapter(FocusTargetTypeAdapter());
  }

  if (!Hive.isAdapterRegistered(30)) Hive.registerAdapter(HabitAdapter());
  if (!Hive.isAdapterRegistered(31)) {
    Hive.registerAdapter(HabitFrequencyAdapter());
  }

  if (!Hive.isAdapterRegistered(40)) Hive.registerAdapter(UserProfileAdapter());
}
