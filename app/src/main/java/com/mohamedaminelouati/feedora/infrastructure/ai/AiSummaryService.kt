package com.mohamedaminelouati.feedora.infrastructure.ai

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import com.mohamedaminelouati.feedora.infrastructure.di.IODispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup

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
}
