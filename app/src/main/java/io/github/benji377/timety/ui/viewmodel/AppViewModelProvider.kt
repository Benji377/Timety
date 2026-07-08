package io.github.benji377.timety.ui.viewmodel

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.benji377.timety.TimetyApplication


/**
 * Returns a [VM] scoped to the current activity rather than the current navigation destination,
 * so the same instance is shared across composables/screens.
 */
@Composable
inline fun <reified VM : ViewModel> activityScopedViewModel(): VM {
    val activity = checkNotNull(LocalActivity.current as? ComponentActivity) {
        "activityScopedViewModel requires a ComponentActivity host"
    }
    return viewModel(viewModelStoreOwner = activity, factory = AppViewModelProvider.Factory)
}

/** Factory that builds each app [ViewModel] with its repositories from [TimetyApplication.container]. */
object AppViewModelProvider {
    val Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val application = checkNotNull(extras[APPLICATION_KEY]) as TimetyApplication
            val container = application.container

            val viewModel = when {
                modelClass.isAssignableFrom(TaskViewModel::class.java) -> {
                    TaskViewModel(
                        application,
                        container.taskRepository,
                        container.userRepository
                    )
                }

                modelClass.isAssignableFrom(HabitViewModel::class.java) -> {
                    HabitViewModel(
                        application,
                        container.habitRepository,
                        container.userRepository
                    )
                }

                modelClass.isAssignableFrom(FocusViewModel::class.java) -> {
                    FocusViewModel(
                        application,
                        container.focusRepository,
                        container.userRepository,
                        container.habitRepository,
                        container.settingsRepository
                    )
                }

                modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                    UserViewModel(container.userRepository)
                }

                modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                    SettingsViewModel(
                        application,
                        container.settingsRepository
                    )
                }

                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
            return modelClass.cast(viewModel)!!
        }
    }
}
