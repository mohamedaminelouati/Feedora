package me.ash.reader.ui.page.settings.cloudbackup

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import me.ash.reader.R
import me.ash.reader.infrastructure.preference.CloudBackupFrequency
import me.ash.reader.infrastructure.remote.RemoteBackupFile
import me.ash.reader.infrastructure.remote.RemoteStorageProtocol
import me.ash.reader.ui.component.base.DisplayText
import me.ash.reader.ui.component.base.FeedbackIconButton
import me.ash.reader.ui.component.base.RYScaffold
import me.ash.reader.ui.component.base.Subtitle
import me.ash.reader.ui.ext.DateFormat
import me.ash.reader.ui.ext.MimeType
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.ext.getCurrentVersion
import me.ash.reader.ui.ext.showToast
import me.ash.reader.ui.ext.toString
import me.ash.reader.ui.page.settings.SettingItem
import me.ash.reader.ui.theme.palette.onLight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CloudBackupPage(
    onBack: () -> Unit,
    viewModel: CloudBackupViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val uiState = viewModel.uiState.collectAsStateValue()
    val settings = uiState.settings

    var showProtocolDialog by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showRotationDialog by remember { mutableStateOf(false) }
    var showClearConfigDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    var fileToRestore by remember { mutableStateOf<RemoteBackupFile?>(null) }
    var fileToDelete by remember { mutableStateOf<RemoteBackupFile?>(null) }

    val fullBackupExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MimeType.JSON)) { uri ->
            uri?.let {
                viewModel.exportFullBackup(context, it) { res ->
                    res.onSuccess {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        context.showToast(context.getString(R.string.backup_export_success))
                    }.onFailure {
                        context.showToast(context.getString(R.string.backup_export_failed))
                    }
                }
            }
        }

    val fullBackupImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                viewModel.importFullBackup(context, it) { res ->
                    res.onSuccess {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        context.showToast(context.getString(R.string.backup_import_success))
                    }.onFailure {
                        context.showToast(context.getString(R.string.backup_import_failed))
                    }
                }
            }
        }

    val preferencesExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MimeType.JSON)) { uri ->
            uri?.let {
                viewModel.exportPreferences(context, it) { res ->
                    res.onSuccess {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        context.showToast(context.getString(R.string.backup_export_success))
                    }.onFailure {
                        context.showToast(context.getString(R.string.backup_export_failed))
                    }
                }
            }
        }

    val preferencesImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                viewModel.importPreferences(context, it) { res ->
                    res.onSuccess {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        context.showToast(context.getString(R.string.backup_import_success))
                    }.onFailure {
                        context.showToast(context.getString(R.string.backup_import_failed))
                    }
                }
            }
        }

    // Progress Dialog
    if (uiState.progressState.isVisible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = uiState.progressState.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { uiState.progressState.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = uiState.progressState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = "${(uiState.progressState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    // Restore Confirmation Dialog
    fileToRestore?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToRestore = null },
            title = { Text(stringResource(R.string.restore_remote_backup)) },
            text = { Text(stringResource(R.string.restore_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        val fileName = file.name
                        fileToRestore = null
                        viewModel.restoreBackup(fileName) { res ->
                            res.onSuccess {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                context.showToast(context.getString(R.string.cloud_restore_success))
                            }.onFailure {
                                context.showToast(context.getString(R.string.cloud_restore_failed))
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.restore_remote_backup))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRestore = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.delete_remote_backup)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    onClick = {
                        val fileName = file.name
                        fileToDelete = null
                        viewModel.deleteRemoteBackup(fileName) { res ->
                            res.onSuccess {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                context.showToast(context.getString(R.string.cloud_delete_success))
                            }.onFailure {
                                context.showToast("Delete failed")
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Protocol Selection Dialog
    if (showProtocolDialog) {
        AlertDialog(
            onDismissRequest = { showProtocolDialog = false },
            title = { Text(stringResource(R.string.protocol)) },
            text = {
                Column {
                    RemoteStorageProtocol.entries.forEach { protocol ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateProtocol(protocol)
                                    showProtocolDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.config.protocol == protocol,
                                onClick = {
                                    viewModel.updateProtocol(protocol)
                                    showProtocolDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = protocol.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProtocolDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Frequency Selection Dialog
    if (showFrequencyDialog) {
        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = { Text(stringResource(R.string.frequency)) },
            text = {
                Column {
                    CloudBackupFrequency.entries.forEach { freq ->
                        val label = when (freq) {
                            CloudBackupFrequency.DISABLED -> stringResource(R.string.freq_disabled)
                            CloudBackupFrequency.HOURS_6 -> stringResource(R.string.freq_6_hours)
                            CloudBackupFrequency.HOURS_12 -> stringResource(R.string.freq_12_hours)
                            CloudBackupFrequency.DAILY -> stringResource(R.string.freq_daily)
                            CloudBackupFrequency.WEEKLY -> stringResource(R.string.freq_weekly)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateFrequency(freq)
                                    showFrequencyDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.frequency == freq,
                                onClick = {
                                    viewModel.updateFrequency(freq)
                                    showFrequencyDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFrequencyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Rotation Selection Dialog
    if (showRotationDialog) {
        val options = listOf(3, 5, 10, 0)
        AlertDialog(
            onDismissRequest = { showRotationDialog = false },
            title = { Text(stringResource(R.string.max_backups_to_keep)) },
            text = {
                Column {
                    options.forEach { count ->
                        val label = if (count == 0) {
                            stringResource(R.string.max_backups_unlimited)
                        } else {
                            stringResource(R.string.max_backups_desc, count)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateMaxToKeep(count)
                                    showRotationDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.maxToKeep == count,
                                onClick = {
                                    viewModel.updateMaxToKeep(count)
                                    showRotationDialog = false
                                },
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRotationDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Clear Configuration Dialog
    if (showClearConfigDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfigDialog = false },
            title = { Text(stringResource(R.string.clear_server_config)) },
            text = { Text(stringResource(R.string.clear_server_config_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearConfiguration()
                        showClearConfigDialog = false
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                        context.showToast(context.getString(R.string.clear_server_config_success))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfigDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
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
                    DisplayText(
                        text = stringResource(R.string.backup_and_data),
                        desc = stringResource(R.string.backup_and_data_desc),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Section 1: Server Settings
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.cloud_backup),
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingItem(
                        title = stringResource(R.string.protocol),
                        desc = settings.config.protocol.name,
                        onClick = { showProtocolDialog = true },
                    ) {}

                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.config.host,
                            onValueChange = { viewModel.updateHost(it) },
                            label = { Text(stringResource(R.string.server_host)) },
                            placeholder = { Text(if (settings.config.protocol == RemoteStorageProtocol.WEBDAV) "cloud.example.com/remote.php/dav/files/user/" else "example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = settings.config.port.toString(),
                                onValueChange = {
                                    val p = it.toIntOrNull() ?: settings.config.protocol.defaultPort
                                    viewModel.updatePort(p)
                                },
                                label = { Text(stringResource(R.string.server_port)) },
                                modifier = Modifier.weight(0.35f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = settings.config.remotePath,
                                onValueChange = { viewModel.updateRemotePath(it) },
                                label = { Text(stringResource(R.string.remote_path)) },
                                modifier = Modifier.weight(0.65f),
                                singleLine = true,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = settings.config.username,
                            onValueChange = { viewModel.updateUsername(it) },
                            label = { Text(stringResource(R.string.username)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = settings.config.password,
                            onValueChange = { viewModel.updatePassword(it) },
                            label = { Text(stringResource(R.string.password)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (settings.config.protocol == RemoteStorageProtocol.WEBDAV || settings.config.protocol == RemoteStorageProtocol.FTPS) {
                        SettingItem(
                            title = stringResource(R.string.trust_insecure_ssl),
                            onClick = { viewModel.updateTrustInsecureSsl(!settings.config.trustInsecureSsl) },
                        ) {
                            Switch(
                                checked = settings.config.trustInsecureSsl,
                                onCheckedChange = { viewModel.updateTrustInsecureSsl(it) },
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.testConnection() },
                            enabled = !uiState.isTestingConnection && settings.config.host.isNotBlank(),
                        ) {
                            if (uiState.isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.testing_connection))
                            } else {
                                Text(stringResource(R.string.test_connection))
                            }
                        }

                        uiState.testConnectionMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = if (uiState.isTestSuccess == true) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (uiState.isTestSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.isTestSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        if (settings.config.host.isNotBlank() || settings.config.username.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showClearConfigDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.clear_server_config))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Section 2: Automated Backup Settings
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.auto_backup),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingItem(
                        title = stringResource(R.string.auto_backup),
                        desc = stringResource(R.string.auto_backup_desc),
                        onClick = { viewModel.updateAutoBackupEnabled(!settings.autoBackupEnabled) },
                    ) {
                        Switch(
                            checked = settings.autoBackupEnabled,
                            onCheckedChange = { viewModel.updateAutoBackupEnabled(it) },
                        )
                    }

                    if (settings.autoBackupEnabled) {
                        val freqDesc = when (settings.frequency) {
                            CloudBackupFrequency.DISABLED -> stringResource(R.string.freq_disabled)
                            CloudBackupFrequency.HOURS_6 -> stringResource(R.string.freq_6_hours)
                            CloudBackupFrequency.HOURS_12 -> stringResource(R.string.freq_12_hours)
                            CloudBackupFrequency.DAILY -> stringResource(R.string.freq_daily)
                            CloudBackupFrequency.WEEKLY -> stringResource(R.string.freq_weekly)
                        }
                        SettingItem(
                            title = stringResource(R.string.frequency),
                            desc = freqDesc,
                            onClick = { showFrequencyDialog = true },
                        ) {}

                        SettingItem(
                            title = stringResource(R.string.require_wifi),
                            desc = stringResource(R.string.require_wifi_desc),
                            onClick = { viewModel.updateRequireWifi(!settings.requireWifi) },
                        ) {
                            Switch(
                                checked = settings.requireWifi,
                                onCheckedChange = { viewModel.updateRequireWifi(it) },
                            )
                        }

                        SettingItem(
                            title = stringResource(R.string.require_charging),
                            desc = stringResource(R.string.require_charging_desc),
                            onClick = { viewModel.updateRequireCharging(!settings.requireCharging) },
                        ) {
                            Switch(
                                checked = settings.requireCharging,
                                onCheckedChange = { viewModel.updateRequireCharging(it) },
                            )
                        }

                        val rotationDesc = if (settings.maxToKeep == 0) {
                            stringResource(R.string.max_backups_unlimited)
                        } else {
                            stringResource(R.string.max_backups_desc, settings.maxToKeep)
                        }
                        SettingItem(
                            title = stringResource(R.string.max_backups_to_keep),
                            desc = rotationDesc,
                            onClick = { showRotationDialog = true },
                        ) {}
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Section 3: Manual Action & Remote Backups
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.remote_backups),
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingItem(
                        title = stringResource(R.string.backup_now),
                        desc = if (settings.lastBackupTime > 0) {
                            val d = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(settings.lastBackupTime))
                            "Dernière : $d (${settings.lastBackupStatus})"
                        } else null,
                        icon = Icons.Outlined.CloudUpload,
                        onClick = {
                            viewModel.performBackup { res ->
                                res.onSuccess {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                    context.showToast(context.getString(R.string.cloud_backup_success))
                                }.onFailure {
                                    context.showToast(context.getString(R.string.cloud_backup_failed))
                                }
                            }
                        },
                    ) {}
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.isLoadingRemoteBackups) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (uiState.remoteBackups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.no_remote_backups),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(uiState.remoteBackups) { remoteFile ->
                        RemoteBackupItemView(
                            file = remoteFile,
                            onRestore = { fileToRestore = remoteFile },
                            onDelete = { fileToDelete = remoteFile },
                        )
                    }
                }

                // Section 4: Full Backup & Restore (Local)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.local_backup_restore),
                    )
                    SettingItem(
                        title = stringResource(R.string.export_full_backup),
                        desc = stringResource(R.string.export_full_backup_desc),
                        onClick = { fullBackupFileLauncher(context, fullBackupExportLauncher) },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.import_full_backup),
                        onClick = { fullBackupImportLauncher.launch(arrayOf(MimeType.ANY)) },
                    ) {}
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Section 5: App Preferences Only
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.app_preferences),
                    )
                    SettingItem(
                        title = stringResource(R.string.import_preferences),
                        onClick = { preferencesImportLauncher.launch(arrayOf(MimeType.ANY)) },
                    ) {}
                    SettingItem(
                        title = stringResource(R.string.export_preferences),
                        onClick = { preferenceFileLauncher(context, preferencesExportLauncher) },
                    ) {}
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Section 6: Storage & Cache
                item {
                    Subtitle(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = stringResource(R.string.storage_and_cache),
                    )
                    SettingItem(
                        title = stringResource(R.string.clear_cache),
                        desc = stringResource(R.string.clear_cache_desc),
                        onClick = {
                            viewModel.clearCache(context) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                context.showToast(context.getString(R.string.clear_cache_success))
                            }
                        },
                    ) {}
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    )
}

private fun fullBackupFileLauncher(
    context: Context,
    launcher: ManagedActivityResultLauncher<String, Uri?>,
) {
    launcher.launch(
        "Read-You-" +
            "${context.getCurrentVersion()}-full-backup-" +
            "${Date().toString(DateFormat.YYYY_MM_DD_DASH_HH_MM_SS_DASH)}.json"
    )
}

private fun preferenceFileLauncher(
    context: Context,
    launcher: ManagedActivityResultLauncher<String, Uri?>,
) {
    launcher.launch(
        "Read-You-" +
            "${context.getCurrentVersion()}-settings-" +
            "${Date().toString(DateFormat.YYYY_MM_DD_DASH_HH_MM_SS_DASH)}.json"
    )
}

@Composable
fun RemoteBackupItemView(
    file: RemoteBackupFile,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(file.lastModified))
    val sizeKb = (file.size / 1024.0).let {
        if (it > 1024) String.format(Locale.US, "%.1f MB", it / 1024.0)
        else String.format(Locale.US, "%.0f KB", it)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateStr • $sizeKb",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = stringResource(R.string.restore_remote_backup),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.delete_remote_backup),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
