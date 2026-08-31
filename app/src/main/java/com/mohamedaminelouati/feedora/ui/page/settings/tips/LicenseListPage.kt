package com.mohamedaminelouati.feedora.ui.page.settings.tips

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.infrastructure.preference.OpenLinkPreference
import com.mohamedaminelouati.feedora.ui.component.base.FeedbackIconButton
import com.mohamedaminelouati.feedora.ui.component.base.RYScaffold
import com.mohamedaminelouati.feedora.ui.ext.openURL
import com.mohamedaminelouati.feedora.ui.theme.palette.onLight

@Composable
fun LicenseListPage(
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    RYScaffold(
        containerColor = MaterialTheme.colorScheme.surface onLight MaterialTheme.colorScheme.inverseOnSurface,
        navigationIcon = {
            FeedbackIconButton(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onBack
            )
        },
        actions = {
            FeedbackIconButton(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.Star,
                contentDescription = stringResource(R.string.open_source_licenses),
                tint = MaterialTheme.colorScheme.onSurface
            ) {
                context.openURL(
                    url = context.getString(R.string.github_link) + "/blob/main/LICENSE",
                    openLink = OpenLinkPreference.AutoPreferCustomTabs,
                )
            }
        },
        content = {
            Column {
                LibrariesContainer(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = WindowInsets(0, 0, 0, 24)
                        .add(WindowInsets.navigationBars)
                        .asPaddingValues(),
                )
            }
        },
    )
}
