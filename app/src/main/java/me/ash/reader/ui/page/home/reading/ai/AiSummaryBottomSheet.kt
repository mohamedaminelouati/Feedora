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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TipsAndUpdates
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ash.reader.infrastructure.ai.AiLanguage
import me.ash.reader.infrastructure.ai.AiSummaryStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummaryBottomSheet(
    title: String,
    content: String,
    onDismissRequest: () -> Unit,
    viewModel: AiSummaryViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedStyle by viewModel.selectedStyle.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(title, content) {
        if (uiState is AiSummaryUiState.Idle) {
            viewModel.generateSummary(title, content)
        }
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
                        imageVector = Icons.Rounded.TipsAndUpdates,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Résumé IA",
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
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Options Row (Language selector & Style selector)
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
                        label = { Text("Langue : ${selectedLanguage.displayName}") },
                    )
                    DropdownMenu(
                        expanded = isLangMenuExpanded,
                        onDismissRequest = { isLangMenuExpanded = false },
                    ) {
                        AiLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = {
                                    isLangMenuExpanded = false
                                    if (selectedLanguage != lang) {
                                        viewModel.setLanguage(lang)
                                        viewModel.generateSummary(title, content)
                                    }
                                },
                                trailingIcon = {
                                    if (selectedLanguage == lang) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                },
                            )
                        }
                    }
                }

                // Style Dropdown
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
                                text = { Text(style.displayName) },
                                onClick = {
                                    isStyleMenuExpanded = false
                                    if (selectedStyle != style) {
                                        viewModel.setStyle(style)
                                        viewModel.generateSummary(title, content)
                                    }
                                },
                                trailingIcon = {
                                    if (selectedStyle == style) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Body based on state
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "AiSummaryContent",
            ) { state ->
                when (state) {
                    is AiSummaryUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Génération du résumé en cours...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    is AiSummaryUiState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                val layoutDir =
                                    if (state.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                                CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                                    SelectionContainer {
                                        Text(
                                            text = state.summary,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                lineHeight = 24.sp,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier =
                                                Modifier.padding(16.dp)
                                                    .verticalScroll(rememberScrollState()),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        viewModel.generateSummary(title, content)
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Régénérer")
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Button(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        clipboardManager.setText(AnnotatedString(state.summary))
                                        Toast.makeText(context, "Résumé copié dans le presse-papier", Toast.LENGTH_SHORT).show()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copier")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    is AiSummaryUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = {
                                    viewModel.generateSummary(title, content)
                                }) {
                                    Text("Réessayer")
                                }
                            }
                        }
                    }

                    AiSummaryUiState.Idle -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Button(onClick = {
                                viewModel.generateSummary(title, content)
                            }) {
                                Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Générer le résumé")
                            }
                        }
                    }
                }
            }
        }
    }
}
