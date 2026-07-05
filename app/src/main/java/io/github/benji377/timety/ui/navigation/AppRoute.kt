package io.github.benji377.timety.ui.navigation


enum class AppRoute(val route: String) {
    FOCUS_MODES("focus_modes"),
    FOCUS_TAGS("focus_tags"),
    SETTINGS("settings"),
    TASK_CATEGORIES("task_categories"),
    TASK_DETAIL("task_detail"),
    HABIT_DETAIL("habit_detail");

    companion object {
        const val ARG_TASK_ID = "taskId"
        const val ARG_HABIT_ID = "habitId"


        val TASK_DETAIL_WITH_ID = "${TASK_DETAIL.route}/{$ARG_TASK_ID}"


        val HABIT_DETAIL_WITH_ID = "${HABIT_DETAIL.route}/{$ARG_HABIT_ID}"


        fun taskDetail(taskId: String?): String =
            if (taskId == null) TASK_DETAIL.route else "${TASK_DETAIL.route}/$taskId"


        fun habitDetail(habitId: String?): String =
            if (habitId == null) HABIT_DETAIL.route else "${HABIT_DETAIL.route}/$habitId"
    }
}
