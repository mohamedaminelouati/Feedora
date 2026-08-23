package me.ash.reader.infrastructure.ai

enum class AiLanguage(
    val displayName: String,
    val promptInstruction: String,
    val code: String,
    val isRtl: Boolean = false,
) {
    AUTO("Auto", "in the same language as the article", "auto"),
    FRENCH("Français", "en langue française (Français)", "fr"),
    ARABIC("العربية", "باللغة العربية الفصحى", "ar", isRtl = true),
    ENGLISH("English", "in English", "en"),
    SPANISH("Español", "en español", "es"),
    GERMAN("Deutsch", "auf Deutsch", "de"),
    ITALIAN("Italiano", "in italiano", "it"),
    PORTUGUESE("Português", "em português", "pt"),
    RUSSIAN("Русский", "на русском языке", "ru"),
    CHINESE("中文", "用简体中文", "zh-CN");

    companion object {
        fun fromName(name: String?): AiLanguage {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}

enum class AiSummaryStyle(val displayName: String, val promptInstruction: String) {
    KEY_POINTS(
        "Points clés",
        "under 3 to 5 concise bullet points with key takeaways"
    ),
    TLDR(
        "Bref (TL;DR)",
        "in a brief 2-sentence TL;DR summary"
    ),
    DETAILED(
        "Détaillé",
        "in a structured, comprehensive summary with context, main points, and conclusions"
    );

    companion object {
        fun fromName(name: String?): AiSummaryStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: KEY_POINTS
        }
    }
}
