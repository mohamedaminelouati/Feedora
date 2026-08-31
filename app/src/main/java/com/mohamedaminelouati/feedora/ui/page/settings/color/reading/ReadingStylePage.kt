package com.mohamedaminelouati.feedora.ui.page.settings.color.reading

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Segment
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalOpenLink
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalOpenLinkSpecificBrowser
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalPullToSwitchArticle
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingAiSummary
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingAutoHideToolbar
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingBoldCharacters
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingFonts
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingPageTonalElevation
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingRenderer
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingTheme
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalRestoreLastArticle
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalSharedContent
import com.mohamedaminelouati.feedora.infrastructure.preference.OpenLinkPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.ReadingFontsPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.ReadingPageTonalElevationPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.ReadingRendererPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.ReadingThemePreference
import com.mohamedaminelouati.feedora.infrastructure.preference.SharedContentPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.not
import com.mohamedaminelouati.feedora.ui.component.ReadingThemePrev
import com.mohamedaminelouati.feedora.ui.component.base.DisplayText
import com.mohamedaminelouati.feedora.ui.component.base.FeedbackIconButton
import com.mohamedaminelouati.feedora.ui.component.base.RYScaffold
import com.mohamedaminelouati.feedora.ui.component.base.RYSwitch
import com.mohamedaminelouati.feedora.ui.component.base.RadioDialog
import com.mohamedaminelouati.feedora.ui.component.base.RadioDialogOption
import com.mohamedaminelouati.feedora.ui.component.base.Subtitle
import com.mohamedaminelouati.feedora.ui.ext.ExternalFonts
import com.mohamedaminelouati.feedora.ui.ext.MimeType
import com.mohamedaminelouati.feedora.ui.ext.getBrowserAppList
import com.mohamedaminelouati.feedora.ui.ext.showToast
import com.mohamedaminelouati.feedora.ui.page.settings.SettingItem
import com.mohamedaminelouati.feedora.ui.theme.palette.onLight

