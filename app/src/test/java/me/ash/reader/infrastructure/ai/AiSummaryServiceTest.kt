package me.ash.reader.infrastructure.ai

import kotlinx.coroutines.Dispatchers
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
    fun testAiLanguageProperties() {
        Assert.assertTrue(AiLanguage.ARABIC.isRtl)
        Assert.assertFalse(AiLanguage.FRENCH.isRtl)
        Assert.assertFalse(AiLanguage.ENGLISH.isRtl)
        Assert.assertEquals("Français", AiLanguage.FRENCH.displayName)
        Assert.assertEquals("العربية", AiLanguage.ARABIC.displayName)
    }

    @Test
    fun testAiLanguageFromName() {
        Assert.assertEquals(AiLanguage.FRENCH, AiLanguage.fromName("FRENCH"))
        Assert.assertEquals(AiLanguage.ARABIC, AiLanguage.fromName("arabic"))
        Assert.assertEquals(AiLanguage.AUTO, AiLanguage.fromName("unknown"))
    }

    @Test
    fun testAiSummaryStyleFromName() {
        Assert.assertEquals(AiSummaryStyle.KEY_POINTS, AiSummaryStyle.fromName("KEY_POINTS"))
        Assert.assertEquals(AiSummaryStyle.TLDR, AiSummaryStyle.fromName("tldr"))
        Assert.assertEquals(AiSummaryStyle.DETAILED, AiSummaryStyle.fromName("detailed"))
    }

    @Test
    fun testSummarizeEmptyContentFails() {
        kotlinx.coroutines.runBlocking {
            val result = aiSummaryService.summarize("Title", "   ", AiLanguage.FRENCH, AiSummaryStyle.KEY_POINTS)
            Assert.assertTrue(result.isFailure)
        }
    }
}
