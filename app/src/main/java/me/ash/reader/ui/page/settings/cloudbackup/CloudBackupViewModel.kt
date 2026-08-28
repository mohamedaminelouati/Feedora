package me.ash.reader.ui.page.settings.cloudbackup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ash.reader.domain.service.BackupImportResult
import me.ash.reader.domain.service.CloudBackupService
import me.ash.reader.domain.service.CloudBackupWorker
import me.ash.reader.infrastructure.preference.CloudBackupFrequency
import me.ash.reader.infrastructure.preference.CloudBackupPreferencesManager
import me.ash.reader.infrastructure.preference.CloudBackupSettings
import me.ash.reader.infrastructure.remote.RemoteBackupFile
import me.ash.reader.infrastructure.remote.RemoteStorageProtocol
import me.ash.reader.ui.page.settings.troubleshooting.BackupProgress

data class CloudBackupUiState(
    val settings: CloudBackupSettings = CloudBackupSettings(),
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null,
    val isTestSuccess: Boolean? = null,
    val progressState: BackupProgress = BackupProgress(),
    val remoteBackups: List<RemoteBackupFile> = emptyList(),
    val isLoadingRemoteBackups: Boolean = false,
)

@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cloudBackupService: CloudBackupService,
    private val preferencesManager: CloudBackupPreferencesManager,
    val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudBackupUiState())
    val uiState: StateFlow<CloudBackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.settingsFlow.collect { newSettings ->
                _uiState.update { it.copy(settings = newSettings) }
                CloudBackupWorker.schedulePeriodicWork(workManager, newSettings)
            }
        }
        loadRemoteBackups()
    }

    fun updateProtocol(protocol: RemoteStorageProtocol) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                val currentConfig = it.config
                val newPort = if (currentConfig.port == currentConfig.protocol.defaultPort) {
                    protocol.defaultPort
                } else {
                    currentConfig.port
                }
                it.copy(config = currentConfig.copy(protocol = protocol, port = newPort))
            }
        }
    }

    fun updateHost(host: String) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(config = it.config.copy(host = host))
            }
        }
    }

    fun updatePort(port: Int) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(config = it.config.copy(port = port))
            }
        }
    }

    fun updateRemotePath(path: String) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(config = it.config.copy(remotePath = path))
            }
        }
    }

    fun updateUsername(username: String) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(config = it.config.copy(username = username))
            }
        }
    }

    fun updatePassword(password: String) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(config = it.config.copy(password = password))
            }
        }
    }

    fun updateTrustInsecureSsl(trust: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(config = it.config.copy(trustInsecureSsl = trust))
            }
        }
    }

    fun updateAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(autoBackupEnabled = enabled)
            }
        }
    }

    fun updateFrequency(frequency: CloudBackupFrequency) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(frequency = frequency)
            }
        }
    }

    fun updateRequireWifi(requireWifi: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(requireWifi = requireWifi)
            }
        }
    }

    fun updateRequireCharging(requireCharging: Boolean) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(requireCharging = requireCharging)
            }
        }
    }

    fun updateMaxToKeep(maxToKeep: Int) {
        viewModelScope.launch {
            preferencesManager.updateSettings {
                it.copy(maxToKeep = maxToKeep)
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionMessage = null, isTestSuccess = null) }
            val currentSettings = _uiState.value.settings
            val result = cloudBackupService.testConnection(currentSettings.config)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        isTestSuccess = true,
                        testConnectionMessage = "Connection successful!",
                    )
                }
                loadRemoteBackups()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        isTestSuccess = false,
                        testConnectionMessage = "Connection failed: ${err.localizedMessage ?: err.message}",
                    )
                }
            }
        }
    }

    fun loadRemoteBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRemoteBackups = true) }
            val result = cloudBackupService.listRemoteBackups()
            result.onSuccess { list ->
                _uiState.update { it.copy(remoteBackups = list, isLoadingRemoteBackups = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoadingRemoteBackups = false) }
            }
        }
    }

    fun performBackup(onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progressState = BackupProgress(
                        isVisible = true,
                        progress = 0f,
                        title = "Cloud Backup",
                        message = "Starting…",
                    )
                )
            }
            val result = cloudBackupService.performBackup { progress, message ->
                _uiState.update {
                    it.copy(
                        progressState = it.progressState.copy(
                            progress = progress,
                            message = message,
                        )
                    )
                }
            }
            _uiState.update { it.copy(progressState = BackupProgress()) }
            if (result.isSuccess) {
                loadRemoteBackups()
            }
            onComplete(result)
        }
    }

    fun restoreBackup(fileName: String, onComplete: (Result<BackupImportResult>) -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progressState = BackupProgress(
                        isVisible = true,
                        progress = 0f,
                        title = "Cloud Restore",
                        message = "Starting…",
                    )
                )
            }
            val result = cloudBackupService.restoreBackup(fileName) { progress, message ->
                _uiState.update {
                    it.copy(
                        progressState = it.progressState.copy(
                            progress = progress,
                            message = message,
                        )
                    )
                }
            }
            _uiState.update { it.copy(progressState = BackupProgress()) }
            onComplete(result)
        }
    }

    fun deleteRemoteBackup(fileName: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = cloudBackupService.deleteRemoteBackup(fileName)
            if (result.isSuccess) {
                loadRemoteBackups()
            }
            onComplete(result)
        }
    }

    fun clearConfiguration() {
        viewModelScope.launch {
            preferencesManager.clearSettings()
            CloudBackupWorker.cancelWork(workManager)
            _uiState.update {
                it.copy(
                    settings = CloudBackupSettings(),
                    remoteBackups = emptyList(),
                    testConnectionMessage = null,
                    isTestSuccess = null,
                )
            }
        }
    }
}
