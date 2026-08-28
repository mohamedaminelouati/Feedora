package me.ash.reader.domain.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.ash.reader.infrastructure.di.IODispatcher
import me.ash.reader.infrastructure.preference.CloudBackupPreferencesManager
import me.ash.reader.infrastructure.preference.CloudBackupSettings
import me.ash.reader.infrastructure.remote.RemoteBackupFile
import me.ash.reader.infrastructure.remote.RemoteServerConfig
import me.ash.reader.infrastructure.remote.RemoteStorageClientFactory
import me.ash.reader.ui.ext.getCurrentVersion

@Singleton
class CloudBackupService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val backupService: BackupService,
    private val clientFactory: RemoteStorageClientFactory,
    private val preferencesManager: CloudBackupPreferencesManager,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun testConnection(config: RemoteServerConfig): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            if (config.host.isBlank()) {
                throw IllegalArgumentException("Host cannot be empty")
            }
            val client = clientFactory.createClient(config)
            client.testConnection().getOrThrow()
        }
    }

    suspend fun listRemoteBackups(): Result<List<RemoteBackupFile>> = withContext(ioDispatcher) {
        runCatching {
            val settings = preferencesManager.getSettings()
            if (settings.config.host.isBlank()) {
                return@runCatching emptyList()
            }
            val client = clientFactory.createClient(settings.config)
            client.listBackups().getOrThrow()
        }
    }

    suspend fun deleteRemoteBackup(fileName: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val settings = preferencesManager.getSettings()
            val client = clientFactory.createClient(settings.config)
            client.delete(fileName).getOrThrow()
        }
    }

    suspend fun performBackup(
        onProgress: suspend (progress: Float, status: String) -> Unit = { _, _ -> },
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val settings = preferencesManager.getSettings()
            if (settings.config.host.isBlank()) {
                throw IllegalStateException("Cloud backup host is not configured")
            }

            val client = clientFactory.createClient(settings.config)
            val tempFile = File(context.cacheDir, "cloud_backup_temp_${System.currentTimeMillis()}.json")

            try {
                // 1. Export local data to temp file (0.0f - 0.5f)
                onProgress(0.05f, "Exporting local data…")
                FileOutputStream(tempFile).use { outputStream ->
                    backupService.exportFullBackup(context, outputStream) { p, msg ->
                        onProgress(p * 0.5f, msg)
                    }
                }

                // 2. Upload to remote server (0.5f - 0.95f)
                onProgress(0.55f, "Uploading to ${settings.config.protocol.name}…")
                val dateStr = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date())
                val appVersion = context.getCurrentVersion()
                val remoteFileName = "Read-You-$appVersion-full-backup-$dateStr.json"

                FileInputStream(tempFile).use { inputStream ->
                    val uploadResult = client.upload(
                        fileName = remoteFileName,
                        inputStream = inputStream,
                        contentLength = tempFile.length(),
                        onProgress = { p ->
                            val currentProgress = 0.5f + (p * 0.45f)
                            // Progress update
                        },
                    )
                    uploadResult.getOrThrow()
                }

                // 3. Rotation: Keep only the max configured number of backups
                if (settings.maxToKeep > 0) {
                    onProgress(0.95f, "Cleaning old backups…")
                    val existing = client.listBackups().getOrNull().orEmpty()
                    if (existing.size > settings.maxToKeep) {
                        val toDelete = existing.drop(settings.maxToKeep)
                        for (oldFile in toDelete) {
                            runCatching { client.delete(oldFile.name) }
                        }
                    }
                }

                preferencesManager.updateSettings {
                    it.copy(
                        lastBackupTime = System.currentTimeMillis(),
                        lastBackupStatus = "Success",
                    )
                }

                onProgress(1.0f, "Completed")
                remoteFileName
            } catch (e: Exception) {
                preferencesManager.updateSettings {
                    it.copy(
                        lastBackupStatus = "Failed: ${e.localizedMessage ?: e.message}",
                    )
                }
                throw e
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }

    suspend fun restoreBackup(
        remoteFileName: String,
        onProgress: suspend (progress: Float, status: String) -> Unit = { _, _ -> },
    ): Result<BackupImportResult> = withContext(ioDispatcher) {
        runCatching {
            val settings = preferencesManager.getSettings()
            if (settings.config.host.isBlank()) {
                throw IllegalStateException("Cloud backup host is not configured")
            }

            val client = clientFactory.createClient(settings.config)
            val tempFile = File(context.cacheDir, "cloud_restore_temp_${System.currentTimeMillis()}.json")

            try {
                // 1. Download remote file (0.0f - 0.5f)
                onProgress(0.10f, "Downloading from ${settings.config.protocol.name}…")
                FileOutputStream(tempFile).use { outputStream ->
                    val downloadResult = client.download(
                        fileName = remoteFileName,
                        outputStream = outputStream,
                        onProgress = { p ->
                            // Download progress
                        },
                    )
                    downloadResult.getOrThrow()
                }

                // 2. Import into database (0.5f - 1.0f)
                onProgress(0.50f, "Restoring data…")
                val importResult = FileInputStream(tempFile).use { inputStream ->
                    backupService.importBackup(context, inputStream) { p, msg ->
                        onProgress(0.5f + (p * 0.5f), msg)
                    }.getOrThrow()
                }

                onProgress(1.0f, "Completed")
                importResult
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }
    }
}
