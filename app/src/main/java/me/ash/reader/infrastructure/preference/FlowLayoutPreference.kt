package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.DataStoreKey.Companion.flowLayout
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.put

val LocalFlowLayout =
    compositionLocalOf<FlowLayoutPreference> { FlowLayoutPreference.default }

sealed class FlowLayoutPreference(val value: Int) : Preference() {
    object LIST : FlowLayoutPreference(0)
    object GRID : FlowLayoutPreference(1)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.flowLayout,
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
        val default: FlowLayoutPreference get() = LIST
        val values get() = listOf(LIST, GRID)

        fun fromPreferences(preferences: Preferences): FlowLayoutPreference =
            when (preferences[DataStoreKey.keys[flowLayout]?.key as Preferences.Key<Int>]) {
                0 -> LIST
                1 -> GRID
                else -> default
            }
    }
}

operator fun FlowLayoutPreference.not(): FlowLayoutPreference =
    when (this) {
        FlowLayoutPreference.LIST -> FlowLayoutPreference.GRID
        FlowLayoutPreference.GRID -> FlowLayoutPreference.LIST
    }
