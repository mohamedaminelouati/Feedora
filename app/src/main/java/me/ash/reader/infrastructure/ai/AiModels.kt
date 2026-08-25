package me.ash.reader.infrastructure.ai

enum class AiLanguage(
    val displayName: String,
    val promptInstruction: String,
    val code: String,
    val isRtl: Boolean = false,
) {
    AUTO("Auto (Article Language)", "in the same language as the article", "auto"),
    ENGLISH("English", "in English", "en"),
    FRENCH("Français (French)", "in French", "fr"),
    ARABIC("العربية (Arabic)", "in Arabic", "ar", isRtl = true),
    ARABIC_NORTH_LEVANTINE("العربية الشمالية الشامية (North Levantine Arabic)", "in North Levantine Arabic", "apc", isRtl = true),
    GERMAN("Deutsch (German)", "in German", "de"),
    SPANISH("Español (Spanish)", "in Spanish", "es"),
    ITALIAN("Italiano (Italian)", "in Italian", "it"),
    PORTUGUESE("Português (Portuguese)", "in Portuguese", "pt"),
    PORTUGUESE_BRAZIL("Português do Brasil (Portuguese Brazil)", "in Brazilian Portuguese", "pt-BR"),
    RUSSIAN("Русский (Russian)", "in Russian", "ru"),
    CHINESE_SIMPLIFIED("简体中文 (Chinese Simplified)", "in Simplified Chinese", "zh-CN"),
    CHINESE_TRADITIONAL("繁體中文 (Chinese Traditional)", "in Traditional Chinese", "zh-TW"),
    JAPANESE("日本語 (Japanese)", "in Japanese", "ja"),
    HINDI("हिन्दी (Hindi)", "in Hindi", "hi"),
    HEBREW("עברית (Hebrew)", "in Hebrew", "he", isRtl = true),
    PERSIAN("فارسی (Persian)", "in Persian", "fa", isRtl = true),
    TURKISH("Türkçe (Turkish)", "in Turkish", "tr"),
    POLISH("Polski (Polish)", "in Polish", "pl"),
    DUTCH("Nederlands (Dutch)", "in Dutch", "nl"),
    UKRAINIAN("Українська (Ukrainian)", "in Ukrainian", "uk"),
    CZECH("Čeština (Czech)", "in Czech", "cs"),
    SWEDISH("Svenska (Swedish)", "in Swedish", "sv"),
    DANISH("Dansk (Danish)", "in Danish", "da"),
    NORWEGIAN_BOKMAL("Norsk Bokmål (Norwegian)", "in Norwegian", "nb"),
    INDONESIAN("Bahasa Indonesia (Indonesian)", "in Indonesian", "id"),
    VIETNAMESE("Tiếng Việt (Vietnamese)", "in Vietnamese", "vi"),
    ROMANIAN("Română (Romanian)", "in Romanian", "ro"),
    HUNGARIAN("Magyar (Hungarian)", "in Hungarian", "hu"),
    BULGARIAN("Български (Bulgarian)", "in Bulgarian", "bg"),
    CATALAN("Català (Catalan)", "in Catalan", "ca"),
    SLOVAK("Slovenčina (Slovak)", "in Slovak", "sk"),
    SLOVENIAN("Slovenščina (Slovenian)", "in Slovenian", "sl"),
    SERBIAN("Српски (Serbian)", "in Serbian", "sr"),
    ESTONIAN("Eesti (Estonian)", "in Estonian", "et"),
    GALICIAN("Galego (Galician)", "in Galician", "gl"),
    BASQUE("Euskara (Basque)", "in Basque", "eu"),
    FILIPINO("Filipino", "in Filipino", "fil"),
    ESPERANTO("Esperanto", "in Esperanto", "eo"),
    KANNADA("ಕನ್ನಡ (Kannada)", "in Kannada", "kn"),
    TAMIL("தமிழ் (Tamil)", "in Tamil", "ta");

    companion object {
        fun fromName(name: String?): AiLanguage {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}

enum class AiSummaryStyle(val displayName: String, val promptInstruction: String) {
    KEY_POINTS(
        "Key Points",
        "under 3 to 5 concise bullet points with key takeaways"
    ),
    TLDR(
        "TL;DR",
        "in a brief 2-sentence TL;DR summary"
    ),
    DETAILED(
        "Detailed",
        "in a structured, comprehensive summary with context, main points, and conclusions"
    );

    companion object {
        fun fromName(name: String?): AiSummaryStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: KEY_POINTS
        }
    }
}
