package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalReadingAiSummary =
    compositionLocalOf<ReadingAiSummaryPreference> { ReadingAiSummaryPreference.default }

sealed class ReadingAiSummaryPreference(val value: Boolean) : Preference() {
    data object ON : ReadingAiSummaryPreference(true)
    data object OFF : ReadingAiSummaryPreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.readingAiSummary,
                value
            )
        }
    }

    companion object {
        val default = ON
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[DataStoreKey.readingAiSummary]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}

operator fun ReadingAiSummaryPreference.not(): ReadingAiSummaryPreference =
    when (value) {
        true -> ReadingAiSummaryPreference.OFF
        false -> ReadingAiSummaryPreference.ON
    }
