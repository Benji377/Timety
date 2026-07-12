package io.github.benji377.timety.services

import android.app.NotificationManager
import android.content.Context
import io.github.benji377.timety.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Applies and restores Do Not Disturb around focus sessions, per Settings → Focus & Productivity
 * → Auto-DND. Gated by the *Notification policy access* special permission, which the user grants
 * via system settings rather than a runtime prompt; every mutating call degrades silently if that
 * access is missing or gets revoked mid-session.
 *
 * The pre-session filter is persisted in [SettingsRepository] rather than held in memory: if the
 * process is killed while DND is applied, [restoreIfOwned] run at the next app start still finds
 * it and restores the user's original filter instead of leaving DND stuck on.
 */
class FocusDndController(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private fun hasPolicyAccess(): Boolean =
        notificationManager?.isNotificationPolicyAccessGranted == true

    /**
     * Applies or lifts DND for the current phase of an active session. [sessionActive] mirrors the
     * running/paused/awaiting-continue union already used elsewhere for "is a session in progress";
     * [isRestPhase] is only consulted while active, to honor the "lift during breaks" toggle.
     */
    suspend fun syncForSessionState(sessionActive: Boolean, isRestPhase: Boolean) {
        if (!settingsRepository.autoDndEnabledFlow.first()) {
            restoreIfOwned()
            return
        }
        val liftDuringBreaks = settingsRepository.autoDndLiftDuringBreaksFlow.first()
        val shouldSuppress = sessionActive && !(isRestPhase && liftDuringBreaks)
        if (shouldSuppress) apply() else restoreIfOwned()
    }

    private suspend fun apply() {
        val manager = notificationManager ?: return
        if (!hasPolicyAccess()) return
        // Only snapshot the filter the first time - re-applying on every unrelated state change
        // (e.g. a tick) would otherwise overwrite the stored value with PRIORITY itself.
        if (settingsRepository.storedInterruptionFilterFlow.first() == null) {
            settingsRepository.saveStoredInterruptionFilter(manager.currentInterruptionFilter)
        }
        try {
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        } catch (_: SecurityException) {
            // Policy access was revoked between the check above and this call; leave the stored
            // filter in place so a later syncForSessionState/restoreIfOwned can still recover it.
        }
    }

    /** Restores the filter DND owned before it was applied, if any. Safe to call unconditionally. */
    suspend fun restoreIfOwned() {
        val stored = settingsRepository.storedInterruptionFilterFlow.first() ?: return
        if (hasPolicyAccess()) {
            try {
                notificationManager?.setInterruptionFilter(stored)
            } catch (_: SecurityException) {
                // Degrade silently; the stored value is cleared regardless so a stale filter
                // number is never reapplied later.
            }
        }
        settingsRepository.clearStoredInterruptionFilter()
    }
}
