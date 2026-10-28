package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Exposes the user's profile, creating a default profile on first launch. */
class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    val userProfile: StateFlow<UserProfileEntity?> = userRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            userRepository.initializeIfNeeded()
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                userRepository.updateUserProfile(current.copy(name = newName))
            } else {
                userRepository.insertUserProfile(
                    UserProfileEntity(
                        name = newName,
                        accountCreated = Instant.now()
                    )
                )
            }
        }
    }

    fun updateProfileImage(path: String) {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                userRepository.updateUserProfile(current.copy(profileImagePath = path))
            } else {
                userRepository.insertUserProfile(
                    UserProfileEntity(
                        name = "Bobert",
                        profileImagePath = path,
                        accountCreated = Instant.now()
                    )
                )
            }
        }
    }
}
