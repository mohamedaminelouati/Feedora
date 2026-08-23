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

    @Test
    fun testSummarizeWithoutApiKeyWorks() {
        val sampleArticle = """
            Linux is an open-source operating system that powers the internet and most of modern cloud computing infrastructure.
            Developers and system administrators choose Linux for its high reliability, security, and exceptional performance.
            Major cloud providers including AWS, Google Cloud, and Microsoft Azure run primarily on Linux virtual machines.
            The open collaboration model allows continuous innovation across computing architectures worldwide.
        """.trimIndent()

        kotlinx.coroutines.runBlocking {
            // AUTO
            val autoResult = aiSummaryService.summarize("Linux in Cloud", sampleArticle, AiLanguage.AUTO, AiSummaryStyle.KEY_POINTS)
            Assert.assertTrue(autoResult.isSuccess)
            Assert.assertTrue(autoResult.getOrThrow().isNotBlank())

            // FRENCH translation
            val frResult = aiSummaryService.summarize("Linux in Cloud", sampleArticle, AiLanguage.FRENCH, AiSummaryStyle.KEY_POINTS)
            Assert.assertTrue(frResult.isSuccess)
            Assert.assertTrue(frResult.getOrThrow().isNotBlank())

            // ARABIC translation
            val arResult = aiSummaryService.summarize("Linux in Cloud", sampleArticle, AiLanguage.ARABIC, AiSummaryStyle.KEY_POINTS)
            Assert.assertTrue(arResult.isSuccess)
            Assert.assertTrue(arResult.getOrThrow().isNotBlank())
        }
    }

    @Test
    fun testSummarizeEmptyContentFails() {
        kotlinx.coroutines.runBlocking {
            val result = aiSummaryService.summarize("Title", "   ", AiLanguage.FRENCH, AiSummaryStyle.KEY_POINTS)
            Assert.assertTrue(result.isFailure)
        }
    }
}
