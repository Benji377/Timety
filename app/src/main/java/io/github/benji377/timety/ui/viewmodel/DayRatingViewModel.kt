package io.github.benji377.timety.ui.viewmodel

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
import io.github.benji377.timety.data.repository.DayRatingRepository
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.services.NotificationService
import io.github.benji377.timety.util.datetime.AppDateFormatUtils
import io.github.benji377.timety.util.datetime.AppDateUtils
import io.github.benji377.timety.util.stats.MoodStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** The day ratings behind the profile's mood calendar, and rating or clearing any past day. */
class DayRatingViewModel(
    private val application: Application,
    private val dayRatingRepository: DayRatingRepository,
    settingsRepository: SettingsRepository,
) : AndroidViewModel(application) {

    val ratingsByDay: StateFlow<Map<LocalDate, DayRating>> = dayRatingRepository.allRatings
        .map(MoodStats::byDay)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    /** When the evening checkup fires; the rate-today prompt shows from then on. */
    val checkupTime: StateFlow<LocalTime> = settingsRepository.endOfDayCheckupTimeFlow
        .map { time ->
            val (hour, minute) = AppDateFormatUtils.parseHHmm(time, defaultHour = 20, defaultMinute = 0)
            LocalTime.of(hour, minute)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LocalTime.of(20, 0)
        )

    fun setRating(day: LocalDate, rating: DayRating) = viewModelScope.launch {
        dayRatingRepository.upsert(
            DayRatingEntity(
                dayKey = AppDateUtils.dayKey(day),
                rating = rating.value,
                createdAt = Instant.now(),
            )
        )
        // Today is answered, so the checkup no longer needs to ask.
        if (day == LocalDate.now()) {
            NotificationManagerCompat.from(application).cancel(NotificationService.END_OF_DAY_CHECKUP_ID)
        }
    }

    fun clearRating(day: LocalDate) = viewModelScope.launch {
        dayRatingRepository.delete(AppDateUtils.dayKey(day))
    }
}
