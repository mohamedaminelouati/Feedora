package me.ash.reader.infrastructure.ai

import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.ash.reader.infrastructure.di.IODispatcher
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import timber.log.Timber

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@Singleton
class AiSummaryService
@Inject
constructor(
    private val okHttpClient: OkHttpClient,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val client =
        okHttpClient.newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
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
            if (language == AiLanguage.SELECT) {
                throw IllegalArgumentException("Please select a language.")
            }
            val plainText = extractPlainText(htmlOrTextContent).take(15000)
            if (plainText.isBlank()) {
                throw IllegalArgumentException("Article content is empty.")
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

            // 2. Default Zero-Key Local + Multilingual Engine
            val baseSummary = TextRankSummarizer.summarize(title, plainText, style)
            if (language == AiLanguage.AUTO) {
                baseSummary
            } else {
                translateText(baseSummary, language.code)
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
            if (language == AiLanguage.SELECT) {
                throw IllegalArgumentException("Please select a language.")
            }
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
                return@runCatching translateText(plain.take(5000), language.code)
            }

            val chunks = mutableListOf<String>()
            var currentChunk = StringBuilder()
            for (p in paragraphs) {
                if (currentChunk.length + p.length > 800 && currentChunk.isNotEmpty()) {
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

            val deferredTranslations = chunks.map { chunk ->
                async(ioDispatcher) {
                    translateText(chunk, language.code)
                }
            }

            deferredTranslations.awaitAll().joinToString("\n\n")
        }.onFailure {
            Timber.e(it, "Translation failed")
        }
    }

    private fun extractPlainText(content: String): String {
        return runCatching {
            Jsoup.parse(content).text().trim()
        }.getOrDefault(content)
    }

    private fun translateText(text: String, targetLanguageCode: String): String {
        if (text.isBlank() || targetLanguageCode == "auto" || targetLanguageCode == "select") return text

        // Primary: Google Mobile Web Translate (High Accuracy, No Captcha Block)
        val primaryResult = runCatching {
            val encodedQuery = java.net.URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.google.com/m?sl=auto&tl=$targetLanguageCode&q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body.string()

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val doc = Jsoup.parse(responseBody)
                val result = doc.select("div.result-container").text().trim()
                if (result.isNotBlank()) {
                    return@runCatching result
                }
            }
            null
        }.getOrNull()

        if (!primaryResult.isNullOrBlank()) {
            return primaryResult
        }

        // Secondary fallback: MyMemory API
        val secondaryResult = runCatching {
            val encodedQuery = java.net.URLEncoder.encode(text, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=auto|$targetLanguageCode"

            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body.string()

            if (response.isSuccessful && responseBody.isNotBlank()) {
                val json = JSONObject(responseBody)
                val responseData = json.optJSONObject("responseData")
                val translated = responseData?.optString("translatedText")
                if (!translated.isNullOrBlank() && !translated.contains("MYMEMORY WARNING")) {
                    return@runCatching translated.trim()
                }
            }
            null
        }.getOrNull()

        return secondaryResult ?: text
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
        throw IOException("No content generated by model.")
    }

    private fun callGemini(apiKey: String, prompt: Pair<String, String>): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val contents = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "${prompt.first}\n\n${prompt.second}")
                    })
                })
            })
        }

        val jsonBody = JSONObject().apply {
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("maxOutputTokens", 1000)
            })
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
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                return parts.getJSONObject(0).getString("text").trim()
            }
        }
        throw IOException("No summary generated from Gemini.")
    }
}
