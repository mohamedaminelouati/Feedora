package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.feedsLayout
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalFeedsLayout =
    compositionLocalOf<FeedsLayoutPreference> { FeedsLayoutPreference.default }

sealed class FeedsLayoutPreference(val value: Int) : Preference() {
    object LIST : FeedsLayoutPreference(0)
    object GRID : FeedsLayoutPreference(1)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.feedsLayout,
                value
            )
        }
    }

    fun toDesc(context: Context): String =
        when (this) {
            LIST -> context.getString(R.string.layout_list)
            GRID -> context.getString(R.string.layout_grid)
        }

    fun isGrid(): Boolean = this == GRID

    companion object {
        val default: FeedsLayoutPreference get() = LIST
        val values get() = listOf(LIST, GRID)

        fun fromPreferences(preferences: Preferences): FeedsLayoutPreference =
            when (preferences[DataStoreKey.keys[feedsLayout]?.key as Preferences.Key<Int>]) {
                0 -> LIST
                1 -> GRID
                else -> default
            }
    }
}

operator fun FeedsLayoutPreference.not(): FeedsLayoutPreference =
    when (this) {
        FeedsLayoutPreference.LIST -> FeedsLayoutPreference.GRID
        FeedsLayoutPreference.GRID -> FeedsLayoutPreference.LIST
    }
