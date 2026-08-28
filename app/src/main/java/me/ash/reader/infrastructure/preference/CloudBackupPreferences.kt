package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.ash.reader.infrastructure.remote.RemoteServerConfig
import me.ash.reader.infrastructure.remote.RemoteStorageProtocol
import me.ash.reader.ui.ext.dataStore

enum class CloudBackupFrequency(val intervalHours: Long) {
    DISABLED(0),
    HOURS_6(6),
    HOURS_12(12),
    DAILY(24),
    WEEKLY(168);

    companion object {
        fun fromName(name: String?): CloudBackupFrequency {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: DISABLED
        }
    }
}

object CloudBackupDataStoreKeys {
    val PROTOCOL = stringPreferencesKey("cloud_backup_protocol")
    val HOST = stringPreferencesKey("cloud_backup_host")
    val PORT = intPreferencesKey("cloud_backup_port")
    val REMOTE_PATH = stringPreferencesKey("cloud_backup_remote_path")
    val USERNAME = stringPreferencesKey("cloud_backup_username")
    val PASSWORD = stringPreferencesKey("cloud_backup_password")
    val TRUST_INSECURE_SSL = booleanPreferencesKey("cloud_backup_trust_insecure_ssl")
    val AUTO_ENABLED = booleanPreferencesKey("cloud_backup_auto_enabled")
    val FREQUENCY = stringPreferencesKey("cloud_backup_frequency")
    val REQUIRE_WIFI = booleanPreferencesKey("cloud_backup_require_wifi")
    val REQUIRE_CHARGING = booleanPreferencesKey("cloud_backup_require_charging")
    val MAX_TO_KEEP = intPreferencesKey("cloud_backup_max_to_keep")
    val LAST_BACKUP_TIME = longPreferencesKey("cloud_backup_last_backup_time")
    val LAST_BACKUP_STATUS = stringPreferencesKey("cloud_backup_last_backup_status")
}

data class CloudBackupSettings(
    val config: RemoteServerConfig = RemoteServerConfig(),
    val autoBackupEnabled: Boolean = false,
    val frequency: CloudBackupFrequency = CloudBackupFrequency.DISABLED,
    val requireWifi: Boolean = true,
    val requireCharging: Boolean = false,
    val maxToKeep: Int = 5,
    val lastBackupTime: Long = 0L,
    val lastBackupStatus: String = "",
)

@Singleton
class CloudBackupPreferencesManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    val settingsFlow: Flow<CloudBackupSettings> = context.dataStore.data.map { prefs ->
        val protocol = RemoteStorageProtocol.fromString(prefs[CloudBackupDataStoreKeys.PROTOCOL] ?: RemoteStorageProtocol.WEBDAV.name)
        val host = prefs[CloudBackupDataStoreKeys.HOST] ?: ""
        val port = prefs[CloudBackupDataStoreKeys.PORT] ?: protocol.defaultPort
        val remotePath = prefs[CloudBackupDataStoreKeys.REMOTE_PATH] ?: "/ReadYou/"
        val username = prefs[CloudBackupDataStoreKeys.USERNAME] ?: ""
        val password = prefs[CloudBackupDataStoreKeys.PASSWORD] ?: ""
        val trustInsecureSsl = prefs[CloudBackupDataStoreKeys.TRUST_INSECURE_SSL] ?: false
        val autoBackupEnabled = prefs[CloudBackupDataStoreKeys.AUTO_ENABLED] ?: false
        val frequency = CloudBackupFrequency.fromName(prefs[CloudBackupDataStoreKeys.FREQUENCY])
        val requireWifi = prefs[CloudBackupDataStoreKeys.REQUIRE_WIFI] ?: true
        val requireCharging = prefs[CloudBackupDataStoreKeys.REQUIRE_CHARGING] ?: false
        val maxToKeep = prefs[CloudBackupDataStoreKeys.MAX_TO_KEEP] ?: 5
        val lastBackupTime = prefs[CloudBackupDataStoreKeys.LAST_BACKUP_TIME] ?: 0L
        val lastBackupStatus = prefs[CloudBackupDataStoreKeys.LAST_BACKUP_STATUS] ?: ""

        CloudBackupSettings(
            config = RemoteServerConfig(
                protocol = protocol,
                host = host,
                port = port,
                remotePath = remotePath,
                username = username,
                password = password,
                trustInsecureSsl = trustInsecureSsl,
            ),
            autoBackupEnabled = autoBackupEnabled,
            frequency = frequency,
            requireWifi = requireWifi,
            requireCharging = requireCharging,
            maxToKeep = maxToKeep,
            lastBackupTime = lastBackupTime,
            lastBackupStatus = lastBackupStatus,
        )
    }

    suspend fun getSettings(): CloudBackupSettings = settingsFlow.first()

    suspend fun updateSettings(transform: (CloudBackupSettings) -> CloudBackupSettings) {
        val current = getSettings()
        val updated = transform(current)
        context.dataStore.edit { prefs ->
            prefs[CloudBackupDataStoreKeys.PROTOCOL] = updated.config.protocol.name
            prefs[CloudBackupDataStoreKeys.HOST] = updated.config.host
            prefs[CloudBackupDataStoreKeys.PORT] = updated.config.port
            prefs[CloudBackupDataStoreKeys.REMOTE_PATH] = updated.config.remotePath
            prefs[CloudBackupDataStoreKeys.USERNAME] = updated.config.username
            prefs[CloudBackupDataStoreKeys.PASSWORD] = updated.config.password
            prefs[CloudBackupDataStoreKeys.TRUST_INSECURE_SSL] = updated.config.trustInsecureSsl
            prefs[CloudBackupDataStoreKeys.AUTO_ENABLED] = updated.autoBackupEnabled
            prefs[CloudBackupDataStoreKeys.FREQUENCY] = updated.frequency.name
            prefs[CloudBackupDataStoreKeys.REQUIRE_WIFI] = updated.requireWifi
            prefs[CloudBackupDataStoreKeys.REQUIRE_CHARGING] = updated.requireCharging
            prefs[CloudBackupDataStoreKeys.MAX_TO_KEEP] = updated.maxToKeep
            prefs[CloudBackupDataStoreKeys.LAST_BACKUP_TIME] = updated.lastBackupTime
            prefs[CloudBackupDataStoreKeys.LAST_BACKUP_STATUS] = updated.lastBackupStatus
        }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { prefs ->
            prefs.remove(CloudBackupDataStoreKeys.PROTOCOL)
            prefs.remove(CloudBackupDataStoreKeys.HOST)
            prefs.remove(CloudBackupDataStoreKeys.PORT)
            prefs.remove(CloudBackupDataStoreKeys.REMOTE_PATH)
            prefs.remove(CloudBackupDataStoreKeys.USERNAME)
            prefs.remove(CloudBackupDataStoreKeys.PASSWORD)
            prefs.remove(CloudBackupDataStoreKeys.TRUST_INSECURE_SSL)
            prefs.remove(CloudBackupDataStoreKeys.AUTO_ENABLED)
            prefs.remove(CloudBackupDataStoreKeys.FREQUENCY)
            prefs.remove(CloudBackupDataStoreKeys.REQUIRE_WIFI)
            prefs.remove(CloudBackupDataStoreKeys.REQUIRE_CHARGING)
            prefs.remove(CloudBackupDataStoreKeys.MAX_TO_KEEP)
            prefs.remove(CloudBackupDataStoreKeys.LAST_BACKUP_TIME)
            prefs.remove(CloudBackupDataStoreKeys.LAST_BACKUP_STATUS)
        }
    }
}
