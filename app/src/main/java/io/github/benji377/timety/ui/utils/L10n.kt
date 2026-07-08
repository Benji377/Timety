package io.github.benji377.timety.ui.utils

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource


/**
 * Resolves a plural string resource for [count], substituting [zeroRes] instead when [count] is
 * zero and a zero-specific string was provided.
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
