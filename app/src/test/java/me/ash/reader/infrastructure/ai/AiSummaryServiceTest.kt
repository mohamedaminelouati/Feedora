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
    fun testAiSummaryStyleFromName() {
        Assert.assertEquals(AiSummaryStyle.KEY_POINTS, AiSummaryStyle.fromName("KEY_POINTS"))
        Assert.assertEquals(AiSummaryStyle.TLDR, AiSummaryStyle.fromName("tldr"))
        Assert.assertEquals(AiSummaryStyle.DETAILED, AiSummaryStyle.fromName("detailed"))
    }

    @Test
    fun testTextRankSummarizerFrench() {
        val sampleArticle = """
            Linux est un système d'exploitation libre et open source mondialement réputé pour sa robustesse.
            Il est utilisé sur la grande majorité des serveurs web, des supercalculateurs et des centres de données cloud.
            La sécurité et la flexibilité du noyau Linux en font le choix numéro un des ingénieurs en informatique.
            Les distributions comme Ubuntu, Fedora et Debian permettent aux utilisateurs d'adapter leur environnement de travail.
            Enfin, la communauté mondiale continue de développer des fonctionnalités innovantes pour le futur du cloud computing.
        """.trimIndent()

        val summary = TextRankSummarizer.summarize("Linux et le Cloud", sampleArticle, AiSummaryStyle.KEY_POINTS)
        Assert.assertTrue(summary.isNotBlank())
        Assert.assertTrue(summary.contains("•"))
    }
}
