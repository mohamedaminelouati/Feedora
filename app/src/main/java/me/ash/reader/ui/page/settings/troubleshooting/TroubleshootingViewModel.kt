package me.ash.reader.ui.page.settings.troubleshooting

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ash.reader.R
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

data class BackupProgress(
    val isVisible: Boolean = false,
    val progress: Float = 0f,
    val title: String = "",
    val message: String = "",
)

data class TroubleshootingUiState(
    val isLoading: Boolean = false,
    val warningDialogVisible: Boolean = false,
    val progressState: BackupProgress = BackupProgress(),
)

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
        uri: Uri,
        onComplete: (Result<BackupImportResult>) -> Unit = {},
    ) {
        viewModelScope.launch(ioDispatcher) {
            val title = context.getString(R.string.importing_backup)
            _troubleshootingUiState.update {
                it.copy(
                    isLoading = true,
                    progressState = BackupProgress(isVisible = true, progress = 0.05f, title = title, message = "")
                )
            }
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    backupService.importBackup(
                        context = context,
                        inputStream = inputStream,
                        onProgress = { prog, msg ->
                            _troubleshootingUiState.update {
                                it.copy(progressState = it.progressState.copy(progress = prog, message = msg))
                            }
                        }
                    ).getOrThrow()
                } ?: throw IllegalStateException("Cannot open input stream for $uri")
            }
            _troubleshootingUiState.update {
                it.copy(isLoading = false, progressState = BackupProgress())
            }
            withContext(mainDispatcher) {
                onComplete(result)
            }
        }
    }

    fun importPreferences(
        context: Context,
        uri: Uri,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch(ioDispatcher) {
            val title = context.getString(R.string.import_preferences)
            _troubleshootingUiState.update {
                it.copy(
                    isLoading = true,
                    progressState = BackupProgress(isVisible = true, progress = 0.5f, title = title, message = "")
                )
            }
            val result = runCatching {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    backupService.importPreferencesOnly(context, inputStream).getOrThrow()
                } ?: throw IllegalStateException("Cannot open input stream for $uri")
            }
            _troubleshootingUiState.update {
                it.copy(isLoading = false, progressState = BackupProgress())
            }
            withContext(mainDispatcher) {
                onComplete(result)
            }
        }
    }

    fun exportFullBackup(
        context: Context,
        uri: Uri,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch(ioDispatcher) {
            val title = context.getString(R.string.exporting_backup)
            _troubleshootingUiState.update {
                it.copy(
                    isLoading = true,
                    progressState = BackupProgress(isVisible = true, progress = 0.02f, title = title, message = "")
                )
            }
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    backupService.exportFullBackup(
                        context = context,
                        outputStream = outputStream,
                        onProgress = { prog, msg ->
                            _troubleshootingUiState.update {
                                it.copy(progressState = it.progressState.copy(progress = prog, message = msg))
                            }
                        }
                    )
                } ?: throw IllegalStateException("Cannot open output stream for $uri")
            }
            _troubleshootingUiState.update {
                it.copy(isLoading = false, progressState = BackupProgress())
            }
            withContext(mainDispatcher) {
                onComplete(result)
            }
        }
    }

    fun exportPreferencesAsJSON(
        context: Context,
        uri: Uri,
        onComplete: (Result<Unit>) -> Unit = {},
    ) {
        viewModelScope.launch(ioDispatcher) {
            val title = context.getString(R.string.export_preferences)
            _troubleshootingUiState.update {
                it.copy(
                    isLoading = true,
                    progressState = BackupProgress(isVisible = true, progress = 0.5f, title = title, message = "")
                )
            }
            val result = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    backupService.exportPreferencesOnly(context, outputStream)
                } ?: throw IllegalStateException("Cannot open output stream for $uri")
            }
            _troubleshootingUiState.update {
                it.copy(isLoading = false, progressState = BackupProgress())
            }
            withContext(mainDispatcher) {
                onComplete(result)
            }
        }
    }

    suspend fun getSyncLogs(): List<Log> = syncLogger.list()

    fun clearSyncLogs() = viewModelScope.launch { syncLogger.clear() }
}
