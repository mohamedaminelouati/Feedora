package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.domain.model.constant.ElevationTokens
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.readingPageTonalElevation
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalReadingPageTonalElevation =
    compositionLocalOf<ReadingPageTonalElevationPreference> { ReadingPageTonalElevationPreference.default }

sealed class ReadingPageTonalElevationPreference(val value: Int) : Preference() {
    data object Outlined : ReadingPageTonalElevationPreference(ElevationTokens.Level0)
    data object Elevated : ReadingPageTonalElevationPreference(ElevationTokens.Level2)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(readingPageTonalElevation, value)
        }
    }

    fun toDesc(context: Context): String =
        when (this) {
            Outlined -> "${ElevationTokens.Level0}dp"
            Elevated -> "${ElevationTokens.Level2}dp"
        }

    companion object {

        val default = Outlined
        val values = listOf(Outlined, Elevated)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[readingPageTonalElevation]?.key as Preferences.Key<Int>]) {
                ElevationTokens.Level0 -> Outlined
                ElevationTokens.Level2 -> Elevated
                else -> default
            }
    }
}

