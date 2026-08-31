package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.infrastructure.ai.AiSummaryStyle
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.aiSummaryStyle
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalSummarizerStyle =
    compositionLocalOf<SummarizerStylePreference> { SummarizerStylePreference.default }

sealed class SummarizerStylePreference(val style: AiSummaryStyle) : Preference() {
    data object KeyPoints : SummarizerStylePreference(AiSummaryStyle.KEY_POINTS)
    data object Tldr : SummarizerStylePreference(AiSummaryStyle.TLDR)
    data object Detailed : SummarizerStylePreference(AiSummaryStyle.DETAILED)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.aiSummaryStyle,
                style.name,
            )
        }
    }

    @Stable
    fun toDesc(context: Context): String =
        when (style) {
            AiSummaryStyle.KEY_POINTS -> context.getString(R.string.summary_style_key_points)
            AiSummaryStyle.TLDR -> context.getString(R.string.summary_style_tldr)
            AiSummaryStyle.DETAILED -> context.getString(R.string.summary_style_detailed)
        }

    companion object {
        val default: SummarizerStylePreference = KeyPoints
        val values = listOf(KeyPoints, Tldr, Detailed)

        fun fromPreferences(preferences: Preferences): SummarizerStylePreference =
            when (preferences[DataStoreKey.keys[aiSummaryStyle]?.key as Preferences.Key<String>]) {
                AiSummaryStyle.KEY_POINTS.name -> KeyPoints
                AiSummaryStyle.TLDR.name -> Tldr
                AiSummaryStyle.DETAILED.name -> Detailed
                else -> default
            }
    }
}
