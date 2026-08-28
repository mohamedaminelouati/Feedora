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
    FRENCH("Français (French)", "en français", "fr"),
    ARABIC("العربية (Arabic)", "باللغة العربية", "ar", isRtl = true),
    ENGLISH("English", "in English", "en"),
    SPANISH("Español (Spanish)", "en español", "es"),
    GERMAN("Deutsch (German)", "auf Deutsch", "de"),
    ITALIAN("Italiano (Italian)", "in italiano", "it"),
    PORTUGUESE("Português (Portuguese)", "em português", "pt"),
    PORTUGUESE_BRAZIL("Português do Brasil", "em português do Brasil", "pt-BR"),
    RUSSIAN("Русский (Russian)", "на русском", "ru"),
    CHINESE_SIMPLIFIED("简体中文 (Chinese Simplified)", "用简体中文", "zh-CN"),
    CHINESE_TRADITIONAL("繁體中文 (Chinese Traditional)", "用繁體中文", "zh-TW"),
    JAPANESE("日本語 (Japanese)", "日本語で", "ja"),
    HINDI("हिन्दी (Hindi)", "हिन्दी में", "hi"),
    HEBREW("עברית (Hebrew)", "בעברית", "he", isRtl = true),
    PERSIAN("فارسی (Persian)", "به زبان فارسی", "fa", isRtl = true),
    TURKISH("Türkçe (Turkish)", "Türkçe olarak", "tr"),
    POLISH("Polski (Polish)", "po polsku", "pl"),
    DUTCH("Nederlands (Dutch)", "in het Nederlands", "nl"),
    UKRAINIAN("Українська (Ukrainian)", "українською", "uk"),
    CZECH("Čeština (Czech)", "v češtině", "cs"),
    SWEDISH("Svenska (Swedish)", "på svenska", "sv"),
    DANISH("Dansk (Danish)", "på dansk", "da"),
    NORWEGIAN_BOKMAL("Norsk Bokmål (Norwegian)", "på norsk", "nb"),
    INDONESIAN("Bahasa Indonesia (Indonesian)", "dalam bahasa Indonesia", "id"),
    VIETNAMESE("Tiếng Việt (Vietnamese)", "bằng tiếng Việt", "vi"),
    ROMANIAN("Română (Romanian)", "în română", "ro"),
    HUNGARIAN("Magyar (Hungarian)", "magyarul", "hu"),
    BULGARIAN("Български (Bulgarian)", "на български", "bg"),
    CATALAN("Català (Catalan)", "en català", "ca"),
    SLOVAK("Slovenčina (Slovak)", "v slovenčine", "sk"),
    SLOVENIAN("Slovenščina (Slovenian)", "v slovenščini", "sl"),
    SERBIAN("Српски (Serbian)", "на српском", "sr"),
    ESTONIAN("Eesti (Estonian)", "eesti keeles", "et"),
    GALICIAN("Galego (Galician)", "en galego", "gl"),
    BASQUE("Euskara (Basque)", "euskaraz", "eu"),
    FILIPINO("Filipino", "sa Filipino", "fil"),
    ESPERANTO("Esperanto", "en Esperanto", "eo"),
    KANNADA("ಕನ್ನಡ (Kannada)", "ಕನ್ನಡದಲ್ಲಿ", "kn"),
    TAMIL("தமிழ் (Tamil)", "தமிழில்", "ta");

    companion object {
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
            AiLanguage.ARABIC -> when (this) {
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
