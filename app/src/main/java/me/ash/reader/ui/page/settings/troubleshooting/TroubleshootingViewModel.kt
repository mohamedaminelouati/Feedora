package me.ash.reader.ui.page.settings.troubleshooting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ash.reader.domain.data.Log
import me.ash.reader.domain.data.SyncLogger
import me.ash.reader.domain.service.AccountService
import me.ash.reader.domain.service.BackupImportResult
import me.ash.reader.domain.service.BackupService
import me.ash.reader.domain.service.OpmlService
import me.ash.reader.domain.service.RssService
import me.ash.reader.infrastructure.di.ApplicationScope
import me.ash.reader.infrastructure.di.DefaultDispatcher
import me.ash.reader.infrastructure.di.IODispatcher
import me.ash.reader.infrastructure.di.MainDispatcher

@HiltViewModel
class TroubleshootingViewModel
@Inject
constructor(
    private val accountService: AccountService,
    private val rssService: RssService,
    private val opmlService: OpmlService,
    private val backupService: BackupService,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope,
    val workManager: WorkManager,
    private val syncLogger: SyncLogger,
) : ViewModel() {

    private val _troubleshootingUiState = MutableStateFlow(TroubleshootingUiState())
    val troubleshootingUiState: StateFlow<TroubleshootingUiState> =
        _troubleshootingUiState.asStateFlow()

    fun showWarningDialog() {
        _troubleshootingUiState.update { it.copy(warningDialogVisible = true) }
    }

    fun hideWarningDialog() {
        _troubleshootingUiState.update { it.copy(warningDialogVisible = false) }
    }

    fun importBackup(
        context: Context,
        inputStream: InputStream,
        onComplete: (Result<BackupImportResult>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _troubleshootingUiState.update { it.copy(isLoading = true) }
            val result = backupService.importBackup(context, inputStream)
            _troubleshootingUiState.update { it.copy(isLoading = false) }
            onComplete(result)
        }
    }

    fun importPreferences(
        context: Context,
        inputStream: InputStream,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _troubleshootingUiState.update { it.copy(isLoading = true) }
            val result = backupService.importPreferencesOnly(context, inputStream)
            _troubleshootingUiState.update { it.copy(isLoading = false) }
            onComplete(result)
        }
    }

    fun exportFullBackup(
        context: Context,
        outputStream: OutputStream,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _troubleshootingUiState.update { it.copy(isLoading = true) }
            val result = runCatching { backupService.exportFullBackup(context, outputStream) }
            _troubleshootingUiState.update { it.copy(isLoading = false) }
            onComplete(result)
        }
    }

    fun exportPreferencesAsJSON(
        context: Context,
        outputStream: OutputStream,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch {
            _troubleshootingUiState.update { it.copy(isLoading = true) }
            val result = runCatching { backupService.exportPreferencesOnly(context, outputStream) }
            _troubleshootingUiState.update { it.copy(isLoading = false) }
            onComplete(result)
        }
    }

    suspend fun getSyncLogs(): List<Log> = syncLogger.list()

    fun clearSyncLogs() = viewModelScope.launch { syncLogger.clear() }
}

data class TroubleshootingUiState(
    val isLoading: Boolean = false,
    val warningDialogVisible: Boolean = false,
)
