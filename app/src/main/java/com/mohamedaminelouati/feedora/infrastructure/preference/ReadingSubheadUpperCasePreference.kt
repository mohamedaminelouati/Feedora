package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.readingSubheadUpperCase
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalReadingSubheadUpperCase =
    compositionLocalOf<ReadingSubheadUpperCasePreference> { ReadingSubheadUpperCasePreference.default }

sealed class ReadingSubheadUpperCasePreference(val value: Boolean) : Preference() {
    object ON : ReadingSubheadUpperCasePreference(true)
    object OFF : ReadingSubheadUpperCasePreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.readingSubheadUpperCase,
                value
            )
        }
    }

    companion object {

        val default = OFF
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[readingSubheadUpperCase]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}

operator fun ReadingSubheadUpperCasePreference.not(): ReadingSubheadUpperCasePreference =
    when (value) {
        true -> ReadingSubheadUpperCasePreference.OFF
        false -> ReadingSubheadUpperCasePreference.ON
    }
