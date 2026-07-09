package io.github.benji377.timety.ui.navigation


/** Navigation destinations for the app's `NavHost`, paired with their route path segment. */
enum class AppRoute(val route: String) {
    FOCUS_MODES("focus_modes"),
    FOCUS_TAGS("focus_tags"),
    SETTINGS("settings"),
    TASK_CATEGORIES("task_categories"),
    QUICK_HABITS("quick_habits"),
    TASK_DETAIL("task_detail"),
    HABIT_DETAIL("habit_detail");

    companion object {
        const val ARG_TASK_ID = "taskId"
        const val ARG_HABIT_ID = "habitId"

        /** Route pattern for [TASK_DETAIL] with a required task ID argument. */
        val TASK_DETAIL_WITH_ID = "${TASK_DETAIL.route}/{$ARG_TASK_ID}"

        /** Route pattern for [HABIT_DETAIL] with a required habit ID argument. */
        val HABIT_DETAIL_WITH_ID = "${HABIT_DETAIL.route}/{$ARG_HABIT_ID}"

        /** Builds the task detail route, omitting the ID segment to navigate to a blank/new task. */
        fun taskDetail(taskId: String?): String =
            if (taskId == null) TASK_DETAIL.route else "${TASK_DETAIL.route}/$taskId"

        /** Builds the habit detail route, omitting the ID segment to navigate to a blank/new habit. */
        fun habitDetail(habitId: String?): String =
            if (habitId == null) HABIT_DETAIL.route else "${HABIT_DETAIL.route}/$habitId"
    }
}
