package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.infrastructure.ai.AiLanguage
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.aiSummaryLanguage
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalSummarizerLanguage =
    compositionLocalOf<SummarizerLanguagePreference> { SummarizerLanguagePreference.default }

data class SummarizerLanguagePreference(val language: AiLanguage) : Preference() {
    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.aiSummaryLanguage,
                language.name,
            )
        }
    }

    @Stable
    fun toDesc(context: Context): String = language.displayName

    companion object {
        val default = SummarizerLanguagePreference(AiLanguage.AUTO)
        val values = listOf(AiLanguage.AUTO) + (AiLanguage.entries.filter { it != AiLanguage.AUTO && it != AiLanguage.SELECT }.sortedBy { it.displayName })

        fun fromPreferences(preferences: Preferences): SummarizerLanguagePreference {
            val name = preferences[DataStoreKey.keys[aiSummaryLanguage]?.key as Preferences.Key<String>]
            return SummarizerLanguagePreference(AiLanguage.fromName(name))
        }
    }
}
