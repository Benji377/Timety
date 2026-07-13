package io.github.benji377.timety.services

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


/**
 * Runs [block] on the IO dispatcher while goAsync keeps the broadcast alive, finishing the
 * pending result when the block completes or throws.
 */
internal fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pendingResult.finish()
        }
    }
}
