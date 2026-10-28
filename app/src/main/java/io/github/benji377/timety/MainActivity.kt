package io.github.benji377.timety

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import io.github.benji377.timety.data.repository.SettingsRepository
import io.github.benji377.timety.data.repository.ThemeMode
import io.github.benji377.timety.data.repository.dataStore
import io.github.benji377.timety.ui.navigation.AppRoute
import io.github.benji377.timety.ui.navigation.BottomNavRoutes
import io.github.benji377.timety.ui.screens.MainScreen
import io.github.benji377.timety.ui.theme.LocalSnackbarHostState
import io.github.benji377.timety.ui.theme.TimetyTheme
import io.github.benji377.timety.ui.utils.DateFormatSettings
import io.github.benji377.timety.ui.utils.LocalDateFormatSettings
import io.github.benji377.timety.util.LocaleHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Single-activity entry point that sets up localization, theme, and the Compose UI tree. */
class MainActivity : ComponentActivity() {
    // Locale code applied when this Activity's base context was attached. Used to
    // detect a language change and recreate the Activity so resources reload.
    private var appliedLocaleCode: String = "system"

    // One-shot navigation request carried by the launching intent (app shortcut or widget tap):
    // a nav route string that MainScreen consumes once and then clears.
    private val navTarget = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navTarget.value = intent.navTargetExtra()
    }

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

        // Android 12+ sends a root activity to the background on Back instead of finishing it.
        // Do the same on older versions: finishing drops the activity-scoped focus view model,
        // and with it the bookkeeping of a session still running in the timer service. Navigation's
        // own Back handling registers later, so it still takes precedence while it can pop.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }
        }

        val settings = SettingsRepository(dataStore)
        // Only a fresh launch carries a request; a rebuild (rotation, language change) would replay it.
        if (savedInstanceState == null) navTarget.value = intent?.navTargetExtra()

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
                        val pendingNavTarget by navTarget.collectAsState()
                        MainScreen(
                            navTarget = pendingNavTarget,
                            onNavTargetConsumed = { navTarget.value = null }
                        )
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Intent extra naming a navigation route (an [AppRoute][io.github.benji377.timety.ui.navigation.AppRoute]
         * or bottom-tab route string) to open once the UI is up. Set by the static app shortcuts
         * in `res/xml/shortcuts.xml` and by widget tap intents.
         */
        const val EXTRA_NAV_TARGET = "navTarget"

        // Shortcuts and widgets only open a tab or a single item; other routes from outside are ignored.
        private val EXTERNAL_DETAIL_ROUTES = setOf(
            AppRoute.TASK_DETAIL.route,
            AppRoute.RECURRING_TASK_DETAIL.route,
            AppRoute.HABIT_DETAIL.route,
        )

        private fun Intent.navTargetExtra(): String? =
            getStringExtra(EXTRA_NAV_TARGET)?.takeIf { route ->
                route in BottomNavRoutes || route.substringBefore('/') in EXTERNAL_DETAIL_ROUTES
            }
    }
}
