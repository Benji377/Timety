package io.github.benji377.timety.util

import android.content.Context
import android.os.Build
import java.util.Locale

/** Applies a per-app locale by wrapping a Context with an overridden Configuration. */
object LocaleHelper {

    /**
     * Wraps [context] so its resources resolve to [code]. Pass "system" (or blank)
     * to follow the OS locale. Codes: "en", "de", "it", "lld".
     */
    fun wrap(context: Context, code: String): Context {
        if (code.isBlank() || code == "system") return context

        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)

        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}
