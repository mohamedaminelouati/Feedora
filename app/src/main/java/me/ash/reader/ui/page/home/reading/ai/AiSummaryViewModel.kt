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
import me.ash.reader.ui.ext.languages
import me.ash.reader.ui.ext.put

enum class AiFeatureMode(val title: String) {
    SUMMARY("Summary"),
    TRANSLATION("Full Translation"),
}

sealed interface AiSummaryUiState {
    data object Idle : AiSummaryUiState
    data object Loading : AiSummaryUiState
    data class Success(
        val result: String,
        val language: AiLanguage,
        val isRtl: Boolean,
        val mode: AiFeatureMode,
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

    private val _featureMode = MutableStateFlow(AiFeatureMode.SUMMARY)
    val featureMode: StateFlow<AiFeatureMode> = _featureMode.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AiLanguage.AUTO)
    val selectedLanguage: StateFlow<AiLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedStyle = MutableStateFlow(AiSummaryStyle.KEY_POINTS)
    val selectedStyle: StateFlow<AiSummaryStyle> = _selectedStyle.asStateFlow()

    private var activeJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching {
                val prefs = context.dataStore.data.first()
                val savedLangName = prefs[stringPreferencesKey(DataStoreKey.aiSummaryLanguage)]
                if (!savedLangName.isNullOrBlank()) {
                    _selectedLanguage.value = AiLanguage.fromName(savedLangName)
                }
                val savedStyleName = prefs[stringPreferencesKey(DataStoreKey.aiSummaryStyle)]
                if (!savedStyleName.isNullOrBlank()) {
                    _selectedStyle.value = AiSummaryStyle.fromName(savedStyleName)
                }
            }
        }
    }

    /**
     * Resolves the target language when AUTO is selected to match the application's active language.
     */
    fun resolveEffectiveLanguage(language: AiLanguage): AiLanguage {
        if (language != AiLanguage.AUTO) return language
        return runCatching {
            val appPref = LanguagesPreference.fromValue(context.languages)
            val locale = appPref.toLocale() ?: Locale.getDefault()
            val code = locale.language.lowercase()
            AiLanguage.values().firstOrNull { it != AiLanguage.AUTO && (it.code == code || it.code.startsWith(code)) }
                ?: AiLanguage.ENGLISH
        }.getOrDefault(AiLanguage.ENGLISH)
    }

    fun initMode(mode: AiFeatureMode, title: String, content: String) {
        _featureMode.value = mode
        executeAction(title, content)
    }

    fun setFeatureMode(mode: AiFeatureMode, title: String, content: String) {
        if (_featureMode.value != mode) {
            _featureMode.value = mode
            executeAction(title, content)
        }
    }

    fun setLanguage(language: AiLanguage, title: String, content: String) {
        _selectedLanguage.value = language
        viewModelScope.launch {
            runCatching {
                context.dataStore.put(DataStoreKey.aiSummaryLanguage, language.name)
            }
        }
        executeAction(title, content)
    }

    fun setStyle(style: AiSummaryStyle, title: String, content: String) {
        _selectedStyle.value = style
        viewModelScope.launch {
            runCatching {
                context.dataStore.put(DataStoreKey.aiSummaryStyle, style.name)
            }
        }
        if (_featureMode.value == AiFeatureMode.SUMMARY) {
            executeAction(title, content)
        }
    }

    fun executeAction(title: String, content: String) {
        activeJob?.cancel()
        _uiState.value = AiSummaryUiState.Loading

        activeJob = viewModelScope.launch {
            val language = _selectedLanguage.value
            val mode = _featureMode.value

            if (mode == AiFeatureMode.SUMMARY) {
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
                            mode = mode,
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = AiSummaryUiState.Error(
                            message = error.localizedMessage ?: "Failed to generate summary",
                        )
                    },
                )
            } else {
                // Full translation mode: resolve AUTO to application language
                val effectiveLanguage = resolveEffectiveLanguage(language)
                aiSummaryService.translateFullArticle(
                    htmlOrTextContent = content,
                    language = effectiveLanguage,
                ).fold(
                    onSuccess = { translated ->
                        _uiState.value = AiSummaryUiState.Success(
                            result = translated,
                            language = effectiveLanguage,
                            isRtl = effectiveLanguage.isRtl,
                            mode = mode,
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = AiSummaryUiState.Error(
                            message = error.localizedMessage ?: "Failed to translate article",
                        )
                    },
                )
            }
        }
    }

    fun reset() {
        activeJob?.cancel()
        _uiState.value = AiSummaryUiState.Idle
    }
}
