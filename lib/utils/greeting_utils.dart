/// Utility functions for generating contextual greetings
class GreetingUtils {
  const GreetingUtils._();

  /// Generates a short contextual greeting based on time of day.
  static String getGreeting(String name) {
    final hour = DateTime.now().hour;

    if (hour < 5) return "Still awake, $name?";
    if (hour < 12) return "Good morning, $name!";
    if (hour < 17) return "Good afternoon, $name!";
    if (hour < 21) return "Good evening, $name!";
    return "Good night, $name!";
  }

  /// Returns a short home-screen title based on the time of day.
  static String getDailyMotivationText() {
    final hour = DateTime.now().hour;

    if (hour < 5) return 'Take it easy and get some well-deserved rest.';
    if (hour < 12) return 'Start the day with a simple task!';
    if (hour < 17) return 'Keep going strong!';
    if (hour < 21) return 'Take a deep breath and enjoy your evening.';
    return 'Rest well, tomorrow is a fresh start.';
  }
}
