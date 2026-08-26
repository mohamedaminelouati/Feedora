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
import me.ash.reader.infrastructure.preference.LanguagesPreference
import me.ash.reader.ui.ext.DataStoreKey
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.languages
import me.ash.reader.ui.ext.put

@HiltViewModel
class AiTranslationViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val aiSummaryService: AiSummaryService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiSummaryUiState>(AiSummaryUiState.Idle)
    val uiState: StateFlow<AiSummaryUiState> = _uiState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(AiLanguage.AUTO)
    val selectedLanguage: StateFlow<AiLanguage> = _selectedLanguage.asStateFlow()

    private var activeJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching {
                val prefs = context.dataStore.data.first()
                val savedTransLang = prefs[stringPreferencesKey(DataStoreKey.aiTranslationLanguage)]
                if (!savedTransLang.isNullOrBlank()) {
                    _selectedLanguage.value = AiLanguage.fromName(savedTransLang)
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
            AiLanguage.entries.firstOrNull {
                it != AiLanguage.AUTO && it != AiLanguage.SELECT && (it.code == code || it.code.startsWith(code))
            } ?: AiLanguage.ENGLISH
        }.getOrDefault(AiLanguage.ENGLISH)
    }

    fun initTranslation(content: String) {
        if (_selectedLanguage.value == AiLanguage.SELECT) {
            _uiState.value = AiSummaryUiState.SelectLanguagePrompt
        } else {
            translateArticle(content)
        }
    }

    fun setLanguage(language: AiLanguage, content: String) {
        _selectedLanguage.value = language
        viewModelScope.launch {
            runCatching {
                context.dataStore.put(DataStoreKey.aiTranslationLanguage, language.name)
            }
        }
        if (language == AiLanguage.SELECT) {
            _uiState.value = AiSummaryUiState.SelectLanguagePrompt
        } else {
            translateArticle(content)
        }
    }

    fun translateArticle(content: String) {
        val language = _selectedLanguage.value
        if (language == AiLanguage.SELECT) {
            _uiState.value = AiSummaryUiState.SelectLanguagePrompt
            return
        }

        activeJob?.cancel()
        _uiState.value = AiSummaryUiState.Loading

        activeJob = viewModelScope.launch {
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
