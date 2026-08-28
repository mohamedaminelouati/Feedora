package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.infrastructure.ai.AiLanguage
import me.ash.reader.infrastructure.ai.AiSummaryStyle
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.summarizerEngine
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalSummarizerEngine =
    compositionLocalOf<SummarizerEnginePreference> { SummarizerEnginePreference.default }

sealed class SummarizerEnginePreference(val value: Int) : Preference() {
    data object ChatGPT : SummarizerEnginePreference(0)
    data object Perplexity : SummarizerEnginePreference(1)
    data object Claude : SummarizerEnginePreference(2)
    data object Smry : SummarizerEnginePreference(3)
    data object Kagi : SummarizerEnginePreference(4)

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
            ChatGPT -> "ChatGPT Web"
            Perplexity -> "Perplexity AI"
            Claude -> "Claude Web"
            Smry -> "Smry.ai"
            Kagi -> "Kagi Summarizer"
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
            ChatGPT -> {
                val encoded = runCatching { URLEncoder.encode(fullQuery, "UTF-8") }.getOrDefault(fullQuery)
                "https://chatgpt.com/?q=$encoded"
            }
            Perplexity -> {
                val encoded = runCatching { URLEncoder.encode(fullQuery, "UTF-8") }.getOrDefault(fullQuery)
                "https://www.perplexity.ai/search?q=$encoded"
            }
            Claude -> {
                val encoded = runCatching { URLEncoder.encode(fullQuery, "UTF-8") }.getOrDefault(fullQuery)
                "https://claude.ai/new?q=$encoded"
            }
            Smry -> {
                val noProtocol = cleanUrl.removePrefix("https://").removePrefix("http://")
                "https://smry.ai/$noProtocol"
            }
            Kagi -> {
                val encoded = runCatching { URLEncoder.encode(cleanUrl, "UTF-8") }.getOrDefault(cleanUrl)
                "https://kagi.com/summarizer?url=$encoded"
            }
        }
    }

    companion object {
        val default: SummarizerEnginePreference = ChatGPT
        val values = listOf(ChatGPT, Perplexity, Claude, Smry, Kagi)

        fun fromPreferences(preferences: Preferences): SummarizerEnginePreference =
            when (preferences[DataStoreKey.keys[summarizerEngine]?.key as Preferences.Key<Int>]) {
                0 -> ChatGPT
                1 -> Perplexity
                2 -> Claude
                3 -> Smry
                4 -> Kagi
                else -> default
            }
    }
}
