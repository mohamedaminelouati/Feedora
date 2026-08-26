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
    fun testTextRankSummarizerArabic() {
        val sampleArticle = """
            يعتبر نظام لينكس من أكثر أنظمة التشغيل أمانا واعتمادية في العالم الحديث.
            تعتمد عليه معظم الخوادم العملاقة ومراكز البيانات العالمية لتشغيل الحوسبة السحابية.
            تتميز برمجيات المصدر المفتوح بإمكانية التطوير المستمر والتعاون بين المبرمجين حول العالم.
            توفر توزيعات لينكس المختلفة بيئة عمل متكاملة للمطورين والمستخدمين على حد سواء.
            إن المستقبل يحمل تطورات هائلة في مجال البرمجيات الحرة ودعم الذكاء الاصطناعي.
        """.trimIndent()

        val summary = TextRankSummarizer.summarize("لينكس والحوسبة السحابية", sampleArticle, AiSummaryStyle.KEY_POINTS)
        Assert.assertTrue(summary.isNotBlank())
        Assert.assertTrue(summary.contains("•"))
    }

    @Test
    fun testTextRankSummarizerStyles() {
        val sampleArticle = """
            Android is a mobile operating system based on a modified version of the Linux kernel.
            It is designed primarily for touchscreen mobile devices such as smartphones and tablets.
            Android has been the best-selling OS worldwide on smartphones since 2011 and on tablets since 2013.
            The source code has been used to develop variants of Android on a range of other electronics.
            Google develops Android, which is free and open-source software with a massive ecosystem of apps.
        """.trimIndent()

        val keyPoints = TextRankSummarizer.summarize("Android OS", sampleArticle, AiSummaryStyle.KEY_POINTS)
        val tldr = TextRankSummarizer.summarize("Android OS", sampleArticle, AiSummaryStyle.TLDR)
        val detailed = TextRankSummarizer.summarize("Android OS", sampleArticle, AiSummaryStyle.DETAILED)

        Assert.assertTrue(keyPoints.contains("•"))
        Assert.assertFalse(tldr.contains("•"))
        Assert.assertTrue(detailed.contains("1."))
    }

    @Test
    fun testSummarizeEmptyContent() {
        kotlinx.coroutines.runBlocking {
            val result = aiSummaryService.summarize("Title", "")
            Assert.assertTrue(result.isFailure)
        }
    }

    @Test
    fun testSummarizeSelectLanguage() {
        kotlinx.coroutines.runBlocking {
            val result = aiSummaryService.summarize("Title", "Some valid content for the article", AiLanguage.SELECT)
            Assert.assertTrue(result.isFailure)
        }
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
