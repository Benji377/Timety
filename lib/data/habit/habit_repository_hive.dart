import 'package:hive/hive.dart';
import 'habit_models.dart';
import 'habit_repository.dart';

class HiveHabitRepository implements HabitRepository {
  static const String boxName = 'habitsBox';

  Future<Box<Habit>> get _box async => await Hive.openBox<Habit>(boxName);

  @override
  Future<List<Habit>> fetchHabits() async {
    final box = await _box;
    return box.values.toList();
  }

  @override
  Future<void> saveHabit(Habit habit) async {
    final box = await _box;
    await box.put(habit.id, habit);
  }

  @override
  Future<void> deleteHabit(String id) async {
    final box = await _box;
    await box.delete(id);
  }

  @override
  Future<void> clearAll() async {
    final box = await _box;
    await box.clear();
  }
}