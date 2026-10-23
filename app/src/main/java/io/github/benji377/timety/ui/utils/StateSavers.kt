package io.github.benji377.timety.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Saves a [Color] as its ARGB int, for `rememberSaveable` form state that holds a color. */
val ColorSaver: Saver<Color, Int> = Saver(save = { it.toArgb() }, restore = { Color(it) })

/**
 * What an editor keys its form state on: [latest] while viewing, and the value from when editing
 * began while editing. Stored updates, or the item loading again after a recreation, then never
 * overwrite the user's input.
 */
@Composable
fun <T> rememberFormSource(latest: T, isEditing: Boolean): T {
    val atEditStart = remember(isEditing) { latest }
    return if (isEditing) atEditStart else latest
}
