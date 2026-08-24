package me.ash.reader.infrastructure.ai

import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.ash.reader.infrastructure.di.IODispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import timber.log.Timber

@Singleton
class AiSummaryService
@Inject
constructor(
    private val okHttpClient: OkHttpClient,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val client =
        okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun summarize(
        title: String,
        htmlOrTextContent: String,
        language: AiLanguage = AiLanguage.AUTO,
        style: AiSummaryStyle = AiSummaryStyle.KEY_POINTS,
        customEndpoint: String? = null,
        apiKey: String? = null,
        customModel: String? = null,
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val plainText = extractPlainText(htmlOrTextContent).take(12000)
            if (plainText.isBlank()) {
                throw IllegalArgumentException("Le contenu de l'article est vide.")
            }

            // 1. If user provided a custom endpoint or valid API Key
            if (!customEndpoint.isNullOrBlank() || !apiKey.isNullOrBlank()) {
                val prompt = buildPrompt(title, plainText.take(6000), language, style)
                return@runCatching when {
                    !customEndpoint.isNullOrBlank() -> {
                        callOpenAiCompatible(customEndpoint, apiKey, customModel ?: "llama-3.3-70b-versatile", prompt)
                    }
                    apiKey != null && (apiKey.startsWith("AIza") || apiKey.length == 39) -> {
                        callGemini(apiKey, prompt)
                    }
                    apiKey != null && apiKey.startsWith("gsk_") -> {
                        callOpenAiCompatible("https://api.groq.com/openai/v1/chat/completions", apiKey, "llama-3.3-70b-versatile", prompt)
                    }
                    else -> {
                        callOpenAiCompatible(customEndpoint ?: "https://api.groq.com/openai/v1/chat/completions", apiKey, "llama-3.3-70b-versatile", prompt)
                    }
                }
            }

            // 2. Default Zero-Key, 100% Free Online/Local Engine
            val baseSummary = TextRankSummarizer.summarize(title, plainText, style)
            if (language == AiLanguage.AUTO) {
                baseSummary
            } else {
                translateSummary(baseSummary, language.code)
            }
        }.onFailure {
            Timber.e(it, "Summarization failed")
        }
    }

    suspend fun translateFullArticle(
        htmlOrTextContent: String,
        language: AiLanguage,
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            if (language == AiLanguage.AUTO) {
                return@runCatching extractPlainText(htmlOrTextContent)
            }
            val doc = Jsoup.parse(htmlOrTextContent)
            val elements = doc.select("p, h1, h2, h3, h4, h5, h6, li, blockquote")
            val paragraphs = if (elements.isNotEmpty()) {
                elements.map { it.text().trim() }.filter { it.isNotBlank() }
            } else {
                doc.text().split("\n").map { it.trim() }.filter { it.isNotBlank() }
            }

            if (paragraphs.isEmpty()) {
                val plain = extractPlainText(htmlOrTextContent)
                if (plain.isBlank()) return@runCatching ""
                return@runCatching translateSummary(plain.take(5000), language.code)
            }

            val chunks = mutableListOf<String>()
            var currentChunk = StringBuilder()
            for (p in paragraphs) {
                if (currentChunk.length + p.length > 1500 && currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append("\n\n")
                }
                currentChunk.append(p)
            }
            if (currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toString())
            }

            val translatedChunks = chunks.map { chunk ->
                translateSummary(chunk, language.code)
            }

            translatedChunks.joinToString("\n\n")
        }.onFailure {
            Timber.e(it, "Translation failed")
        }
    }

    private fun extractPlainText(content: String): String {
        return runCatching {
            Jsoup.parse(content).text().trim()
        }.getOrDefault(content)
    }

    private fun translateSummary(text: String, targetLanguageCode: String): String {
        if (text.isBlank() || targetLanguageCode == "auto") return text

        return runCatching {
            val encodedQuery = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLanguageCode&dt=t&q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body.string()

            if (!response.isSuccessful || responseBody.isBlank()) {
                return@runCatching text
            }

            val jsonArray = JSONArray(responseBody)
            val sentencesArray = jsonArray.getJSONArray(0)
            val translatedText = StringBuilder()

            for (i in 0 until sentencesArray.length()) {
                val sentence = sentencesArray.getJSONArray(i)
                val part = sentence.optString(0)
                if (!part.isNullOrBlank()) {
                    translatedText.append(part)
                }
            }

            val result = translatedText.toString().trim()
            if (result.isNotBlank()) result else text
        }.getOrDefault(text)
    }

    private fun buildPrompt(
        title: String,
        content: String,
        language: AiLanguage,
        style: AiSummaryStyle,
    ): Pair<String, String> {
        val systemPrompt =
            """
            You are a professional, accurate, and concise multilingual article summarizer.
            Your task is to summarize the provided article faithfully, highlighting core insights without speculation.
            Format instructions:
            - Write the entire summary ${language.promptInstruction}.
            - Structure the summary ${style.promptInstruction}.
            - Do not include meta commentary or introductory filler. Start directly with the summary content.
            """.trimIndent()

        val userPrompt =
            """
            Title: $title
            
            Article Content:
            $content
            """.trimIndent()

        return Pair(systemPrompt, userPrompt)
    }

    private fun callOpenAiCompatible(
        endpoint: String,
        apiKey: String?,
        model: String,
        prompt: Pair<String, String>,
    ): String {
        val url =
            if (endpoint.endsWith("/chat/completions")) endpoint
            else endpoint.trimEnd('/') + "/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", model)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", prompt.first)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt.second)
                    })
                },
            )
            put("temperature", 0.3)
            put("max_tokens", 1000)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))

        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body.string()

        if (!response.isSuccessful) {
            val errorMsg = runCatching {
                JSONObject(responseBody).optJSONObject("error")?.optString("message")
            }.getOrNull() ?: "HTTP ${response.code}: ${response.message}"
            throw IOException(errorMsg)
        }

        val json = JSONObject(responseBody)
        val choices = json.getJSONArray("choices")
        if (choices.length() > 0) {
            val message = choices.getJSONObject(0).getJSONObject("message")
            return message.getString("content").trim()
        }

        throw IOException("No summary generated in response")
    }

    private fun callGemini(apiKey: String, prompt: Pair<String, String>): String {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val fullText = "${prompt.first}\n\n${prompt.second}"
        val jsonBody = JSONObject().apply {
            put(
                "contents",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put(
                            "parts",
                            JSONArray().apply {
                                put(JSONObject().apply { put("text", fullText) })
                            },
                        )
                    })
                },
            )
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body.string()

        if (!response.isSuccessful) {
            val errorMsg = runCatching {
                JSONObject(responseBody).optJSONObject("error")?.optString("message")
            }.getOrNull() ?: "HTTP ${response.code}: ${response.message}"
            throw IOException(errorMsg)
        }

        val json = JSONObject(responseBody)
        val candidates = json.getJSONArray("candidates")
        if (candidates.length() > 0) {
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text").trim()
            }
        }

        throw IOException("No summary returned by Gemini")
    }
}
