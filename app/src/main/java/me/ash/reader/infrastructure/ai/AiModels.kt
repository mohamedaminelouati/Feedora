package me.ash.reader.infrastructure.ai

enum class AiLanguage(val displayName: String, val promptInstruction: String, val isRtl: Boolean = false) {
    AUTO("Auto", "in the same language as the article"),
    FRENCH("Français", "en langue française (Français)"),
    ARABIC("العربية", "باللغة العربية الفصحى", isRtl = true),
    ENGLISH("English", "in English"),
    SPANISH("Español", "en español"),
    GERMAN("Deutsch", "auf Deutsch"),
    ITALIAN("Italiano", "in italiano"),
    PORTUGUESE("Português", "em português"),
    RUSSIAN("Русский", "на русском языке"),
    CHINESE("中文", "用简体中文");

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
