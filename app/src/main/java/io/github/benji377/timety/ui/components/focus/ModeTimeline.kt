package io.github.benji377.timety.ui.components.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Stub for the ModeTimeline component to allow FocusScreen to compile.
// Detailed implementation will be done when expanding the timeline logic.
@Composable
fun ModeTimeline(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Mode Timeline Stub")
    }
}
