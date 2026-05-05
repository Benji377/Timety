import 'habit_models.dart';

abstract class HabitRepository {
  Future<List<Habit>> fetchHabits();
  Future<void> saveHabit(Habit habit);
  Future<void> deleteHabit(String id);
  Future<void> clearAll();
}