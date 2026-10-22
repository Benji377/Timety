package io.github.benji377.timety.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import io.github.benji377.timety.R
import io.github.benji377.timety.ui.theme.AppTheme
import io.github.benji377.timety.ui.theme.FocusColor
import io.github.benji377.timety.ui.theme.HabitColor
import io.github.benji377.timety.ui.theme.WarningAccent


/** About page: community and feedback links, then the app card with version and project links. */
@Composable
internal fun AboutSettings() {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "1.0.0"
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    SettingsHeader(stringResource(R.string.settingsSectionSupport))
    NavSettingsItem(
        title = stringResource(R.string.settingsLabelCommunity),
        subtitle = stringResource(R.string.settingsLabelCommunitySubtitle),
        icon = Icons.Outlined.Forum,
        iconTint = FocusColor,
        trailing = SettingsTrailing.EXTERNAL,
        onClick = { openUrl("https://github.com/Benji377/Timety/discussions") }
    )
    NavSettingsItem(
        title = stringResource(R.string.settingsLabelFeedback),
        subtitle = stringResource(R.string.settingsLabelFeedbackSubtitle),
        icon = Icons.Outlined.BugReport,
        iconTint = HabitColor,
        trailing = SettingsTrailing.EXTERNAL,
        onClick = { openUrl("https://tally.so/r/ODbEoA") }
    )

    SettingsGroupDivider()

    // About card: app icon, version, and support links.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = AppTheme.flatCardElevation,
        shape = AppTheme.brNeo,
        border = BorderStroke(AppTheme.borderCard, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = R.mipmap.ic_launcher,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.appTitle),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                stringResource(R.string.settingsLabelVersion, versionName ?: "1.0.0"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settingsLabelBuiltBy)) },
                supportingContent = { Text(stringResource(R.string.settingsLabelMaintainer)) },
                leadingContent = { Icon(Icons.Filled.Person, null, tint = WarningAccent) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            NavSettingsItem(
                title = stringResource(R.string.settingsLabelDonate),
                subtitle = stringResource(R.string.settingsLabelDonateSubtitle),
                icon = Icons.Filled.Favorite,
                iconTint = Color.Red,
                trailing = SettingsTrailing.EXTERNAL,
                onClick = { openUrl("https://github.com/sponsors/Benji377") }
            )
            NavSettingsItem(
                title = stringResource(R.string.settingsLabelSourceCode),
                subtitle = stringResource(R.string.settingsLabelSourceCodeSubtitle),
                icon = Icons.Filled.Code,
                iconTint = Color.Blue,
                trailing = SettingsTrailing.EXTERNAL,
                onClick = { openUrl("https://github.com/Benji377/Timety") }
            )
        }
    }
}
