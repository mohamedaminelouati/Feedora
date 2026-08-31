package com.mohamedaminelouati.feedora.ui.page.settings

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
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
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
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalNewVersionNumber
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalSkipVersionNumber
import com.mohamedaminelouati.feedora.infrastructure.preference.toDisplayName
import com.mohamedaminelouati.feedora.ui.component.base.Banner
import com.mohamedaminelouati.feedora.ui.component.base.DisplayText
import com.mohamedaminelouati.feedora.ui.component.base.FeedbackIconButton
import com.mohamedaminelouati.feedora.ui.component.base.RYScaffold
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.getCurrentVersion
import com.mohamedaminelouati.feedora.ui.page.settings.tips.UpdateDialog
import com.mohamedaminelouati.feedora.ui.page.settings.tips.UpdateViewModel
import com.mohamedaminelouati.feedora.ui.theme.palette.onLight

private data class SettingSearchItem(
    val title: String,
    val description: String,
    val category: String,
    val icon: ImageVector,
    val keywords: List<String>,
    val onClick: () -> Unit,
)

@Composable
fun SettingsPage(
    updateViewModel: UpdateViewModel = hiltViewModel(),
    onBack: () -> Unit,
    navigateToFeedsPageStyle: () -> Unit,
    navigateToFlowPageStyle: () -> Unit,
    navigateToReadingPageStyle: () -> Unit,
    navigateToColorAndStyle: () -> Unit,
    navigateToAccounts: () -> Unit,
    navigateToCloudBackup: () -> Unit,
    navigateToLanguages: () -> Unit,
    navigateToTipsAndSupport: () -> Unit,
) {
    val context = LocalContext.current
    val newVersion = LocalNewVersionNumber.current
    val skipVersion = LocalSkipVersionNumber.current
    val currentVersion by remember { mutableStateOf(context.getCurrentVersion()) }

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val accountsTitle = stringResource(R.string.accounts)
    val accountsDesc = stringResource(R.string.accounts_desc)
    val globalStyleTitle = stringResource(R.string.global_style)
    val colorAndStyleDesc = stringResource(R.string.color_and_style_desc)
    val feedsPageStyleTitle = stringResource(R.string.feeds_page)
    val feedsPageStyleDesc = stringResource(R.string.feeds_page_style_desc)
    val flowPageStyleTitle = stringResource(R.string.flow_page)
    val flowPageStyleDesc = stringResource(R.string.flow_page_style_desc)
    val readingPageStyleTitle = stringResource(R.string.reading_page)
    val readingPageStyleDesc = stringResource(R.string.reading_page_style_desc)
    val backupsTitle = stringResource(R.string.backups_title)
    val backupAndDataDesc = stringResource(R.string.backup_and_data_desc)
    val languagesTitle = stringResource(R.string.languages)
    val languagesDesc = Locale.getDefault().toDisplayName()
    val helpAndSupportTitle = stringResource(R.string.help_and_support)
    val tipsAndSupportDesc = stringResource(R.string.tips_and_support_desc)
    val darkThemeTitle = stringResource(R.string.dark_theme)
    val boldCharactersTitle = stringResource(R.string.bold_characters)
    val boldCharactersDesc = stringResource(R.string.bold_characters_preview)
    val syncNotificationTitle = stringResource(R.string.sync_notification)
    val syncNotificationDesc = stringResource(R.string.sync_notification_desc)
    val restoreLastArticleTitle = stringResource(R.string.restore_last_article)
    val restoreLastArticleDesc = stringResource(R.string.restore_last_article_desc)
    val restoreScrollPositionTitle = stringResource(R.string.restore_scroll_position)
    val restoreScrollPositionDesc = stringResource(R.string.restore_scroll_position_desc)
    val syncStatusTitle = stringResource(R.string.show_sync_status)
    val syncStatusDesc = stringResource(R.string.show_sync_status_desc)
    val pullToSwitchTitle = stringResource(R.string.pull_to_switch_feed)
    val swipeToStartTitle = stringResource(R.string.swipe_to_start)
    val swipeToEndTitle = stringResource(R.string.swipe_to_end)
    val sortUnreadArticlesTitle = stringResource(R.string.sort_unread_articles)
    val markAsReadOnScrollTitle = stringResource(R.string.mark_as_read_on_scroll)
    val hideEmptyGroupsTitle = stringResource(R.string.hide_empty_groups)
    val initialPageTitle = stringResource(R.string.initial_page)
    val initialFilterTitle = stringResource(R.string.initial_filter)
    val openLinkBrowserTitle = stringResource(R.string.open_link_specific_browser)
    val sharedContentTitle = stringResource(R.string.shared_content)
    val pullToSwitchArticleTitle = stringResource(R.string.pull_to_switch_article)

    val allSearchableItems = remember(
        accountsTitle,
        accountsDesc,
        globalStyleTitle,
        colorAndStyleDesc,
        feedsPageStyleTitle,
        feedsPageStyleDesc,
        flowPageStyleTitle,
        flowPageStyleDesc,
        readingPageStyleTitle,
        readingPageStyleDesc,
        backupsTitle,
        backupAndDataDesc,
        languagesTitle,
        languagesDesc,
        helpAndSupportTitle,
        tipsAndSupportDesc,
        darkThemeTitle,
        boldCharactersTitle,
        syncNotificationTitle,
        restoreLastArticleTitle,
        restoreScrollPositionTitle,
        syncStatusTitle,
        pullToSwitchTitle,
        swipeToStartTitle,
        swipeToEndTitle,
        sortUnreadArticlesTitle,
        markAsReadOnScrollTitle,
        hideEmptyGroupsTitle,
        initialPageTitle,
        initialFilterTitle,
        openLinkBrowserTitle,
        sharedContentTitle,
        pullToSwitchArticleTitle,
    ) {
        listOf(
            SettingSearchItem(
                title = accountsTitle,
                description = accountsDesc,
                category = accountsTitle,
                icon = Icons.Outlined.AccountCircle,
                keywords = listOf("rss", "local", "miniflux", "freshrss", "nextcloud", "inoreader", "feedbin", "fever", "google reader", "comptes", "ajouter", "sync"),
                onClick = navigateToAccounts,
            ),
            SettingSearchItem(
                title = globalStyleTitle,
                description = colorAndStyleDesc,
                category = globalStyleTitle,
                icon = Icons.Outlined.Palette,
                keywords = listOf("theme", "colors", "dark", "light", "appearance", "font", "style", "ui", "couleur", "apparence", "police", "global"),
                onClick = navigateToColorAndStyle,
            ),
            SettingSearchItem(
                title = feedsPageStyleTitle,
                description = feedsPageStyleDesc,
                category = feedsPageStyleTitle,
                icon = Icons.Outlined.Folder,
                keywords = listOf("feeds", "feed", "page", "groups", "expand", "elevation", "favicons", "flux", "dossiers", "layout", "grid"),
                onClick = navigateToFeedsPageStyle,
            ),
            SettingSearchItem(
                title = flowPageStyleTitle,
                description = flowPageStyleDesc,
                category = flowPageStyleTitle,
                icon = Icons.Outlined.Dashboard,
                keywords = listOf("flow", "stream", "articles", "images", "date", "title", "header", "sticky", "grid", "grille", "swipes", "gestes", "filtre"),
                onClick = navigateToFlowPageStyle,
            ),
            SettingSearchItem(
                title = readingPageStyleTitle,
                description = readingPageStyleDesc,
                category = readingPageStyleTitle,
                icon = Icons.Outlined.MenuBook,
                keywords = listOf("reading", "reader", "font", "size", "line height", "text", "alignment", "ai", "lecture", "liens", "partage", "bionic"),
                onClick = navigateToReadingPageStyle,
            ),
            SettingSearchItem(
                title = backupsTitle,
                description = backupAndDataDesc,
                category = backupsTitle,
                icon = Icons.Outlined.CloudUpload,
                keywords = listOf("backup", "cloud", "webdav", "ftp", "sftp", "ftps", "sauvegarde", "données", "cache", "json", "restaurer"),
                onClick = navigateToCloudBackup,
            ),
            SettingSearchItem(
                title = darkThemeTitle,
                description = globalStyleTitle,
                category = globalStyleTitle,
                icon = Icons.Outlined.DarkMode,
                keywords = listOf("dark", "night", "amoled", "black", "theme", "mode", "oled"),
                onClick = navigateToColorAndStyle,
            ),
            SettingSearchItem(
                title = boldCharactersTitle,
                description = boldCharactersDesc,
                category = readingPageStyleTitle,
                icon = Icons.Outlined.FormatBold,
                keywords = listOf("bold", "bionic", "reading", "characters", "text"),
                onClick = navigateToReadingPageStyle,
            ),
            SettingSearchItem(
                title = swipeToStartTitle,
                description = flowPageStyleTitle,
                category = flowPageStyleTitle,
                icon = Icons.Outlined.Swipe,
                keywords = listOf("swipe", "left", "gesture", "mark", "read", "star", "action"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.swipeStartAction)
                    navigateToFlowPageStyle()
                },
            ),
            SettingSearchItem(
                title = swipeToEndTitle,
                description = flowPageStyleTitle,
                category = flowPageStyleTitle,
                icon = Icons.Outlined.Swipe,
                keywords = listOf("swipe", "right", "gesture", "mark", "read", "star", "action"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.swipeEndAction)
                    navigateToFlowPageStyle()
                },
            ),
            SettingSearchItem(
                title = sortUnreadArticlesTitle,
                description = flowPageStyleTitle,
                category = flowPageStyleTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("sort", "order", "articles", "unread", "date", "recent", "earliest", "latest", "chrono"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.flowSortUnreadArticles)
                    navigateToFlowPageStyle()
                },
            ),
            SettingSearchItem(
                title = markAsReadOnScrollTitle,
                description = flowPageStyleTitle,
                category = flowPageStyleTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("mark", "read", "scroll", "automatic", "auto"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.markAsReadOnScroll)
                    navigateToFlowPageStyle()
                },
            ),
            SettingSearchItem(
                title = hideEmptyGroupsTitle,
                description = feedsPageStyleTitle,
                category = feedsPageStyleTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("hide", "empty", "groups", "folders"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.hideEmptyGroups)
                    navigateToFeedsPageStyle()
                },
            ),
            SettingSearchItem(
                title = initialFilterTitle,
                description = flowPageStyleTitle,
                category = flowPageStyleTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("initial", "filter", "default", "all", "unread", "starred"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.initialFilter)
                    navigateToFlowPageStyle()
                },
            ),
            SettingSearchItem(
                title = openLinkBrowserTitle,
                description = readingPageStyleTitle,
                category = readingPageStyleTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("browser", "links", "open", "chrome", "firefox", "custom tabs", "web"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.openLink)
                    navigateToReadingPageStyle()
                },
            ),
            SettingSearchItem(
                title = sharedContentTitle,
                description = readingPageStyleTitle,
                category = readingPageStyleTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("share", "shared", "content", "title", "link", "url"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.sharedContent)
                    navigateToReadingPageStyle()
                },
            ),
            SettingSearchItem(
                title = syncNotificationTitle,
                description = syncNotificationDesc,
                category = accountsTitle,
                icon = Icons.Outlined.Notifications,
                keywords = listOf("sync", "notification", "alerts", "new articles", "count"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.syncNotification)
                    navigateToAccounts()
                },
            ),
            SettingSearchItem(
                title = restoreLastArticleTitle,
                description = restoreLastArticleDesc,
                category = readingPageStyleTitle,
                icon = Icons.Outlined.History,
                keywords = listOf("resume", "last article", "startup", "reopen", "reading", "history"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.restoreLastArticle)
                    navigateToReadingPageStyle()
                },
            ),
            SettingSearchItem(
                title = restoreScrollPositionTitle,
                description = restoreScrollPositionDesc,
                category = flowPageStyleTitle,
                icon = Icons.Outlined.TouchApp,
                keywords = listOf("scroll", "position", "remember", "all", "unread", "list", "flow"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.restoreScrollPosition)
                    navigateToFlowPageStyle()
                },
            ),
            SettingSearchItem(
                title = syncStatusTitle,
                description = syncStatusDesc,
                category = feedsPageStyleTitle,
                icon = Icons.Outlined.Sync,
                keywords = listOf("sync status", "last sync", "date", "time", "feeds", "error", "status"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.feedsShowSyncStatus)
                    navigateToFeedsPageStyle()
                },
            ),
            SettingSearchItem(
                title = pullToSwitchTitle,
                description = flowPageStyleTitle,
                category = flowPageStyleTitle,
                icon = Icons.Outlined.Swipe,
                keywords = listOf("pull", "swipe", "next feed", "bottom", "load"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.pullToLoadNextFeed)
                    navigateToFlowPageStyle()
                },
            ),
            SettingSearchItem(
                title = pullToSwitchArticleTitle,
                description = readingPageStyleTitle,
                category = readingPageStyleTitle,
                icon = Icons.Outlined.Swipe,
                keywords = listOf("pull", "switch", "article", "next", "reading"),
                onClick = {
                    SettingsHighlightManager.highlight(DataStoreKey.pullToSwitchArticle)
                    navigateToReadingPageStyle()
                },
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
                title = helpAndSupportTitle,
                description = tipsAndSupportDesc,
                category = helpAndSupportTitle,
                icon = Icons.Outlined.TipsAndUpdates,
                keywords = listOf("help", "support", "about", "donate", "license", "version", "update", "github", "aide", "assistance"),
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
        content = {
            LazyColumn {
                item {
                    DisplayText(text = stringResource(R.string.settings), desc = "")
                }

                // Permanent Search Bar Field
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_settings),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
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
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (searchQuery.isNotBlank()) {
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
                    // 1- Compte
                    item {
                        SelectableSettingGroupItem(
                            title = accountsTitle,
                            desc = accountsDesc,
                            icon = Icons.Outlined.AccountCircle,
                            onClick = navigateToAccounts,
                        )
                    }
                    // 2- Style global
                    item {
                        SelectableSettingGroupItem(
                            title = globalStyleTitle,
                            desc = colorAndStyleDesc,
                            icon = Icons.Outlined.Palette,
                            onClick = navigateToColorAndStyle,
                        )
                    }
                    // 3- Page des flux
                    item {
                        SelectableSettingGroupItem(
                            title = feedsPageStyleTitle,
                            desc = feedsPageStyleDesc,
                            icon = Icons.Outlined.Folder,
                            onClick = navigateToFeedsPageStyle,
                        )
                    }
                    // 4- Page des articles
                    item {
                        SelectableSettingGroupItem(
                            title = flowPageStyleTitle,
                            desc = flowPageStyleDesc,
                            icon = Icons.Outlined.Dashboard,
                            onClick = navigateToFlowPageStyle,
                        )
                    }
                    // 5- Page de lecture
                    item {
                        SelectableSettingGroupItem(
                            title = readingPageStyleTitle,
                            desc = readingPageStyleDesc,
                            icon = Icons.Outlined.MenuBook,
                            onClick = navigateToReadingPageStyle,
                        )
                    }
                    // 6- Sauvegardes
                    item {
                        SelectableSettingGroupItem(
                            title = backupsTitle,
                            desc = backupAndDataDesc,
                            icon = Icons.Outlined.CloudUpload,
                            onClick = navigateToCloudBackup,
                        )
                    }
                    // 7- Langues
                    item {
                        SelectableSettingGroupItem(
                            title = languagesTitle,
                            desc = languagesDesc,
                            icon = Icons.Outlined.Language,
                            onClick = navigateToLanguages,
                        )
                    }
                    // 8- Aide et assistance
                    item {
                        SelectableSettingGroupItem(
                            title = helpAndSupportTitle,
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

    UpdateDialog(updateViewModel = updateViewModel)
}
