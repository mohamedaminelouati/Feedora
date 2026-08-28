package me.ash.reader.infrastructure.ai

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.ash.reader.R

enum class AiLanguage(
    val displayName: String,
    val promptInstruction: String,
    val code: String,
    val isRtl: Boolean = false,
) {
    SELECT("Select language…", "", "select"),
    AUTO("Auto (App Language)", "in the application's language", "auto"),
    ARABIC("العربية (Arabic)", "باللغة العربية", "ar", isRtl = true),
    ARABIC_NORTH_LEVANTINE("العربية الشامية (North Levantine Arabic)", "باللهجة الشامية", "apc", isRtl = true),
    BASQUE("Euskara (Basque)", "euskaraz", "eu"),
    BULGARIAN("Български (Bulgarian)", "на български", "bg"),
    CATALAN("Català (Catalan)", "en català", "ca"),
    CHINESE_SIMPLIFIED("简体中文 (Chinese Simplified)", "用简体中文", "zh-CN"),
    CHINESE_TRADITIONAL("繁體中文 (Chinese Traditional)", "用繁體中文", "zh-TW"),
    CZECH("Čeština (Czech)", "v češtině", "cs"),
    DANISH("Dansk (Danish)", "på dansk", "da"),
    DUTCH("Nederlands (Dutch)", "in het Nederlands", "nl"),
    ENGLISH("English", "in English", "en"),
    ESPERANTO("Esperanto", "en Esperanto", "eo"),
    ESTONIAN("Eesti (Estonian)", "eesti keeles", "et"),
    FILIPINO("Filipino", "sa Filipino", "fil"),
    FRENCH("Français (French)", "en français", "fr"),
    GALICIAN("Galego (Galician)", "en galego", "gl"),
    GERMAN("Deutsch (German)", "auf Deutsch", "de"),
    HEBREW("עברית (Hebrew)", "בעברית", "he", isRtl = true),
    HINDI("हिन्दी (Hindi)", "हिन्दी में", "hi"),
    HUNGARIAN("Magyar (Hungarian)", "magyarul", "hu"),
    INDONESIAN("Bahasa Indonesia (Indonesian)", "dalam bahasa Indonesia", "id"),
    ITALIAN("Italiano (Italian)", "in italiano", "it"),
    JAPANESE("日本語 (Japanese)", "日本語で", "ja"),
    KANNADA("ಕನ್ನಡ (Kannada)", "ಕನ್ನಡದಲ್ಲಿ", "kn"),
    NORWEGIAN_BOKMAL("Norsk Bokmål (Norwegian)", "på norsk", "nb"),
    PERSIAN("فارسی (Persian)", "به زبان فارسی", "fa", isRtl = true),
    POLISH("Polski (Polish)", "po polsku", "pl"),
    PORTUGUESE("Português (Portuguese)", "em português", "pt"),
    PORTUGUESE_BRAZIL("Português do Brasil", "em português do Brasil", "pt-BR"),
    ROMANIAN("Română (Romanian)", "în română", "ro"),
    RUSSIAN("Русский (Russian)", "на русском", "ru"),
    SERBIAN("Српски (Serbian)", "на српском", "sr"),
    SLOVAK("Slovenčina (Slovak)", "v slovenčine", "sk"),
    SLOVENIAN("Slovenščina (Slovenian)", "v slovenščini", "sl"),
    SPANISH("Español (Spanish)", "en español", "es"),
    SWEDISH("Svenska (Swedish)", "på svenska", "sv"),
    TAMIL("தமிழ் (Tamil)", "தமிழில்", "ta"),
    TURKISH("Türkçe (Turkish)", "Türkçe olarak", "tr"),
    UKRAINIAN("Українська (Ukrainian)", "українською", "uk"),
    VIETNAMESE("Tiếng Việt (Vietnamese)", "bằng tiếng Việt", "vi");

    companion object {
        val sortedEntries: List<AiLanguage> by lazy {
            listOf(SELECT, AUTO) + (entries.filter { it != SELECT && it != AUTO }.sortedBy { it.displayName })
        }

        fun fromName(name: String?): AiLanguage {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}

enum class AiSummaryStyle(val displayName: String) {
    KEY_POINTS("Key Points"),
    TLDR("TL;DR"),
    DETAILED("Detailed");

    fun toPrompt(language: AiLanguage): String {
        return when (language) {
            AiLanguage.ARABIC, AiLanguage.ARABIC_NORTH_LEVANTINE -> when (this) {
                KEY_POINTS -> "لخص هذا المقال باللغة العربية في شكل نقاط رئيسية موجزة ومفيدة:"
                TLDR -> "لخص هذا المقال باللغة العربية باختصار شديد في جملتين أو ثلاث (TL;DR):"
                DETAILED -> "قدم تلخيصاً مفصلاً وشاملاً لهذا المقال باللغة العربية يوضح جميع النقاط الأساسية:"
            }
            AiLanguage.FRENCH -> when (this) {
                KEY_POINTS -> "Résume cet article en français sous forme de points clés concis :"
                TLDR -> "Résume cet article en français sous forme d'un résumé court en 2 ou 3 phrases (TL;DR) :"
                DETAILED -> "Fais un résumé détaillé, complet et structuré de cet article en français :"
            }
            AiLanguage.ENGLISH -> when (this) {
                KEY_POINTS -> "Summarize this article in English as concise key bullet points:"
                TLDR -> "Summarize this article in English as a brief 2-3 sentence TL;DR:"
                DETAILED -> "Provide a comprehensive, detailed, and well-structured summary of this article in English:"
            }
            else -> when (this) {
                KEY_POINTS -> "Summarize this article ${language.promptInstruction} as concise key bullet points:"
                TLDR -> "Summarize this article ${language.promptInstruction} as a short 2-3 sentence TL;DR:"
                DETAILED -> "Provide a comprehensive and detailed summary of this article ${language.promptInstruction}:"
            }
        }
    }

    @Composable
    fun toDisplayName(): String = when (this) {
        KEY_POINTS -> stringResource(R.string.summary_style_key_points)
        TLDR -> stringResource(R.string.summary_style_tldr)
        DETAILED -> stringResource(R.string.summary_style_detailed)
    }

    companion object {
        fun fromName(name: String?): AiSummaryStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: KEY_POINTS
        }
    }
}
