package io.github.benji377.timety.services

import android.content.Intent
import android.os.Looper
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

/**
 * Lifecycle of [FocusTimerService]: it must not stop itself on the idle state it sees before the
 * first start command, which on slow phones crashed the app (#272).
 */
@RunWith(RobolectricTestRunner::class)
class FocusTimerServiceTest {
    private lateinit var controller: ServiceController<FocusTimerService>

    @Before
    fun setUp() {
        FocusTimerManager.stopTimer(discard = true)
        controller = Robolectric.buildService(FocusTimerService::class.java)
    }

    @After
    fun tearDown() {
        FocusTimerManager.stopTimer(discard = true)
        controller.destroy()
    }

    @Test
    fun idleStateBeforeTheFirstStart_doesNotStopTheService() {
        // The crash: the state collector runs between onCreate and onStartCommand and sees "idle".
        controller.create()
        idleMainLooperFor(SETTLE_MS)

        assertFalse(isStoppedBySelf())
    }

    @Test
    fun startAfterTheCollectorSawIdle_runsInTheForeground() {
        controller.create()
        idleMainLooperFor(SETTLE_MS)

        startCommand(Intent().setAction(FocusTimerService.ACTION_START), 1)
        idleMainLooperFor(SETTLE_MS)

        assertFalse(isStoppedBySelf())
        assertNotNull(shadowOf(controller.get()).lastForegroundNotification)
        assertTrue(FocusTimerManager.timerState.value.isSessionActive)
    }

    @Test
    fun sessionEnding_stopsTheService() {
        controller.create()
        startCommand(Intent().setAction(FocusTimerService.ACTION_START), 1)
        idleMainLooperFor(SETTLE_MS)

        startCommand(Intent().setAction(FocusTimerService.ACTION_DISCARD), 2)
        idleMainLooperUntil { isStoppedBySelf() }

        assertTrue(isStoppedBySelf())
    }

    @Test
    fun stickyRestartWithoutASession_stopsTheService() {
        // After the process is killed, START_STICKY restarts the service with a null intent and an
        // idle timer: there is nothing to show, so it must not linger.
        controller.create()
        startCommand(null, 1)
        idleMainLooperUntil { isStoppedBySelf() }

        assertTrue(isStoppedBySelf())
    }

    private fun startCommand(intent: Intent?, startId: Int) {
        controller.get().onStartCommand(intent, 0, startId)
    }

    private fun isStoppedBySelf(): Boolean = shadowOf(controller.get()).isStoppedBySelf

    // The collector's Do Not Disturb sync reads DataStore off the main thread, so give it real time
    // to finish and keep running what it posts back to the main looper.
    private fun idleMainLooperFor(ms: Long) {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    private fun idleMainLooperUntil(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + TIMEOUT_MS
        while (!condition() && System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    private companion object {
        const val SETTLE_MS = 500L
        const val TIMEOUT_MS = 3_000L
    }
}
