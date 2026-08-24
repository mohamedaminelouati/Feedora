package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalRestoreLastArticle =
    compositionLocalOf<RestoreLastArticlePreference> { RestoreLastArticlePreference.default }

sealed class RestoreLastArticlePreference(val value: Boolean) : Preference() {
    data object ON : RestoreLastArticlePreference(true)
    data object OFF : RestoreLastArticlePreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.restoreLastArticle,
                value
            )
        }
    }

    companion object {
        val default = OFF
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[DataStoreKey.restoreLastArticle]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}

operator fun RestoreLastArticlePreference.not(): RestoreLastArticlePreference =
    when (value) {
        true -> RestoreLastArticlePreference.OFF
        false -> RestoreLastArticlePreference.ON
    }
