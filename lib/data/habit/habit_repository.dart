import 'habit_models.dart';

/// Repository interface for managing habits
abstract class HabitRepository {
  /// Fetches all habits from the data source.
  Future<List<Habit>> fetchHabits();

  /// Saves a habit to the data source. If the habit already exists, it will be updated.
  Future<void> saveHabit(Habit habit);

  /// Deletes a habit by its unique identifier.
  Future<void> deleteHabit(String id);

  /// Clears all habits from the data source. Used for testing and resetting data.
  Future<void> clearAll();
}
