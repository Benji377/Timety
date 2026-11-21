package io.github.benji377.timety.ui.components.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme

/** Small pin after a list tile's title, marking a pinned task or habit. */
@Composable
fun PinnedMarker() {
    Icon(
        imageVector = Icons.Filled.PushPin,
        contentDescription = stringResource(R.string.commonLabelPinned),
        modifier = Modifier
            .padding(start = AppTheme.spaceXSmall)
            .size(AppTheme.iconSizeSmall),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
