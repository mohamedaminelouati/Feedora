package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.infrastructure.ai.AiLanguage
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.aiSummaryLanguage
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

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
        val values = AiLanguage.entries.filter { it != AiLanguage.SELECT }.toList()

        fun fromPreferences(preferences: Preferences): SummarizerLanguagePreference {
            val name = preferences[DataStoreKey.keys[aiSummaryLanguage]?.key as Preferences.Key<String>]
            return SummarizerLanguagePreference(AiLanguage.fromName(name))
        }
    }
}
