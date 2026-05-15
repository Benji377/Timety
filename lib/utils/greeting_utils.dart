/// Utility functions for generating contextual greetings
class GreetingUtils {
  const GreetingUtils._();

  /// Generates a contextual greeting based on time of day and day of week
  ///
  /// Returns a personalized greeting message that varies by:
  /// - Time of day (morning/afternoon/evening)
  /// - Day of week (special messages for Monday, Friday, Sunday)
  static String getGreeting(String name) {
    final now = DateTime.now();
    final hour = now.hour;
    final weekday = now.weekday;

    String greeting;
    if (hour < 12) {
      greeting = "Good Morning, $name!";
    } else if (hour < 17) {
      greeting = "Good Afternoon, $name!";
    } else {
      greeting = "Good Evening, $name!";
    }

    // Add special day-specific messages
    if (weekday == DateTime.monday && hour < 12) {
      return "$greeting Let's crush this week!";
    } else if (weekday == DateTime.friday && hour > 15) {
      return "$greeting The weekend is almost here!";
    } else if (weekday == DateTime.sunday) {
      return "$greeting Take it easy today!";
    }

    return greeting;
  }
}
