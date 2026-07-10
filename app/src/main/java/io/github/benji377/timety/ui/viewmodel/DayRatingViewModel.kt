package io.github.benji377.timety.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.user.DayRatingEntity
import io.github.benji377.timety.data.repository.DayRatingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Exposes the end-of-day ratings logged from the evening checkup notification. */
class DayRatingViewModel(
    dayRatingRepository: DayRatingRepository
) : ViewModel() {

    val allRatings: StateFlow<List<DayRatingEntity>> = dayRatingRepository.allRatings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
