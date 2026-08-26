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

    // Summary state
    private val _summaryUiState = MutableStateFlow<AiSummaryUiState>(AiSummaryUiState.Idle)
    val summaryUiState: StateFlow<AiSummaryUiState> = _summaryUiState.asStateFlow()

    private val _summaryLanguage = MutableStateFlow(AiLanguage.AUTO)
    val summaryLanguage: StateFlow<AiLanguage> = _summaryLanguage.asStateFlow()

    private val _summaryStyle = MutableStateFlow(AiSummaryStyle.KEY_POINTS)
    val summaryStyle: StateFlow<AiSummaryStyle> = _summaryStyle.asStateFlow()

    // Translation state
    private val _translationUiState = MutableStateFlow<AiSummaryUiState>(AiSummaryUiState.Idle)
    val translationUiState: StateFlow<AiSummaryUiState> = _translationUiState.asStateFlow()

    private val _translationLanguage = MutableStateFlow(AiLanguage.AUTO)
    val translationLanguage: StateFlow<AiLanguage> = _translationLanguage.asStateFlow()

    private var summaryJob: Job? = null
    private var translationJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching {
                val prefs = context.dataStore.data.first()
                val savedSummaryLang = prefs[stringPreferencesKey(DataStoreKey.aiSummaryLanguage)]
                if (!savedSummaryLang.isNullOrBlank()) {
                    _summaryLanguage.value = AiLanguage.fromName(savedSummaryLang)
                }
                val savedStyle = prefs[stringPreferencesKey(DataStoreKey.aiSummaryStyle)]
                if (!savedStyle.isNullOrBlank()) {
                    _summaryStyle.value = AiSummaryStyle.fromName(savedStyle)
                }
                val savedTransLang = prefs[stringPreferencesKey(DataStoreKey.aiTranslationLanguage)]
                if (!savedTransLang.isNullOrBlank()) {
                    _translationLanguage.value = AiLanguage.fromName(savedTransLang)
                }
            }
        }
    }

    fun resolveEffectiveLanguage(language: AiLanguage): AiLanguage {
        if (language != AiLanguage.AUTO && language != AiLanguage.SELECT) return language
        return runCatching {
            val appPref = LanguagesPreference.fromValue(context.languages)
            val locale = appPref.toLocale() ?: Locale.getDefault()
            val code = locale.language.lowercase()
            AiLanguage.entries.firstOrNull { it != AiLanguage.AUTO && it != AiLanguage.SELECT && (it.code == code || it.code.startsWith(code)) }
                ?: AiLanguage.ENGLISH
        }.getOrDefault(AiLanguage.ENGLISH)
    }

    // --- Summary actions ---

    fun initSummary(title: String, content: String) {
        if (_summaryLanguage.value == AiLanguage.SELECT) {
            _summaryUiState.value = AiSummaryUiState.SelectLanguagePrompt
        } else {
            generateSummary(title, content)
        }
    }

    fun setSummaryLanguage(language: AiLanguage, title: String, content: String) {
        _summaryLanguage.value = language
        viewModelScope.launch {
            runCatching {
                context.dataStore.put(DataStoreKey.aiSummaryLanguage, language.name)
            }
        }
        if (language == AiLanguage.SELECT) {
            _summaryUiState.value = AiSummaryUiState.SelectLanguagePrompt
        } else {
            generateSummary(title, content)
        }
    }

    fun setSummaryStyle(style: AiSummaryStyle, title: String, content: String) {
        _summaryStyle.value = style
        viewModelScope.launch {
            runCatching {
                context.dataStore.put(DataStoreKey.aiSummaryStyle, style.name)
            }
        }
        if (_summaryLanguage.value != AiLanguage.SELECT) {
            generateSummary(title, content)
        }
    }

    fun generateSummary(title: String, content: String) {
        val language = _summaryLanguage.value
        if (language == AiLanguage.SELECT) {
            _summaryUiState.value = AiSummaryUiState.SelectLanguagePrompt
            return
        }

        summaryJob?.cancel()
        _summaryUiState.value = AiSummaryUiState.Loading

        summaryJob = viewModelScope.launch {
            val style = _summaryStyle.value
            val effectiveLanguage = resolveEffectiveLanguage(language)
            aiSummaryService.summarize(
                title = title,
                htmlOrTextContent = content,
                language = if (language == AiLanguage.AUTO) language else effectiveLanguage,
                style = style,
            ).fold(
                onSuccess = { summary ->
                    _summaryUiState.value = AiSummaryUiState.Success(
                        result = summary,
                        language = effectiveLanguage,
                        isRtl = effectiveLanguage.isRtl,
                    )
                },
                onFailure = { error ->
                    _summaryUiState.value = AiSummaryUiState.Error(
                        message = error.localizedMessage ?: "Failed to generate summary",
                    )
                },
            )
        }
    }

    // --- Translation actions ---

    fun initTranslation(content: String) {
        if (_translationLanguage.value == AiLanguage.SELECT) {
            _translationUiState.value = AiSummaryUiState.SelectLanguagePrompt
        } else {
            translateArticle(content)
        }
    }

    fun setTranslationLanguage(language: AiLanguage, content: String) {
        _translationLanguage.value = language
        viewModelScope.launch {
            runCatching {
                context.dataStore.put(DataStoreKey.aiTranslationLanguage, language.name)
            }
        }
        if (language == AiLanguage.SELECT) {
            _translationUiState.value = AiSummaryUiState.SelectLanguagePrompt
        } else {
            translateArticle(content)
        }
    }

    fun translateArticle(content: String) {
        val language = _translationLanguage.value
        if (language == AiLanguage.SELECT) {
            _translationUiState.value = AiSummaryUiState.SelectLanguagePrompt
            return
        }

        translationJob?.cancel()
        _translationUiState.value = AiSummaryUiState.Loading

        translationJob = viewModelScope.launch {
            val effectiveLanguage = resolveEffectiveLanguage(language)
            aiSummaryService.translateFullArticle(
                htmlOrTextContent = content,
                language = effectiveLanguage,
            ).fold(
                onSuccess = { translated ->
                    _translationUiState.value = AiSummaryUiState.Success(
                        result = translated,
                        language = effectiveLanguage,
                        isRtl = effectiveLanguage.isRtl,
                    )
                },
                onFailure = { error ->
                    _translationUiState.value = AiSummaryUiState.Error(
                        message = error.localizedMessage ?: "Failed to translate article",
                    )
                },
            )
        }
    }
}
