package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalRestoreScrollPosition =
    compositionLocalOf<RestoreScrollPositionPreference> { RestoreScrollPositionPreference.default }

sealed class RestoreScrollPositionPreference(val value: Boolean) : Preference() {
    data object ON : RestoreScrollPositionPreference(true)
    data object OFF : RestoreScrollPositionPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.restoreScrollPosition,
                value
            )
        }
    }

    companion object {
        val default = ON
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[DataStoreKey.restoreScrollPosition]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}

operator fun RestoreScrollPositionPreference.not(): RestoreScrollPositionPreference =
    when (value) {
        true -> RestoreScrollPositionPreference.OFF
        false -> RestoreScrollPositionPreference.ON
    }
