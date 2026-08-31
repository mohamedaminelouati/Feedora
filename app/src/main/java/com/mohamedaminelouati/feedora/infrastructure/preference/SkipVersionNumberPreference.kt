package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.domain.model.general.Version
import com.mohamedaminelouati.feedora.domain.model.general.toVersion
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.skipVersionNumber
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalSkipVersionNumber = compositionLocalOf { SkipVersionNumberPreference.default }

object SkipVersionNumberPreference {

    val default = Version()

    fun put(context: Context, scope: CoroutineScope, value: String) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(DataStoreKey.skipVersionNumber, value)
        }
    }

    fun fromPreferences(preferences: Preferences) =
        preferences[DataStoreKey.keys[skipVersionNumber]?.key as Preferences.Key<String>].toVersion()
}
