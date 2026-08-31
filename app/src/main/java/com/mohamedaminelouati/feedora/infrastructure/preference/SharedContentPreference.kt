package com.mohamedaminelouati.feedora.infrastructure.preference

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey
import com.mohamedaminelouati.feedora.ui.ext.DataStoreKey.Companion.sharedContent
import com.mohamedaminelouati.feedora.ui.ext.dataStore
import com.mohamedaminelouati.feedora.ui.ext.orNotEmpty
import com.mohamedaminelouati.feedora.ui.ext.put

val LocalSharedContent = compositionLocalOf<SharedContentPreference> { SharedContentPreference.default }

sealed class SharedContentPreference(val value: Int) : Preference() {
    object OnlyLink : SharedContentPreference(0)
    object TitleAndLink : SharedContentPreference(1)

    override fun put(context: Context, scope: CoroutineScope) {
        scope.launch {
            context.dataStore.put(
                DataStoreKey.sharedContent,
                value
            )
        }
    }

    @Stable
    fun toDesc(context: Context): String =
        when (this) {
            OnlyLink -> context.getString(R.string.only_link)
            TitleAndLink -> context.getString(R.string.title_and_link)
        }

    fun share(context: Context, title: String?, link: String?) {
        when (this) {
            OnlyLink -> share(context, link.orEmpty())
            TitleAndLink -> share(context, title.orNotEmpty { it + "\n" } + link.orEmpty())
        }
    }

    private fun share(context: Context, content: String) {
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }, context.getString(R.string.share)))
    }

    companion object {

        val default = OnlyLink
        val values = listOf(OnlyLink, TitleAndLink)

        fun fromPreferences(preferences: Preferences): SharedContentPreference =
            when (preferences[DataStoreKey.keys[sharedContent]?.key as Preferences.Key<Int>]) {
                0 -> OnlyLink
                1 -> TitleAndLink
                else -> default
            }
    }
}
