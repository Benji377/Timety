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

class UserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = userRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addXp(amount: Int) {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                userRepository.updateUserProfile(current.copy(totalXp = current.totalXp + amount))
            } else {
                userRepository.insertUserProfile(
                    UserProfileEntity(
                        name = "Bobert",
                        accountCreated = Instant.now(),
                        totalXp = amount
                    )
                )
            }
        }
    }
}
