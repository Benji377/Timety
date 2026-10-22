package io.github.benji377.timety.ui.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Saves a [Color] as its ARGB int, for `rememberSaveable` form state that holds a color. */
val ColorSaver: Saver<Color, Int> = Saver(save = { it.toArgb() }, restore = { Color(it) })
