package me.ash.reader.infrastructure.ai

import java.util.Locale
import kotlin.math.ln

object TextRankSummarizer {

    private val FRENCH_STOPWORDS = setOf(
        "alors", "au", "aucuns", "aussi", "autre", "avant", "avec", "avoir", "bon", "car", "ce",
        "cela", "ces", "ceux", "chaque", "ci", "comme", "comment", "dans", "des", "du", "dedans",
        "dehors", "depuis", "devrait", "doit", "donc", "dos", "droite", "début", "elle", "elles",
        "en", "encore", "essai", "est", "et", "eu", "fait", "faites", "fois", "font", "hors", "ici",
        "il", "ils", "je", "juste", "la", "le", "les", "leur", "là", "ma", "maintenant", "mais",
        "mes", "mine", "moins", "mon", "mot", "même", "ni", "nommés", "notre", "nous", "nouveaux",
        "ou", "où", "par", "parce", "pas", "peut", "peu", "plupart", "pour", "pourquoi", "quand",
        "que", "quel", "quelle", "quelles", "quels", "qui", "sa", "sans", "ses", "seulement",
        "si", "sien", "son", "sont", "sous", "soyez", "sur", "ta", "tandis", "tellement", "tels",
        "tes", "ton", "tous", "tout", "trop", "très", "tu", "voient", "vont", "votre", "vous",
        "vu", "ça", "étaient", "état", "étions", "été", "être"
    )

    private val ARABIC_STOPWORDS = setOf(
        "في", "من", "على", "إلى", "عن", "مع", "هذا", "هذه", "ذلك", "تلك", "هؤلاء", "التي", "الذي",
        "الذين", "اللاتي", "اللواتي", "أن", "إن", "كان", "كانت", "يكون", "تكون", "هو", "هي", "هم",
        "هن", "نحن", "أنا", "أنت", "أنتما", "أنتم", "أنتن", "قد", "لقد", "لم", "لن", "ما", "ماذا",
        "لماذا", "كيف", "أين", "متى", "كم", "أي", "كل", "جميع", "بعض", "غير", "سوى", "إلا", "لكن",
        "بيد", "حيث", "إذا", "لو", "لولا", "بين", "فوق", "تحت", "أمام", "خلف", "وراء", "يمين",
        "يسار", "دون", "حتى", "كما", "مثل", "نحو", "منذ", "خلال", "أثناء", "ضد", "معظم", "أكثر",
        "أقل", "جدا", "أيضا", "فقط", "كذلك", "ثم", "أو", "أم", "بل", "لا", "ليس", "ليست"
    )

    private val ENGLISH_STOPWORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
        "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between",
        "both", "but", "by", "can't", "cannot", "could", "couldn't", "did", "didn't", "do",
        "does", "doesn't", "doing", "don't", "down", "during", "each", "few", "for", "from",
        "further", "had", "hadn't", "has", "hasn't", "have", "haven't", "having", "he", "he'd",
        "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his",
        "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't",
        "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself",
        "no", "nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our",
        "ours", "ourselves", "out", "over", "own", "same", "shan't", "she", "she'd", "she'll",
        "she's", "should", "shouldn't", "so", "some", "such", "than", "that", "that's", "the",
        "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they",
        "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too",
        "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've",
        "were", "weren't", "what", "what's", "when", "when's", "where", "where's", "which",
        "while", "who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't",
        "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"
    )

    fun summarize(
        title: String,
        plainText: String,
        style: AiSummaryStyle = AiSummaryStyle.KEY_POINTS,
    ): String {
        val sentences = splitSentences(plainText)
        if (sentences.isEmpty()) return plainText.take(300)
        if (sentences.size <= 2) return sentences.joinToString("\n\n")

        val titleWords = tokenizeWords(title).toSet()
        val allTokens = sentences.flatMap { tokenizeWords(it) }
        val wordFreq = mutableMapOf<String, Int>()
        for (w in allTokens) {
            wordFreq[w] = (wordFreq[w] ?: 0) + 1
        }

        val totalWords = allTokens.size.toDouble().coerceAtLeast(1.0)
        val wordScores = wordFreq.mapValues { (_, count) ->
            val tf = count / totalWords
            val idf = ln(totalWords / (count + 1.0)) + 1.0
            tf * idf
        }

        // Score each sentence
        val scoredSentences = sentences.mapIndexed { index, sentence ->
            val words = tokenizeWords(sentence)
            var score = 0.0
            for (w in words) {
                val s = wordScores[w] ?: 0.0
                score += if (titleWords.contains(w)) s * 2.5 else s
            }

            // Length normalization
            if (words.isNotEmpty()) {
                score /= words.size.toDouble()
            }

            // Position bonus: earlier sentences carry more journalistic weight
            if (index == 0) score *= 2.0
            else if (index == 1) score *= 1.5
            else if (index < 4) score *= 1.2

            ScoredSentence(index, sentence, score)
        }

        val targetCount = when (style) {
            AiSummaryStyle.KEY_POINTS -> 4.coerceAtMost(sentences.size)
            AiSummaryStyle.TLDR -> 2.coerceAtMost(sentences.size)
            AiSummaryStyle.DETAILED -> 6.coerceAtMost(sentences.size)
        }

        // Select top sentences and preserve original narrative order
        val selected = scoredSentences
            .sortedByDescending { it.score }
            .take(targetCount)
            .sortedBy { it.index }

        return when (style) {
            AiSummaryStyle.KEY_POINTS -> {
                selected.joinToString("\n\n") { "• ${it.text.trim()}" }
            }
            AiSummaryStyle.TLDR -> {
                selected.joinToString(" ") { it.text.trim() }
            }
            AiSummaryStyle.DETAILED -> {
                selected.mapIndexed { idx, s ->
                    "${idx + 1}. ${s.text.trim()}"
                }.joinToString("\n\n")
            }
        }
    }

    private fun splitSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?؟\n])\\s+"))
            .map { it.trim() }
            .filter { it.length >= 25 && it.any { c -> c.isLetter() } }
    }

    private fun tokenizeWords(text: String): List<String> {
        return text.lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { w ->
                w.length >= 3 &&
                !FRENCH_STOPWORDS.contains(w) &&
                !ARABIC_STOPWORDS.contains(w) &&
                !ENGLISH_STOPWORDS.contains(w)
            }
    }

    private data class ScoredSentence(val index: Int, val text: String, val score: Double)
}