@Composable
fun ReadingStylePage(
    onBack: () -> Unit,
    navigateToReadingBoldCharacters: () -> Unit,
    navigateToReadingPageTitle: () -> Unit,
    navigateToReadingPageText: () -> Unit,
    navigateToReadingPageImage: () -> Unit,
    navigateToReadingPageVideo: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val readingTheme = LocalReadingTheme.current
    val tonalElevation = LocalReadingPageTonalElevation.current
    val fonts = LocalReadingFonts.current
    val autoHideToolbar = LocalReadingAutoHideToolbar.current
    val pullToSwitchArticle = LocalPullToSwitchArticle.current
    val renderer = LocalReadingRenderer.current
    val boldCharacters = LocalReadingBoldCharacters.current
    val restoreLastArticle = LocalRestoreLastArticle.current
    val openLink = LocalOpenLink.current
    val openLinkSpecificBrowser = LocalOpenLinkSpecificBrowser.current
    val sharedContent = LocalSharedContent.current

    val isOpenLinkSpecificBrowserItemEnabled = remember(openLink) {
        openLink == OpenLinkPreference.SpecificBrowser
    }

    var tonalElevationDialogVisible by remember { mutableStateOf(false) }
    var rendererDialogVisible by remember { mutableStateOf(false) }
    var fontsDialogVisible by remember { mutableStateOf(false) }
    var openLinkDialogVisible by remember { mutableStateOf(false) }
    var openLinkSpecificBrowserDialogVisible by remember { mutableStateOf(false) }
    var sharedContentDialogVisible by remember { mutableStateOf(false) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                ExternalFonts(
                    context,
                    it,
                    ExternalFonts.FontType.ReadingFont
                ).copyToInternalStorage()
                ReadingFontsPreference.External.put(context, scope)
            } ?: context.showToast("Cannot get activity result with launcher")
        }

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
        content = {
            LazyColumn {
                item {
                    DisplayText(text = stringResource(R.string.reading_page), desc = "")
                }

                // Preview
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.width(24.dp))
                        ReadingThemePreference.values.map {
                            if (readingTheme == ReadingThemePreference.Custom || it != ReadingThemePreference.Custom) {
                                ReadingThemePrev(selected = readingTheme, theme = it) {
                                    it.put(context, scope)
                                    it.applyTheme(context, scope)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(150.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Spacer(modifier = Modifier.width((24 - 8).dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                MaterialTheme.colorScheme.inverseOnSurface
                                        onLight MaterialTheme.colorScheme.surface.copy(0.7f)
                            )
                            .clickable { },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Display & Reading Engine
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.reading_display_engine)
                    )
                    SettingItem(
                        title = stringResource(R.string.content_renderer),
                        desc = renderer.toDesc(context),
                        onClick = { rendererDialogVisible = true },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.bold_characters),
                        separatedActions = renderer == ReadingRendererPreference.WebView,
                        enabled = renderer == ReadingRendererPreference.WebView,
                        desc = if (renderer == ReadingRendererPreference.WebView) null
                        else stringResource(R.string.only_available_on_webview),
                        onClick = navigateToReadingBoldCharacters,
                    ) {
                        if (renderer == ReadingRendererPreference.WebView) {
                            RYSwitch(
                                enable = renderer == ReadingRendererPreference.WebView,
                                activated = boldCharacters.value,
                            ) {
                                (!boldCharacters).put(context, scope)
                            }
                        }
                    }
                    SettingItem(
                        title = stringResource(R.string.reading_fonts),
                        desc = fonts.toDesc(context),
                        onClick = { fontsDialogVisible = true },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.auto_hide_toolbars),
                        onClick = {
                            (!autoHideToolbar).put(context, scope)
                        },
                    ) {
                        RYSwitch(activated = autoHideToolbar.value) {
                            (!autoHideToolbar).put(context, scope)
                        }
                    }
                    SettingItem(
                        title = stringResource(id = R.string.pull_to_switch_article),
                        onClick = { pullToSwitchArticle.toggle(context, scope) }) {
                        RYSwitch(activated = pullToSwitchArticle.value, onClick = {
                            pullToSwitchArticle.toggle(context, scope)
                        })
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // AI Reading Assistant
                item {
                    val readingAiSummary = LocalReadingAiSummary.current
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.reading_ai_assistant)
                    )
                    SettingItem(
                        title = stringResource(R.string.reading_ai_summary),
                        desc = stringResource(R.string.reading_ai_summary_desc),
                        onClick = {
                            (!readingAiSummary).put(context, scope)
                        },
                    ) {
                        RYSwitch(activated = readingAiSummary.value) {
                            (!readingAiSummary).put(context, scope)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Typography & Media
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.reading_typography_media)
                    )
                    SettingItem(
                        title = stringResource(R.string.title),
                        desc = stringResource(R.string.title_desc),
                        icon = Icons.Rounded.Title,
                        onClick = navigateToReadingPageTitle,
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.text),
                        desc = stringResource(R.string.text_desc),
                        icon = Icons.AutoMirrored.Rounded.Segment,
                        onClick = navigateToReadingPageText,
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.images),
                        desc = stringResource(R.string.images_desc),
                        icon = Icons.Outlined.Image,
                        onClick = navigateToReadingPageImage,
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.videos),
                        desc = stringResource(R.string.videos_desc),
                        icon = Icons.Outlined.Movie,
                        enabled = false,
                        onClick = navigateToReadingPageVideo,
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Reading Session & History
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.on_start),
                    )
                    SettingItem(
                        title = stringResource(R.string.restore_last_article),
                        desc = stringResource(R.string.restore_last_article_desc),
                        onClick = {
                            (!restoreLastArticle).put(context, scope)
                        },
                    ) {
                        RYSwitch(activated = restoreLastArticle.value) {
                            (!restoreLastArticle).put(context, scope)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // External Links & Sharing
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.external_links_sharing),
                    )
                    SettingItem(
                        title = stringResource(R.string.initial_open_app),
                        desc = openLink.toDesc(context),
                        onClick = {
                            openLinkDialogVisible = true
                        },
                    ) {}
                    SettingItem(
                        enabled = isOpenLinkSpecificBrowserItemEnabled,
                        title = stringResource(R.string.open_link_specific_browser),
                        desc = openLinkSpecificBrowser.toDesc(context),
                        onClick = {
                            openLinkSpecificBrowserDialogVisible = true
                        },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.shared_content),
                        desc = sharedContent.toDesc(context),
                        onClick = {
                            sharedContentDialogVisible = true
                        },
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Toolbars
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.toolbars)
                    )
                    SettingItem(
                        title = stringResource(R.string.tonal_elevation),
                        desc = "${tonalElevation.value}dp",
                        onClick = {
                            tonalElevationDialogVisible = true
                        },
                    ) {}
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    )

    RadioDialog(
        visible = tonalElevationDialogVisible,
        title = stringResource(R.string.tonal_elevation),
        options = ReadingPageTonalElevationPreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == tonalElevation,
            ) {
                it.put(context, scope)
            }
        }
    ) {
        tonalElevationDialogVisible = false
    }

    RadioDialog(
        visible = rendererDialogVisible,
        title = stringResource(R.string.content_renderer),
        options = ReadingRendererPreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == renderer,
            ) {
                it.put(context, scope)
            }
        }
    ) {
        rendererDialogVisible = false
    }

    RadioDialog(
        visible = fontsDialogVisible,
        title = stringResource(R.string.reading_fonts),
        options = ReadingFontsPreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                style = TextStyle(fontFamily = it.asFontFamily(context)),
                selected = it == fonts,
            ) {
                if (it.value == ReadingFontsPreference.External.value) {
                    launcher.launch(arrayOf(MimeType.FONT))
                } else {
                    it.put(context, scope)
                }
            }
        }
    ) {
        fontsDialogVisible = false
    }

    RadioDialog(
        visible = openLinkDialogVisible,
        title = stringResource(R.string.initial_open_app),
        options = OpenLinkPreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == openLink,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        openLinkDialogVisible = false
    }

    val browserList = remember(context) {
        context.getBrowserAppList()
    }

    RadioDialog(
        visible = openLinkSpecificBrowserDialogVisible,
        title = stringResource(R.string.open_link_specific_browser),
        options = browserList.map {
            RadioDialogOption(
                text = it.loadLabel(context.packageManager).toString(),
                selected = it.activityInfo.packageName == openLinkSpecificBrowser.packageName,
            ) {
                openLinkSpecificBrowser.copy(packageName = it.activityInfo.packageName)
                    .put(context, scope)
            }
        },
        onDismissRequest = {
            openLinkSpecificBrowserDialogVisible = false
        }
    )

    RadioDialog(
        visible = sharedContentDialogVisible,
        title = stringResource(R.string.shared_content),
        options = SharedContentPreference.values.map {
            RadioDialogOption(
                text = it.toDesc(context),
                selected = it == sharedContent,
            ) {
                it.put(context, scope)
            }
        },
    ) {
        sharedContentDialogVisible = false
    }
}
