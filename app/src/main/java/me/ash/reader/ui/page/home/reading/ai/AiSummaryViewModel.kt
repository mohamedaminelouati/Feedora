package me.ash.reader.ui.page.home.reading.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ash.reader.infrastructure.ai.AiLanguage
import me.ash.reader.infrastructure.ai.AiSummaryService
import me.ash.reader.infrastructure.ai.AiSummaryStyle

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

    fun setFeatureMode(mode: AiFeatureMode, title: String, content: String) {
        if (_featureMode.value != mode) {
            _featureMode.value = mode
            executeAction(title, content)
        }
    }

    fun setLanguage(language: AiLanguage, title: String, content: String) {
        _selectedLanguage.value = language
        executeAction(title, content)
    }

    fun setStyle(style: AiSummaryStyle, title: String, content: String) {
        _selectedStyle.value = style
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
                aiSummaryService.summarize(
                    title = title,
                    htmlOrTextContent = content,
                    language = language,
                    style = style,
                ).fold(
                    onSuccess = { summary ->
                        _uiState.value = AiSummaryUiState.Success(
                            result = summary,
                            language = language,
                            isRtl = language.isRtl,
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
                // Full translation mode: default to English if AUTO was selected
                val targetLang = if (language == AiLanguage.AUTO) AiLanguage.ENGLISH else language
                aiSummaryService.translateFullArticle(
                    htmlOrTextContent = content,
                    language = targetLang,
                ).fold(
                    onSuccess = { translated ->
                        _uiState.value = AiSummaryUiState.Success(
                            result = translated,
                            language = targetLang,
                            isRtl = targetLang.isRtl,
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
