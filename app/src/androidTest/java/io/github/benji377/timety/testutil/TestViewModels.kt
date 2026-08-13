package io.github.benji377.timety.testutil

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.benji377.timety.ui.viewmodel.DayRatingViewModel
import io.github.benji377.timety.ui.viewmodel.FocusViewModel
import io.github.benji377.timety.ui.viewmodel.GoalViewModel
import io.github.benji377.timety.ui.viewmodel.HabitViewModel
import io.github.benji377.timety.ui.viewmodel.QuickHabitViewModel
import io.github.benji377.timety.ui.viewmodel.RecurringTaskViewModel
import io.github.benji377.timety.ui.viewmodel.SettingsViewModel
import io.github.benji377.timety.ui.viewmodel.TaskViewModel
import io.github.benji377.timety.ui.viewmodel.UserViewModel

/**
 * Builds the app's view models against a [TestAppContainer] instead of
 * [TimetyApplication.container][io.github.benji377.timety.TimetyApplication.container].
 *
 * Deliberately mirrors `AppViewModelProvider.Factory` rather than reusing it: that factory reads
 * the application's real container, and swapping that field races the coroutines
 * `TimetyApplication.onCreate` launches. Constructing view models directly avoids the race
 * entirely. Keep the `when` below in sync with the production factory.
 *
 * View models are resolved through a real [ViewModelStore] so [clear] cancels every
 * `viewModelScope`; otherwise a coroutine from one test outlives its closed database and fails the
 * next one.
 */
class TestViewModels(
    private val application: Application,
    private val container: TestAppContainer,
) {
    private val store = ViewModelStore()

    val provider: ViewModelProvider = ViewModelProvider(
        store,
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val viewModel = when {
                    modelClass.isAssignableFrom(TaskViewModel::class.java) ->
                        TaskViewModel(
                            application,
                            container.taskRepository,
                            container.userRepository,
                        )

                    modelClass.isAssignableFrom(RecurringTaskViewModel::class.java) ->
                        RecurringTaskViewModel(
                            application,
                            container.recurringTaskRepository,
                            container.userRepository,
                        )

                    modelClass.isAssignableFrom(HabitViewModel::class.java) ->
                        HabitViewModel(
                            application,
                            container.habitRepository,
                            container.userRepository,
                        )

                    modelClass.isAssignableFrom(QuickHabitViewModel::class.java) ->
                        QuickHabitViewModel(application, container.quickHabitRepository)

                    modelClass.isAssignableFrom(GoalViewModel::class.java) ->
                        GoalViewModel(container.goalRepository, container.userRepository)

                    modelClass.isAssignableFrom(FocusViewModel::class.java) ->
                        FocusViewModel(
                            application,
                            container.focusRepository,
                            container.userRepository,
                            container.habitRepository,
                            container.settingsRepository,
                        )

                    modelClass.isAssignableFrom(UserViewModel::class.java) ->
                        UserViewModel(container.userRepository)

                    modelClass.isAssignableFrom(DayRatingViewModel::class.java) ->
                        DayRatingViewModel(container.dayRatingRepository)

                    modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                        SettingsViewModel(application, container.settingsRepository)

                    else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
                }
                return modelClass.cast(viewModel)!!
            }
        },
    )

    /** Cancels every `viewModelScope` built through this holder. */
    fun clear() = store.clear()
}

/** Resolves (and caches) a view model from this holder. */
inline fun <reified VM : ViewModel> TestViewModels.get(): VM = provider[VM::class.java]
