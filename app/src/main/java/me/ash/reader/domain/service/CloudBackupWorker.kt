package me.ash.reader.domain.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import me.ash.reader.infrastructure.preference.CloudBackupFrequency
import me.ash.reader.infrastructure.preference.CloudBackupSettings

@HiltWorker
class CloudBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cloudBackupService: CloudBackupService,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val result = cloudBackupService.performBackup()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val CLOUD_BACKUP_WORK_NAME = "CloudBackupPeriodicWork"
        const val CLOUD_BACKUP_TAG = "CLOUD_BACKUP_TAG"

        fun schedulePeriodicWork(workManager: WorkManager, settings: CloudBackupSettings) {
            if (!settings.autoBackupEnabled || settings.frequency == CloudBackupFrequency.DISABLED || settings.config.host.isBlank()) {
                cancelWork(workManager)
                return
            }

            val intervalHours = settings.frequency.intervalHours
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (settings.requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresCharging(settings.requireCharging)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CloudBackupWorker>(
                intervalHours, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(CLOUD_BACKUP_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                CLOUD_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )
        }

        fun cancelWork(workManager: WorkManager) {
            workManager.cancelUniqueWork(CLOUD_BACKUP_WORK_NAME)
        }
    }
}
