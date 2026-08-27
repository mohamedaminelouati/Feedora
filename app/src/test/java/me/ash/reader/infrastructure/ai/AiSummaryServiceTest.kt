package me.ash.reader.infrastructure.ai

import kotlinx.coroutines.Dispatchers
import me.ash.reader.infrastructure.preference.SummarizerEnginePreference
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AiSummaryServiceTest {

    private lateinit var aiSummaryService: AiSummaryService

    @Before
    fun setUp() {
        val okHttpClient = OkHttpClient.Builder().build()
        aiSummaryService = AiSummaryService(okHttpClient, Dispatchers.IO)
    }

    @Test
    fun testSummarizerEngineUrls() {
        val testUrl = "https://example.com/article/123"

        val smryUrl = SummarizerEnginePreference.Smry.buildSummaryUrl(testUrl)
        Assert.assertEquals("https://smry.ai/example.com/article/123", smryUrl)

        val kagiUrl = SummarizerEnginePreference.Kagi.buildSummaryUrl(testUrl)
        Assert.assertTrue(kagiUrl.contains("kagi.com/summarizer?url="))

        val perplexityUrl = SummarizerEnginePreference.Perplexity.buildSummaryUrl(testUrl)
        Assert.assertTrue(perplexityUrl.contains("perplexity.ai/search?q="))

        val chatGptUrl = SummarizerEnginePreference.ChatGPT.buildSummaryUrl(testUrl)
        Assert.assertTrue(chatGptUrl.contains("chatgpt.com/?q="))
    }

    @Test
    fun testAiLanguageProperties() {
        Assert.assertTrue(AiLanguage.ARABIC.isRtl)
        Assert.assertFalse(AiLanguage.FRENCH.isRtl)
        Assert.assertFalse(AiLanguage.ENGLISH.isRtl)
        Assert.assertTrue(AiLanguage.FRENCH.displayName.contains("Français"))
        Assert.assertTrue(AiLanguage.ARABIC.displayName.contains("العربية"))
        Assert.assertEquals("ar", AiLanguage.ARABIC.code)
        Assert.assertEquals("fr", AiLanguage.FRENCH.code)
    }

    @Test
    fun testAiLanguageFromName() {
        Assert.assertEquals(AiLanguage.FRENCH, AiLanguage.fromName("FRENCH"))
        Assert.assertEquals(AiLanguage.ARABIC, AiLanguage.fromName("arabic"))
        Assert.assertEquals(AiLanguage.AUTO, AiLanguage.fromName("unknown"))
    }

    @Test
    fun testTranslateFullArticleAuto() {
        kotlinx.coroutines.runBlocking {
            val result = aiSummaryService.translateFullArticle("<p>Hello World</p>", AiLanguage.AUTO)
            Assert.assertTrue(result.isSuccess)
            Assert.assertEquals("Hello World", result.getOrNull()?.trim())
        }
    }
}
