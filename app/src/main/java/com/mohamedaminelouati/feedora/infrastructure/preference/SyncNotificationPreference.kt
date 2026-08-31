package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalSyncNotification =
    compositionLocalOf<SyncNotificationPreference> { SyncNotificationPreference.default }

sealed class SyncNotificationPreference(val value: Boolean) : Preference() {
    data object ON : SyncNotificationPreference(true)
    data object OFF : SyncNotificationPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.syncNotification,
                value
            )
        }
    }

    companion object {
        val default = ON
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[DataStoreKey.syncNotification]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}

operator fun SyncNotificationPreference.not(): SyncNotificationPreference =
    when (value) {
        true -> SyncNotificationPreference.OFF
        false -> SyncNotificationPreference.ON
    }
