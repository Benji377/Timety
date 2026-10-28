package io.github.benji377.timety.util.reminder

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/** Android 12+'s "Alarms & reminders" access, which on-time reminders need. */
object ExactAlarmAccess {
    /** Whether the access is missing; always false before Android 12, which grants it implicitly. */
    fun isMissing(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == false

    /** Opens this app's "Alarms & reminders" system page. */
    @RequiresApi(Build.VERSION_CODES.S)
    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
}
