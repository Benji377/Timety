package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.user.UserProfileEntity
import io.github.benji377.timety.data.repository.UserRepository
import io.github.benji377.timety.util.stats.ExperienceEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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


    val currentLevel: StateFlow<Int> = userProfile
        .map { ExperienceEngine.calculateLevel(it?.totalXp ?: 0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExperienceEngine.calculateLevel(0)
        )


    val levelTitle: StateFlow<String> = currentLevel
        .map { ExperienceEngine.getTitle(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExperienceEngine.getTitle(ExperienceEngine.calculateLevel(0))
        )


    val levelProgress: StateFlow<Double> = userProfile
        .map { ExperienceEngine.getLevelProgress(it?.totalXp ?: 0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExperienceEngine.getLevelProgress(0)
        )

    init {
        viewModelScope.launch {
            userRepository.initializeIfNeeded()
        }
    }

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

    fun updateName(newName: String) {
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                userRepository.updateUserProfile(current.copy(name = newName))
            } else {
                userRepository.insertUserProfile(
                    UserProfileEntity(
                        name = newName,
                        accountCreated = Instant.now(),
                        totalXp = 0
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
                        accountCreated = Instant.now(),
                        totalXp = 0
                    )
                )
            }
        }
    }
}
