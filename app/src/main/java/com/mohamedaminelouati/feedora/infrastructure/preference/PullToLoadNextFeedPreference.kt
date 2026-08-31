package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.pullToLoadNextFeed
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

sealed class PullToLoadNextFeedPreference(val value: Int) : Preference() {
    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(pullToLoadNextFeed, value)
        }
    }

    @Composable
    abstract fun description(): String

    object None : PullToLoadNextFeedPreference(0) {
        @Composable
        override fun description(): String = stringResource(R.string.none)
    }

    object LoadNextFeed : PullToLoadNextFeedPreference(1) {
        @Composable
        override fun description(): String = stringResource(R.string.load_next_feed)
    }

    object MarkAsReadAndLoadNextFeed : PullToLoadNextFeedPreference(2) {
        @Composable
        override fun description(): String =
            stringResource(R.string.mark_all_as_read_and_load_next_feed)
    }

    companion object {
        val default = LoadNextFeed
        val values = arrayOf(
            None, LoadNextFeed, MarkAsReadAndLoadNextFeed
        )

        fun fromPreference(preference: Preferences): PullToLoadNextFeedPreference {
            val value =
                preference[DataStoreKey.keys[pullToLoadNextFeed]?.key as Preferences.Key<Int>]
            return when (value) {
                0 -> None
                1 -> LoadNextFeed
                2 -> MarkAsReadAndLoadNextFeed
                else -> default
            }
        }
    }
}