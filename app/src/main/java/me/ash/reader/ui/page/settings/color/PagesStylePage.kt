package me.ash.reader.ui.page.settings.color

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Feed
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.ash.reader.R
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.page.settings.SettingItem
import me.ash.reader.ui.theme.palette.onLight

@Composable
fun PagesStylePage(
    onBack: () -> Unit,
    navigateToFeedsPageStyle: () -> Unit,
    navigateToFlowPageStyle: () -> Unit,
    navigateToReadingPageStyle: () -> Unit,
) {
    RYScaffold(
        containerColor = MaterialTheme.colorScheme.surface onLight MaterialTheme.colorScheme.inverseOnSurface,
        navigationIcon = {
            FeedbackIconButton(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = onBack,
            )
        },
        content = {
            LazyColumn {
                item {
                    DisplayText(
                        text = stringResource(R.string.pages_layout_and_display),
                        desc = stringResource(R.string.pages_layout_and_display_desc),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    SettingItem(
                        title = stringResource(R.string.feeds_page),
                        desc = stringResource(R.string.feeds_page_style_desc),
                        icon = Icons.Outlined.Feed,
                        onClick = navigateToFeedsPageStyle,
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.flow_page),
                        desc = stringResource(R.string.flow_page_style_desc),
                        icon = Icons.Outlined.ViewStream,
                        onClick = navigateToFlowPageStyle,
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.reading_page),
                        desc = stringResource(R.string.reading_page_style_desc),
                        icon = Icons.Outlined.ChromeReaderMode,
                        onClick = navigateToReadingPageStyle,
                    ) {}
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        },
    )
}
