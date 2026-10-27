package io.github.benji377.timety.services

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.benji377.timety.R
import io.github.benji377.timety.TimetyApplication
import io.github.benji377.timety.data.model.user.DayRating
import io.github.benji377.timety.data.model.user.DayRatingEntity
import io.github.benji377.timety.util.datetime.AppDateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/**
 * Drives the evening checkup flow end to end: the reminder broadcast posts a notification
 * carrying the three day-rating actions, and tapping one stores the rating and dismisses it.
 * Once today is rated, the checkup comes without the actions.
 */
@RunWith(AndroidJUnit4::class)
class EndOfDayNotificationTest {

    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext
    private val manager: NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private val ratings get() = (context.applicationContext as TimetyApplication).container.dayRatingRepository
    private val todayKey get() = AppDateUtils.dayKey(LocalDate.now())

    @Before
    fun clearNotificationAndTodaysRating() {
        manager.cancel(NotificationService.END_OF_DAY_CHECKUP_ID)
        runBlocking { ratings.delete(todayKey) }
    }

    private fun sendCheckup() {
        context.sendBroadcast(
            Intent(context, ReminderReceiver::class.java)
                .putExtra(NotificationService.EXTRA_NOTIFICATION_ID, NotificationService.END_OF_DAY_CHECKUP_ID)
                .putExtra(NotificationService.EXTRA_TITLE, "Evening checkup")
                .putExtra(NotificationService.EXTRA_BODY, "How was your day?")
                .putExtra(NotificationService.EXTRA_CHANNEL_ID, NotificationService.CHANNEL_EVENING)
        )
    }

    @Test
    fun eveningCheckup_postsRatingActions_andTapStoresTheRating() {
        sendCheckup()

        val notification = awaitCheckupNotification()
        assertNotNull("evening checkup notification never appeared", notification)
        val actions = notification!!.notification.actions.orEmpty()
        assertEquals(
            listOf(
                context.getString(R.string.dayRatingBad),
                context.getString(R.string.dayRatingOk),
                context.getString(R.string.dayRatingGreat),
            ),
            actions.map { it.title.toString() },
        )

        // Fire the GREAT action's PendingIntent like a tap on the notification button would.
        actions.last().actionIntent.send()

        val stored = awaitUntilNotNull { runBlocking { ratings.get(todayKey) } }
        assertNotNull("tapping the action stored no rating", stored)
        assertEquals(DayRating.GREAT, DayRating.fromValue(stored!!.rating))
        assertNull("notification should be dismissed after rating", awaitCheckupGone())
    }

    @Test
    fun eveningCheckup_whenTodayIsRated_hasNoRatingActions() {
        runBlocking {
            ratings.upsert(DayRatingEntity(todayKey, DayRating.OK.value, Instant.now()))
        }
        sendCheckup()

        val notification = awaitCheckupNotification()
        assertNotNull("evening checkup notification never appeared", notification)
        assertTrue(notification!!.notification.actions.isNullOrEmpty())
    }

    private fun awaitCheckupNotification(): StatusBarNotification? = awaitUntilNotNull {
        manager.activeNotifications.find { it.id == NotificationService.END_OF_DAY_CHECKUP_ID }
    }

    /** Polls until the checkup notification disappears; returns it if it is still up after 5s. */
    private fun awaitCheckupGone(): StatusBarNotification? {
        repeat(50) {
            val active = manager.activeNotifications
                .find { it.id == NotificationService.END_OF_DAY_CHECKUP_ID }
                ?: return null
            if (it == 49) return active
            Thread.sleep(100)
        }
        return null
    }

    private fun <T : Any> awaitUntilNotNull(block: () -> T?): T? {
        repeat(50) {
            block()?.let { return it }
            Thread.sleep(100)
        }
        return null
    }
}
