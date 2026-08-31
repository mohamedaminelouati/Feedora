package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.domain.model.general.Version
import com.mohamedaminelouati.feedora.domain.model.general.toVersion
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.newVersionNumber
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalNewVersionNumber = compositionLocalOf { NewVersionNumberPreference.default }

object NewVersionNumberPreference {

    val provide: (Settings) -> ProvidedValue<Version> =
        fun(settings: Settings) = LocalNewVersionNumber provides settings.newVersionNumber

    val default = Version()

    fun put(context: Context, scope: CoroutineScope, value: String) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.put(newVersionNumber, value)
        }
    }

    fun fromPreferences(preferences: Preferences) =
        preferences[DataStoreKey.keys[newVersionNumber]?.key as Preferences.Key<String>].toVersion()
}
