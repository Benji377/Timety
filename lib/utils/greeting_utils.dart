/// Utility functions for generating contextual greetings
class GreetingUtils {
  const GreetingUtils._();

  /// Generates a short contextual greeting based on time of day.
  static String getGreeting(String name) {
    final now = DateTime.now();
    final hour = now.hour;

    String greeting;
    if (hour < 12) {
      greeting = "Good Morning, $name!";
    } else if (hour < 17) {
      greeting = "Good Afternoon, $name!";
    } else {
      greeting = "Good Evening, $name!";
    }

    return greeting;
  }

  /// Returns a short home-screen title based on the time of day.
  static String getDailyMotivationText() {
    final hour = DateTime.now().hour;
    if (hour < 12) return 'Start small. Stay steady.';
    if (hour < 17) return 'Keep moving. Keep it clean.';
    return 'Finish strong. Reset well.';
  }
}
