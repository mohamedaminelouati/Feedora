package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.infrastructure.ai.AiLanguage
import com.mohamedaminelouati.feedora.infrastructure.ai.AiSummaryStyle
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.summarizerEngine
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalSummarizerEngine =
    compositionLocalOf<SummarizerEnginePreference> { SummarizerEnginePreference.default }

sealed class SummarizerEnginePreference(val value: Int) : Preference() {
    data object ProtonLumo : SummarizerEnginePreference(0)
    data object DuckAi : SummarizerEnginePreference(1)
    data object Smry : SummarizerEnginePreference(2)
    data object Perplexity : SummarizerEnginePreference(3)
    data object ChatGPT : SummarizerEnginePreference(4)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.summarizerEngine,
                value,
            )
        }
    }

    @Stable
    fun toDesc(context: Context): String =
        when (this) {
            ProtonLumo -> "Proton Lumo"
            DuckAi -> "Duck.ai"
            Smry -> "Smry.ai"
            Perplexity -> "Perplexity AI"
            ChatGPT -> "ChatGPT"
        }

    fun buildSummaryUrl(
        articleUrl: String,
        language: AiLanguage = AiLanguage.AUTO,
        style: AiSummaryStyle = AiSummaryStyle.KEY_POINTS,
    ): String {
        val cleanUrl = articleUrl.trim()
        val prompt = style.toPrompt(language)
        val fullQuery = "$prompt $cleanUrl"

        return when (this) {
            ProtonLumo -> {
                val encoded = runCatching { URLEncoder.encode(fullQuery, "UTF-8") }.getOrDefault(fullQuery)
                "https://lumo.proton.me/guest/?q=$encoded"
            }
            DuckAi -> {
                val encoded = runCatching { URLEncoder.encode(fullQuery, "UTF-8") }.getOrDefault(fullQuery)
                "https://duckduckgo.com/?q=$encoded&ia=chat"
            }
            Smry -> {
                val noProtocol = cleanUrl.removePrefix("https://").removePrefix("http://")
                "https://smry.ai/$noProtocol"
            }
            Perplexity -> {
                val encoded = runCatching { URLEncoder.encode(fullQuery, "UTF-8") }.getOrDefault(fullQuery)
                "https://www.perplexity.ai/search?q=$encoded"
            }
            ChatGPT -> {
                val encoded = runCatching { URLEncoder.encode(fullQuery, "UTF-8") }.getOrDefault(fullQuery)
                "https://chatgpt.com/?q=$encoded"
            }
        }
    }

    companion object {
        val default: SummarizerEnginePreference = ProtonLumo
        val values = listOf(ProtonLumo, DuckAi, Smry, Perplexity, ChatGPT)

        fun fromPreferences(preferences: Preferences): SummarizerEnginePreference =
            when (preferences[DataStoreKey.keys[summarizerEngine]?.key as Preferences.Key<Int>]) {
                0 -> ProtonLumo
                1 -> DuckAi
                2 -> Smry
                3 -> Perplexity
                4 -> ChatGPT
                else -> default
            }
    }
}
