package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.readingTitleUpperCase
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalReadingTitleUpperCase =
    compositionLocalOf<ReadingTitleUpperCasePreference> { ReadingTitleUpperCasePreference.default }

sealed class ReadingTitleUpperCasePreference(val value: Boolean) : Preference() {
    object ON : ReadingTitleUpperCasePreference(true)
    object OFF : ReadingTitleUpperCasePreference(false)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.readingTitleUpperCase,
                value
            )
        }
    }

    companion object {

        val default = OFF
        val values = listOf(ON, OFF)

        fun fromPreferences(preferences: Preferences) =
            when (preferences[DataStoreKey.keys[readingTitleUpperCase]?.key as Preferences.Key<Boolean>]) {
                true -> ON
                false -> OFF
                else -> default
            }
    }
}

operator fun ReadingTitleUpperCasePreference.not(): ReadingTitleUpperCasePreference =
    when (value) {
        true -> ReadingTitleUpperCasePreference.OFF
        false -> ReadingTitleUpperCasePreference.ON
    }
