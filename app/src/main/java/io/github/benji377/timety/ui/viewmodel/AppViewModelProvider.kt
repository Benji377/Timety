package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.benji377.timety.TimetyApplication

object AppViewModelProvider {
    val Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            val application = checkNotNull(extras[APPLICATION_KEY]) as TimetyApplication
            val container = application.container

            return when {
                modelClass.isAssignableFrom(TaskViewModel::class.java) -> {
                    TaskViewModel(container.taskRepository) as T
                }
                modelClass.isAssignableFrom(HabitViewModel::class.java) -> {
                    HabitViewModel(container.habitRepository) as T
                }
                modelClass.isAssignableFrom(FocusViewModel::class.java) -> {
                    FocusViewModel(container.focusRepository) as T
                }
                modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                    UserViewModel(container.userRepository) as T
                }
                else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
