package me.ash.reader.ui.page.home.reading.ai

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.ash.reader.infrastructure.ai.AiLanguage
import me.ash.reader.infrastructure.ai.AiSummaryService
import me.ash.reader.infrastructure.ai.AiSummaryStyle
import me.ash.reader.infrastructure.preference.LanguagesPreference
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.get
import me.ash.reader.ui.ext.languages
import me.ash.reader.ui.ext.put

sealed interface AiSummaryUiState {
    data object Idle : AiSummaryUiState
    data object SelectLanguagePrompt : AiSummaryUiState
    data object Loading : AiSummaryUiState
    data class Success(
        val result: String,
        val language: AiLanguage,
        val isRtl: Boolean,
    ) : AiSummaryUiState
    data class Error(val message: String) : AiSummaryUiState
}

@HiltViewModel
class AiSummaryViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val aiSummaryService: AiSummaryService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiSummaryUiState>(AiSummaryUiState.Idle)
    val uiState: StateFlow<AiSummaryUiState> = _uiState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        context.dataStore.get<String>(DataStoreKey.aiSummaryLanguage)
            ?.takeIf { it.isNotBlank() }
            ?.let { AiLanguage.fromName(it) }
            ?: AiLanguage.AUTO
    )
    val selectedLanguage: StateFlow<AiLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedStyle = MutableStateFlow(
        context.dataStore.get<String>(DataStoreKey.aiSummaryStyle)
            ?.takeIf { it.isNotBlank() }
            ?.let { AiSummaryStyle.fromName(it) }
            ?: AiSummaryStyle.KEY_POINTS
    )
    val selectedStyle: StateFlow<AiSummaryStyle> = _selectedStyle.asStateFlow()

    private var activeJob: Job? = null

    fun resolveEffectiveLanguage(language: AiLanguage): AiLanguage {
        if (language != AiLanguage.AUTO && language != AiLanguage.SELECT) return language
        return runCatching {
            val appPref = LanguagesPreference.fromValue(context.languages)
            val locale = appPref.toLocale() ?: Locale.getDefault()
            val code = locale.language.lowercase()
            AiLanguage.entries.firstOrNull {
                it != AiLanguage.AUTO && it != AiLanguage.SELECT && (it.code == code || it.code.startsWith(code))
            } ?: AiLanguage.ENGLISH
        }.getOrDefault(AiLanguage.ENGLISH)
    }

    fun initSummary(title: String, content: String) {
        if (_selectedLanguage.value == AiLanguage.SELECT) {
            _uiState.value = AiSummaryUiState.SelectLanguagePrompt
        } else {
            generateSummary(title, content)
        }
    }

    fun setLanguage(language: AiLanguage, title: String, content: String) {
        _selectedLanguage.value = language
        viewModelScope.launch {
            runCatching {
                context.dataStore.put(DataStoreKey.aiSummaryLanguage, language.name)
            }
        }
        if (language == AiLanguage.SELECT) {
            _uiState.value = AiSummaryUiState.SelectLanguagePrompt
        } else {
            generateSummary(title, content)
        }
    }

    fun setStyle(style: AiSummaryStyle, title: String, content: String) {
        _selectedStyle.value = style
        viewModelScope.launch {
            runCatching {
                context.dataStore.put(DataStoreKey.aiSummaryStyle, style.name)
            }
        }
        if (_selectedLanguage.value != AiLanguage.SELECT) {
            generateSummary(title, content)
        }
    }

    fun generateSummary(title: String, content: String) {
        val language = _selectedLanguage.value
        if (language == AiLanguage.SELECT) {
            _uiState.value = AiSummaryUiState.SelectLanguagePrompt
            return
        }

        activeJob?.cancel()
        _uiState.value = AiSummaryUiState.Loading

        activeJob = viewModelScope.launch {
            val style = _selectedStyle.value
            val effectiveLanguage = resolveEffectiveLanguage(language)
            aiSummaryService.summarize(
                title = title,
                htmlOrTextContent = content,
                language = if (language == AiLanguage.AUTO) language else effectiveLanguage,
                style = style,
            ).fold(
                onSuccess = { summary ->
                    _uiState.value = AiSummaryUiState.Success(
                        result = summary,
                        language = effectiveLanguage,
                        isRtl = effectiveLanguage.isRtl,
                    )
                },
                onFailure = { error ->
                    _uiState.value = AiSummaryUiState.Error(
                        message = error.localizedMessage ?: "Failed to generate summary",
                    )
                },
            )
        }
    }
}
