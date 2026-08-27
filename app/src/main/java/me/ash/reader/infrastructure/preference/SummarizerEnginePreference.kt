package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.summarizerEngine
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalSummarizerEngine =
    compositionLocalOf<SummarizerEnginePreference> { SummarizerEnginePreference.default }

sealed class SummarizerEnginePreference(val value: Int) : Preference() {
    data object Smry : SummarizerEnginePreference(0)
    data object Kagi : SummarizerEnginePreference(1)
    data object Perplexity : SummarizerEnginePreference(2)
    data object ChatGPT : SummarizerEnginePreference(3)

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
            Smry -> "Smry.ai"
            Kagi -> "Kagi Summarizer"
            Perplexity -> "Perplexity AI"
            ChatGPT -> "ChatGPT Web"
        }

    fun buildSummaryUrl(articleUrl: String): String {
        val cleanUrl = articleUrl.trim()
        return when (this) {
            Smry -> {
                val noProtocol = cleanUrl.removePrefix("https://").removePrefix("http://")
                "https://smry.ai/$noProtocol"
            }
            Kagi -> {
                val encoded = runCatching { URLEncoder.encode(cleanUrl, "UTF-8") }.getOrDefault(cleanUrl)
                "https://kagi.com/summarizer?url=$encoded"
            }
            Perplexity -> {
                val query = runCatching { URLEncoder.encode("Summarize: $cleanUrl", "UTF-8") }.getOrDefault(cleanUrl)
                "https://www.perplexity.ai/search?q=$query"
            }
            ChatGPT -> {
                val query = runCatching { URLEncoder.encode("Summarize this article: $cleanUrl", "UTF-8") }.getOrDefault(cleanUrl)
                "https://chatgpt.com/?q=$query"
            }
        }
    }

    companion object {
        val default: SummarizerEnginePreference = Smry
        val values = listOf(Smry, Kagi, Perplexity, ChatGPT)

        fun fromPreferences(preferences: Preferences): SummarizerEnginePreference =
            when (preferences[DataStoreKey.keys[summarizerEngine]?.key as Preferences.Key<Int>]) {
                0 -> Smry
                1 -> Kagi
                2 -> Perplexity
                3 -> ChatGPT
                else -> default
            }
    }
}
