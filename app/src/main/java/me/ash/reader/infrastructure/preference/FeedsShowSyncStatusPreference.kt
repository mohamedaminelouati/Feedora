package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalFeedsShowSyncStatus =
    compositionLocalOf<FeedsShowSyncStatusPreference> { FeedsShowSyncStatusPreference.default }

sealed class FeedsShowSyncStatusPreference(val value: Boolean) : Preference() {
    data object ON : FeedsShowSyncStatusPreference(true)
    data object OFF : FeedsShowSyncStatusPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.feedsShowSyncStatus,
                value
            )
        }
    }

    companion object {
        val default = ON
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[DataStoreKey.feedsShowSyncStatus]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}

operator fun FeedsShowSyncStatusPreference.not(): FeedsShowSyncStatusPreference =
    when (value) {
        true -> FeedsShowSyncStatusPreference.OFF
        false -> FeedsShowSyncStatusPreference.ON
    }
