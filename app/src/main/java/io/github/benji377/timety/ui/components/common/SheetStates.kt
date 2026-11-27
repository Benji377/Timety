package io.github.benji377.timety.ui.components.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable

/** State for a modal bottom sheet that opens straight to full height, with no half-open stop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberFullHeightSheetState(): SheetState = rememberBottomSheetState(
    initialValue = SheetValue.Hidden,
    enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
)
