package me.ash.reader.ui.page.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SettingsHighlightManager {
    var highlightKey: String? by mutableStateOf(null)

    fun highlight(key: String?) {
        highlightKey = key
    }

    fun clear() {
        highlightKey = null
    }
}
