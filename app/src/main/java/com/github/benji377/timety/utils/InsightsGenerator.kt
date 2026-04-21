package com.github.benji377.timety.utils

import com.github.benji377.timety.data.FocusSession
import java.util.Calendar

/**
 * Generates insights based on focus session data.
 */
object InsightsGenerator {

    fun generateInsights(sessions: List<FocusSession>): List<String> {
        if (sessions.isEmpty()) {
            return listOf("Start focusing to unlock insights!")
        }

        val insights = mutableListOf<String>()

        // Insight 1: Total focus time
        val totalMinutes = sessions.sumOf { it.duration / 60000L }
        if (totalMinutes > 0) {
            insights.add("You've spent $totalMinutes minutes focusing 🎯")
        }

        // Insight 2: Best time of day
        val timeOfDayBreakdown = sessions.groupBy { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
            when (cal.get(Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "morning"
                in 12..17 -> "afternoon"
                in 18..21 -> "evening"
                else -> "night"
            }
        }

        val bestTimeOfDay =
            timeOfDayBreakdown.maxByOrNull { it.value.sumOf { s -> s.duration } }?.key
        if (bestTimeOfDay != null) {
            val timeLabel = when (bestTimeOfDay) {
                "morning" -> "Morning"
                "afternoon" -> "Afternoon"
                "evening" -> "Evening"
                else -> "Night"
            }
            insights.add("You're most focused in the $timeLabel ⏰")
        }

        // Insight 3: Streak milestone
        val recentSessions = sessions.sortedByDescending { it.startTime }.take(7)
        if (recentSessions.size >= 5) {
            insights.add("You're on a focus spree! Keep it up 🔥")
        }

        // Insight 4: Average session length
        val avgSessionMinutes = (sessions.sumOf { it.duration } / sessions.size) / 60000L
        if (avgSessionMinutes > 0) {
            val sessionLabel =
                if (avgSessionMinutes < 30) "short" else if (avgSessionMinutes < 90) "medium" else "long"
            insights.add("Your average focus session is $sessionLabel (~$avgSessionMinutes min)")
        }

        // Insight 5: Rating analysis
        val ratings = sessions.groupBy { it.rating }
        val highRatingPercentage = ratings.filterKeys { it?.name == "GREAT" }
            .values.sumOf { it.size } * 100 / sessions.size
        if (highRatingPercentage >= 70) {
            insights.add("Great focus quality! You're consistently in the zone 💪")
        } else if (highRatingPercentage < 30) {
            insights.add("Try shorter sessions for better focus quality 🎯")
        }

        return insights.take(3) // Return top 3 insights
    }
}

