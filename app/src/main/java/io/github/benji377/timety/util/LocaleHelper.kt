package io.github.benji377.timety.util

import android.content.Context
import java.util.Locale


/** Applies a user-selected locale override to a [Context] for per-app language switching. */
object LocaleHelper {


    /**
     * Returns a new [Context] configured with [code] as its locale. `"system"` (or a blank code)
     * is treated as "use the device locale", so the original [context] is returned unchanged.
     */
    fun wrap(context: Context, code: String): Context {
        if (code.isBlank() || code == "system") return context

        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)

        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }
}
