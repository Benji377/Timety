import '../data/focus_session.dart';

class InsightsGenerator {
  static List<String> generateInsights(List<FocusSession> sessions) {
    if (sessions.isEmpty) {
      return ["Start focusing to unlock insights!"];
    }

    final insights = <String>[];

    // Insight 1: Total focus time
    final totalMinutes = sessions.fold(0, (sum, s) => sum + (s.duration ~/ 60000));
    if (totalMinutes > 0) {
      insights.add("You've spent $totalMinutes minutes focusing 🎯");
    }

    // Insight 2: Best time of day
    final timeOfDayBreakdown = <String, int>{};
    for (var session in sessions) {
      final hour = DateTime.fromMillisecondsSinceEpoch(session.startTime).hour;
      String timeOfDay;
      if (hour >= 5 && hour <= 11) {
        timeOfDay = "morning";
      } else if (hour >= 12 && hour <= 17) {
        timeOfDay = "afternoon";
      } else if (hour >= 18 && hour <= 21) {
        timeOfDay = "evening";
      } else {
        timeOfDay = "night";
      }
      timeOfDayBreakdown[timeOfDay] = (timeOfDayBreakdown[timeOfDay] ?? 0) + session.duration;
    }

    if (timeOfDayBreakdown.isNotEmpty) {
      final bestTimeOfDay = timeOfDayBreakdown.entries.reduce((a, b) => a.value > b.value ? a : b).key;
      final timeLabel = bestTimeOfDay[0].toUpperCase() + bestTimeOfDay.substring(1);
      insights.add("You're most focused in the $timeLabel ⏰");
    }

    // Insight 3: Streak milestone
    if (sessions.length >= 5) {
      insights.add("You're on a focus spree! Keep it up 🔥");
    }

    // Insight 4: Average session length
    final avgSessionMinutes = totalMinutes ~/ sessions.length;
    if (avgSessionMinutes > 0) {
      final sessionLabel = avgSessionMinutes < 30 ? "short" : (avgSessionMinutes < 90 ? "medium" : "long");
      insights.add("Your average focus session is $sessionLabel (~$avgSessionMinutes min)");
    }

    return insights.take(3).toList();
  }
}
