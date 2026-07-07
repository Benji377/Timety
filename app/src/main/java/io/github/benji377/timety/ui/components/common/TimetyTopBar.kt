package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * Standard top bar: app background color (no surface tint) and a bold title,
 * so every screen carries the same header treatment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetyTopBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TimetyTopBar(
        titleContent = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = navigationIcon,
        actions = actions,
    )
}

/** Variant for screens whose title area is richer than a single line of text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetyTopBar(
    titleContent: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = titleContent,
        navigationIcon = navigationIcon,
        actions = actions,
    )
}
