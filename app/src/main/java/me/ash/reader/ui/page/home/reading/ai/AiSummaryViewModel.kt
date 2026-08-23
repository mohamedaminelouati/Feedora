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

sealed interface AiSummaryUiState {
    data object Idle : AiSummaryUiState
    data object Loading : AiSummaryUiState
    data class Success(val summary: String, val language: AiLanguage, val isRtl: Boolean) : AiSummaryUiState
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

    private val _selectedLanguage = MutableStateFlow(AiLanguage.AUTO)
    val selectedLanguage: StateFlow<AiLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedStyle = MutableStateFlow(AiSummaryStyle.KEY_POINTS)
    val selectedStyle: StateFlow<AiSummaryStyle> = _selectedStyle.asStateFlow()

    private var summaryJob: Job? = null

    fun setLanguage(language: AiLanguage) {
        _selectedLanguage.value = language
    }

    fun setStyle(style: AiSummaryStyle) {
        _selectedStyle.value = style
    }

    fun generateSummary(title: String, content: String) {
        summaryJob?.cancel()
        _uiState.value = AiSummaryUiState.Loading

        summaryJob = viewModelScope.launch {
            val language = _selectedLanguage.value
            val style = _selectedStyle.value
            aiSummaryService.summarize(
                title = title,
                htmlOrTextContent = content,
                language = language,
                style = style,
            ).fold(
                onSuccess = { summary ->
                    _uiState.value = AiSummaryUiState.Success(
                        summary = summary,
                        language = language,
                        isRtl = language.isRtl,
                    )
                },
                onFailure = { error ->
                    _uiState.value = AiSummaryUiState.Error(
                        message = error.localizedMessage ?: "Échec du résumé",
                    )
                },
            )
        }
    }

    fun reset() {
        summaryJob?.cancel()
        _uiState.value = AiSummaryUiState.Idle
    }
}
