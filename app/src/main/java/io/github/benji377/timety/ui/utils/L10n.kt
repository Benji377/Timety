package io.github.benji377.timety.ui.utils

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Resolves an ICU-style plural that may define an explicit `=0` case.
 *
 * Android's `getQuantityString` does not select the `zero` quantity for
 * languages like English/German, so the ARB→resource converter emits the `=0`
 * text as a companion `"<key>Zero"` string. Pass that string id as [zeroRes]
 * (or 0 when the plural has no `=0` case).
 */
@Composable
fun quantityString(
    @PluralsRes pluralRes: Int,
    count: Int,
    @StringRes zeroRes: Int = 0,
    vararg formatArgs: Any,
): String {
    if (count == 0 && zeroRes != 0) {
        return if (formatArgs.isEmpty()) stringResource(zeroRes)
        else stringResource(zeroRes, *formatArgs)
    }
    return pluralStringResource(pluralRes, count, *formatArgs)
}
