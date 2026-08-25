package me.ash.reader.ui.page.home.reading.ai

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ash.reader.R
import me.ash.reader.infrastructure.ai.AiLanguage
import me.ash.reader.infrastructure.ai.AiSummaryStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummaryBottomSheet(
    title: String,
    content: String,
    initialMode: AiFeatureMode = AiFeatureMode.SUMMARY,
    onDismissRequest: () -> Unit,
    viewModel: AiSummaryViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentMode by viewModel.featureMode.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedStyle by viewModel.selectedStyle.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(title, content, initialMode) {
        viewModel.initMode(initialMode, title, content)
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ai_summary),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (currentMode == AiFeatureMode.SUMMARY) {
                            stringResource(R.string.ai_summary)
                        } else {
                            stringResource(R.string.ai_full_translation)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.reset()
                    onDismissRequest()
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mode Tabs (Summary vs Translation)
            TabRow(
                selectedTabIndex = currentMode.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                AiFeatureMode.entries.forEach { mode ->
                    Tab(
                        selected = currentMode == mode,
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.setFeatureMode(mode, title, content)
                        },
                        text = {
                            Text(
                                text = if (mode == AiFeatureMode.SUMMARY) {
                                    stringResource(R.string.ai_summary)
                                } else {
                                    stringResource(R.string.ai_full_translation)
                                },
                                fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Options Row (Language selector & Style selector if summary mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Language Dropdown
                var isLangMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = true,
                        onClick = { isLangMenuExpanded = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(selectedLanguage.displayName)
                            }
                        },
                    )

                    DropdownMenu(
                        expanded = isLangMenuExpanded,
                        onDismissRequest = { isLangMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 350.dp),
                    ) {
                        val availableLanguages = if (currentMode == AiFeatureMode.TRANSLATION) {
                            AiLanguage.entries.filter { it != AiLanguage.AUTO }
                        } else {
                            AiLanguage.entries
                        }
                        availableLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(lang.displayName)
                                        if (selectedLanguage == lang) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    isLangMenuExpanded = false
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.setLanguage(lang, title, content)
                                },
                            )
                        }
                    }
                }

                // Style Selector (Only for Summary mode)
                if (currentMode == AiFeatureMode.SUMMARY) {
                    var isStyleMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = false,
                            onClick = { isStyleMenuExpanded = true },
                            label = { Text(selectedStyle.displayName) },
                        )

                        DropdownMenu(
                            expanded = isStyleMenuExpanded,
                            onDismissRequest = { isStyleMenuExpanded = false },
                        ) {
                            AiSummaryStyle.entries.forEach { style ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(style.displayName)
                                            if (selectedStyle == style) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        isStyleMenuExpanded = false
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.setStyle(style, title, content)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Content Box
            Surface(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = 160.dp, max = 460.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AiContentAnimation",
                ) { state ->
                    when (state) {
                        is AiSummaryUiState.Loading,
                        is AiSummaryUiState.Idle -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp,
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (currentMode == AiFeatureMode.SUMMARY) {
                                            stringResource(R.string.ai_generating_summary)
                                        } else {
                                            stringResource(R.string.ai_translating_article)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        is AiSummaryUiState.Success -> {
                            val layoutDirection =
                                if (state.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

                            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                                SelectionContainer {
                                    Column(
                                        modifier =
                                            Modifier.fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                                .padding(18.dp),
                                    ) {
                                        Text(
                                            text = state.result,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                lineHeight = 26.sp,
                                                fontSize = 15.5.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        is AiSummaryUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp),
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            viewModel.executeAction(title, content)
                                        }
                                    ) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            if (uiState is AiSummaryUiState.Success) {
                val successResult = (uiState as AiSummaryUiState.Success).result
                val copiedMsg = stringResource(R.string.copied_to_clipboard)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.executeAction(title, content)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.regenerate))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            clipboardManager.setText(AnnotatedString(successResult))
                            Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.copy))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
