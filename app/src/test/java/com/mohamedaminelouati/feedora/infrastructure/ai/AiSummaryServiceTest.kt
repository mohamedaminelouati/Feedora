package com.mohamedaminelouati.feedora.infrastructure.ai

import kotlinx.coroutines.Dispatchers
import com.mohamedaminelouati.feedora.infrastructure.preference.SummarizerEnginePreference
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

        val duckAiEnglishDetailed = SummarizerEnginePreference.DuckAi.buildSummaryUrl(
            articleUrl = testUrl,
            language = AiLanguage.ENGLISH,
            style = AiSummaryStyle.DETAILED,
        )
        Assert.assertTrue(duckAiEnglishDetailed.contains("duckduckgo.com/?q="))

        val protonLumoUrl = SummarizerEnginePreference.ProtonLumo.buildSummaryUrl(
            articleUrl = testUrl,
            language = AiLanguage.FRENCH,
            style = AiSummaryStyle.KEY_POINTS,
        )
        Assert.assertTrue(protonLumoUrl.contains("lumo.proton.me/guest/?q="))
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
    fun testAiSummaryStylePrompt() {
        val promptFr = AiSummaryStyle.KEY_POINTS.toPrompt(AiLanguage.FRENCH)
        Assert.assertTrue(promptFr.contains("français"))

        val promptAr = AiSummaryStyle.TLDR.toPrompt(AiLanguage.ARABIC)
        Assert.assertTrue(promptAr.contains("العربية"))

        val promptEn = AiSummaryStyle.DETAILED.toPrompt(AiLanguage.ENGLISH)
        Assert.assertTrue(promptEn.contains("English"))
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
