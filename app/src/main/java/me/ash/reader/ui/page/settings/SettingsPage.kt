package me.ash.reader.ui.page.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.util.Locale
import me.ash.reader.R
import me.ash.reader.infrastructure.preference.LocalNewVersionNumber
import me.ash.reader.infrastructure.preference.LocalSkipVersionNumber
import me.ash.reader.infrastructure.preference.toDisplayName
import me.ash.reader.ui.component.base.Banner
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.ext.getCurrentVersion
import me.ash.reader.ui.page.settings.tips.UpdateDialog
import me.ash.reader.ui.page.settings.tips.UpdateViewModel
import me.ash.reader.ui.theme.palette.onLight

private data class SettingSearchItem(
    val title: String,
    val description: String,
    val category: String,
    val icon: ImageVector,
    val keywords: List<String> = emptyList(),
    val onClick: () -> Unit,
)

@Composable
fun SettingsPage(
    updateViewModel: UpdateViewModel = hiltViewModel(),
    onBack: () -> Unit,
    navigateToAccounts: () -> Unit,
    navigateToColorAndStyle: () -> Unit,
    navigateToInteraction: () -> Unit,
    navigateToLanguages: () -> Unit,
    navigateToTroubleshooting: () -> Unit,
    navigateToTipsAndSupport: () -> Unit,
) {
    val context = LocalContext.current
    val newVersion = LocalNewVersionNumber.current
    val skipVersion = LocalSkipVersionNumber.current
    val currentVersion by remember { mutableStateOf(context.getCurrentVersion()) }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val accountsTitle = stringResource(R.string.accounts)
    val accountsDesc = stringResource(R.string.accounts_desc)
    val colorAndStyleTitle = stringResource(R.string.color_and_style)
    val colorAndStyleDesc = stringResource(R.string.color_and_style_desc)
    val darkThemeTitle = stringResource(R.string.dark_theme)
    val boldCharactersTitle = stringResource(R.string.bold_characters)
    val boldCharactersDesc = stringResource(R.string.bold_characters_preview)
    val interactionTitle = stringResource(R.string.interaction)
    val interactionDesc = stringResource(R.string.interaction_desc)
    val syncNotificationTitle = stringResource(R.string.sync_notification)
    val syncNotificationDesc = stringResource(R.string.sync_notification_desc)
    val restoreLastArticleTitle = stringResource(R.string.restore_last_article)
    val restoreLastArticleDesc = stringResource(R.string.restore_last_article_desc)
    val restoreScrollPositionTitle = stringResource(R.string.restore_scroll_position)
    val restoreScrollPositionDesc = stringResource(R.string.restore_scroll_position_desc)
    val syncStatusTitle = stringResource(R.string.show_sync_status)
    val syncStatusDesc = stringResource(R.string.show_sync_status_desc)
    val pullToSwitchTitle = stringResource(R.string.pull_to_switch_feed)
    val languagesTitle = stringResource(R.string.languages)
    val languagesDesc = Locale.getDefault().toDisplayName()
    val troubleshootingTitle = stringResource(R.string.troubleshooting)
    val troubleshootingDesc = stringResource(R.string.troubleshooting_desc)
    val tipsAndSupportTitle = stringResource(R.string.tips_and_support)
    val tipsAndSupportDesc = stringResource(R.string.tips_and_support_desc)

    val allSearchableItems = remember(
        accountsTitle,
        colorAndStyleTitle,
        darkThemeTitle,
        boldCharactersTitle,
        interactionTitle,
        syncNotificationTitle,
        restoreLastArticleTitle,
        restoreScrollPositionTitle,
        syncStatusTitle,
        pullToSwitchTitle,
        languagesTitle,
        languagesDesc,
        troubleshootingTitle,
        tipsAndSupportTitle,
    ) {
        listOf(
            SettingSearchItem(
                title = accountsTitle,
                description = accountsDesc,
                category = accountsTitle,
                icon = Icons.Outlined.AccountCircle,
                keywords = listOf("rss", "local", "miniflux", "freshrss", "nextcloud", "inoreader", "feedbin", "fever", "google reader"),
                onClick = navigateToAccounts,
            ),
            SettingSearchItem(
                title = colorAndStyleTitle,
                description = colorAndStyleDesc,
                category = colorAndStyleTitle,
                icon = Icons.Outlined.Palette,
                keywords = listOf("theme", "colors", "dark", "light", "appearance", "font", "style", "ui"),
                onClick = navigateToColorAndStyle,
            ),
            SettingSearchItem(
                title = darkThemeTitle,
                description = colorAndStyleTitle,
                category = colorAndStyleTitle,
                icon = Icons.Outlined.DarkMode,
                keywords = listOf("dark", "night", "amoled", "black", "theme"),
                onClick = navigateToColorAndStyle,
            ),
            SettingSearchItem(
                title = boldCharactersTitle,
                description = boldCharactersDesc,
                category = colorAndStyleTitle,
                icon = Icons.Outlined.FormatBold,
                keywords = listOf("bold", "bionic", "reading", "characters"),
                onClick = navigateToColorAndStyle,
            ),
            SettingSearchItem(
                title = interactionTitle,
                description = interactionDesc,
                category = interactionTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("gestures", "navigation", "startup", "behavior", "scroll"),
                onClick = navigateToInteraction,
            ),
            SettingSearchItem(
                title = syncNotificationTitle,
                description = syncNotificationDesc,
                category = interactionTitle,
                icon = Icons.Outlined.Notifications,
                keywords = listOf("sync", "notification", "alerts", "new articles", "count"),
                onClick = navigateToInteraction,
            ),
            SettingSearchItem(
                title = restoreLastArticleTitle,
                description = restoreLastArticleDesc,
                category = interactionTitle,
                icon = Icons.Outlined.History,
                keywords = listOf("resume", "last article", "startup", "reopen", "reading"),
                onClick = navigateToInteraction,
            ),
            SettingSearchItem(
                title = restoreScrollPositionTitle,
                description = restoreScrollPositionDesc,
                category = interactionTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("scroll", "position", "remember", "all", "unread", "list"),
                onClick = navigateToInteraction,
            ),
            SettingSearchItem(
                title = syncStatusTitle,
                description = syncStatusDesc,
                category = interactionTitle,
                icon = Icons.Outlined.Sync,
                keywords = listOf("sync status", "last sync", "date", "time", "feeds"),
                onClick = navigateToInteraction,
            ),
            SettingSearchItem(
                title = pullToSwitchTitle,
                description = interactionTitle,
                category = interactionTitle,
                icon = Icons.Outlined.Swipe,
                keywords = listOf("pull", "swipe", "next feed", "bottom", "load"),
                onClick = navigateToInteraction,
            ),
            SettingSearchItem(
                title = languagesTitle,
                description = languagesDesc,
                category = languagesTitle,
                icon = Icons.Outlined.Language,
                keywords = listOf("language", "locale", "translation", "english", "french", "arabic", "spanish", "german", "chinese"),
                onClick = navigateToLanguages,
            ),
            SettingSearchItem(
                title = troubleshootingTitle,
                description = troubleshootingDesc,
                category = troubleshootingTitle,
                icon = Icons.Outlined.BugReport,
                keywords = listOf("logs", "cache", "clean", "error", "debug", "backup", "restore", "database"),
                onClick = navigateToTroubleshooting,
            ),
            SettingSearchItem(
                title = tipsAndSupportTitle,
                description = tipsAndSupportDesc,
                category = tipsAndSupportTitle,
                icon = Icons.Outlined.TipsAndUpdates,
                keywords = listOf("help", "support", "about", "donate", "license", "version", "update", "github"),
                onClick = navigateToTipsAndSupport,
            ),
        )
    }

    val filteredItems = remember(searchQuery, allSearchableItems) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            allSearchableItems.filter { item ->
                item.title.lowercase(Locale.ROOT).contains(query) ||
                    item.description.lowercase(Locale.ROOT).contains(query) ||
                    item.category.lowercase(Locale.ROOT).contains(query) ||
                    item.keywords.any { it.lowercase(Locale.ROOT).contains(query) }
            }
        }
    }

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
        actions = {
            FeedbackIconButton(
                imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (isSearchActive) {
                    stringResource(R.string.close)
                } else {
                    stringResource(R.string.search_settings)
                },
                tint = MaterialTheme.colorScheme.onSurface,
                onClick = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) {
                        searchQuery = ""
                    }
                },
            )
        },
        content = {
            LazyColumn {
                item {
                    DisplayText(text = stringResource(R.string.settings), desc = "")
                }

                // Search Bar Field
                item {
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { Text(stringResource(R.string.search_settings)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.close),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                        )
                    }
                }

                if (isSearchActive && searchQuery.isNotBlank()) {
                    if (filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.no_settings_found),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(filteredItems) { item ->
                            SelectableSettingGroupItem(
                                title = item.title,
                                desc = "${item.category} • ${item.description}",
                                icon = item.icon,
                                onClick = item.onClick,
                            )
                        }
                    }
                } else {
                    // Default Settings Menu
                    item {
                        Box {
                            if (newVersion.whetherNeedUpdate(currentVersion, skipVersion)) {
                                Banner(
                                    modifier = Modifier.zIndex(1f),
                                    title = stringResource(R.string.get_new_updates),
                                    desc = stringResource(
                                        R.string.get_new_updates_desc,
                                        newVersion.toString(),
                                    ),
                                    icon = Icons.Outlined.Lightbulb,
                                    action = {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.close),
                                        )
                                    },
                                ) {
                                    updateViewModel.showDialog()
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                    item {
                        SelectableSettingGroupItem(
                            title = accountsTitle,
                            desc = accountsDesc,
                            icon = Icons.Outlined.AccountCircle,
                            onClick = navigateToAccounts,
                        )
                    }
                    item {
                        SelectableSettingGroupItem(
                            title = colorAndStyleTitle,
                            desc = colorAndStyleDesc,
                            icon = Icons.Outlined.Palette,
                            onClick = navigateToColorAndStyle,
                        )
                    }
                    item {
                        SelectableSettingGroupItem(
                            title = interactionTitle,
                            desc = interactionDesc,
                            icon = Icons.Outlined.TouchApp,
                            onClick = navigateToInteraction,
                        )
                    }
                    item {
                        SelectableSettingGroupItem(
                            title = languagesTitle,
                            desc = Locale.getDefault().toDisplayName(),
                            icon = Icons.Outlined.Language,
                            onClick = navigateToLanguages,
                        )
                    }
                    item {
                        SelectableSettingGroupItem(
                            title = troubleshootingTitle,
                            desc = troubleshootingDesc,
                            icon = Icons.Outlined.BugReport,
                            onClick = navigateToTroubleshooting,
                        )
                    }
                    item {
                        SelectableSettingGroupItem(
                            title = tipsAndSupportTitle,
                            desc = tipsAndSupportDesc,
                            icon = Icons.Outlined.TipsAndUpdates,
                            onClick = navigateToTipsAndSupport,
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        },
    )

    UpdateDialog()
}
