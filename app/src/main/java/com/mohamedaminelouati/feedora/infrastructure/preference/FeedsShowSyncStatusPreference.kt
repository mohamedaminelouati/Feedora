package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalFeedsShowSyncStatus =
    compositionLocalOf<FeedsShowSyncStatusPreference> { FeedsShowSyncStatusPreference.default }

sealed class FeedsShowSyncStatusPreference(val value: Int) : Preference() {
    data object Always : FeedsShowSyncStatusPreference(0)
    data object ErrorsOnly : FeedsShowSyncStatusPreference(1)
    data object Never : FeedsShowSyncStatusPreference(2)

    @Composable
    fun toDesc(): String = when (this) {
        Always -> stringResource(R.string.sync_status_always)
        ErrorsOnly -> stringResource(R.string.sync_status_errors_only)
        Never -> stringResource(R.string.sync_status_never)
    }

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.feedsShowSyncStatus,
                value
            )
        }
    }

    companion object {
        val default = Always
        val values = listOf(Always, ErrorsOnly, Never)

        fun fromValue(value: Int) = when (value) {
            0 -> Always
            1 -> ErrorsOnly
            2 -> Never
            else -> default
        }

        fun fromPreferences(preferences: Preferences): FeedsShowSyncStatusPreference {
            val key = intPreferencesKey(DataStoreKey.feedsShowSyncStatus)
            return preferences[key]?.let { fromValue(it) } ?: default
        }
    }
}
