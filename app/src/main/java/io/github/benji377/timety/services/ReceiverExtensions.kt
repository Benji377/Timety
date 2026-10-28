package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Runs [block] on the IO dispatcher while goAsync keeps the broadcast alive, finishing the
 * pending result when the block completes or throws. A failure is logged rather than rethrown:
 * uncaught, it would crash the process from a background broadcast.
 */
internal fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Broadcast work failed", e)
        } finally {
            pendingResult.finish()
        }
    }
}
