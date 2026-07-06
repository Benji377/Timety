package io.github.benji377.timety

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.ThemeMode
import io.github.benji377.timety.data.repository.dataStore
import io.github.benji377.timety.ui.theme.TimetyTheme
import io.github.benji377.timety.ui.utils.DateFormatSettings
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.material3.SnackbarHostState
import io.github.benji377.timety.ui.screens.MainScreen
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState

class MainActivity : ComponentActivity() {

    // Locale code applied when this Activity's base context was attached. Used to
    // detect a language change and recreate the Activity so resources reload.
    private var appliedLocaleCode: String = "system"

    override fun attachBaseContext(newBase: Context) {
        val settings = SettingsRepository(newBase.dataStore)
        appliedLocaleCode = runBlocking { settings.appLocaleCodeFlow.first() }
        super.attachBaseContext(LocaleHelper.wrap(newBase, appliedLocaleCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // targetSdk 35+ enforces edge-to-edge on Android 15; opting in explicitly keeps the
        // rendering identical on older OS versions.
        enableEdgeToEdge()

        val settings = SettingsRepository(dataStore)

        // Recreate the Activity when the selected language changes.
        lifecycleScope.launch {
            settings.appLocaleCodeFlow.collect { code ->
                if (code != appliedLocaleCode) recreate()
            }
        }

        setContent {
            val themePref by settings.themePrefFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themePref) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val use24HourFormat by settings.use24HourFormatFlow.collectAsState(initial = true)
            val dateFormatCode by settings.dateFormatFlow.collectAsState(initial = "System")

            TimetyTheme(darkTheme = darkTheme) {
                val snackbarHostState =
                    androidx.compose.runtime.remember { SnackbarHostState() }
                CompositionLocalProvider(
                    LocalDateFormatSettings provides DateFormatSettings(
                        use24HourFormat,
                        dateFormatCode
                    ),
                    LocalSnackbarHostState provides snackbarHostState
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen()
                    }
                }
            }
        }
    }
}
