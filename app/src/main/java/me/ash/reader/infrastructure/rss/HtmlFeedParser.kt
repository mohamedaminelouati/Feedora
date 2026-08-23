package me.ash.reader.infrastructure.rss

import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.feed.synd.SyndImageImpl
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object HtmlFeedParser {

    private val englishDateRegex =
        Regex(
            """\b(\d{1,2})\s+(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{4})\b""",
            RegexOption.IGNORE_CASE,
        )

    private val isoDateRegex =
        Regex("""\b(\d{4}-\d{2}-\d{2}(?:[T ]\d{2}:\d{2}(?::\d{2})?)?)\b""")

    fun parse(pageUrl: String, bodyBytes: ByteArray, charset: Charset = Charsets.UTF_8): SyndFeed? {
        return runCatching {
            val html = String(bodyBytes, charset)
            val doc = Jsoup.parse(html, pageUrl)
            parseDocument(pageUrl, doc)
        }.getOrNull()
    }

    fun parseDocument(pageUrl: String, doc: Document): SyndFeed? {
        val entries = extractEntries(doc, pageUrl)
        if (entries.isEmpty()) return null

        val feedTitle = extractFeedTitle(doc, pageUrl)
        val feedDescription = extractFeedDescription(doc)
        val feedIcon = doc.selectFirst("link[rel~=(?i)icon]")?.absUrl("href")

        return SyndFeedImpl().apply {
            feedType = "rss_2.0"
            title = feedTitle
            link = pageUrl
            description = feedDescription
            publishedDate = entries.firstOrNull()?.publishedDate ?: Date()
            this.entries = entries

            if (!feedIcon.isNullOrBlank()) {
                image = SyndImageImpl().apply {
                    url = feedIcon
                    title = feedTitle
                    link = pageUrl
                }
            }
        }
    }

    private fun extractFeedTitle(doc: Document, pageUrl: String): String {
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
        if (!ogTitle.isNullOrBlank()) return ogTitle

        val h1 = doc.selectFirst("h1")?.text()?.trim()
        if (!h1.isNullOrBlank()) return h1

        val pageTitle = doc.title().trim()
        if (pageTitle.isNotBlank()) return pageTitle

        return runCatching { java.net.URI(pageUrl).path.trim('/').replace("/", " - ") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() } ?: pageUrl
    }

    private fun extractFeedDescription(doc: Document): String {
        return doc.selectFirst("meta[name=description]")?.attr("content")?.trim()
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
            ?: ""
    }

    private fun extractEntries(doc: Document, pageUrl: String): List<SyndEntry> {
        // 1. Check for semantic <article> tags
        val articleElements = doc.select("article")
        if (articleElements.isNotEmpty()) {
            val list = articleElements.mapNotNull { parseArticleElement(it, pageUrl) }
            if (list.isNotEmpty()) return list
        }

        // 2. Check for common article container classes
        val containerSelectors = listOf(
            ".post",
            ".news-item",
            ".entry",
            ".card-article",
            ".story",
            ".topic-item",
            ".blog-post",
            ".item-list > li",
            ".feed-item",
        )

        for (selector in containerSelectors) {
            val elements = doc.select(selector)
            if (elements.size >= 2) {
                val list = elements.mapNotNull { parseArticleElement(it, pageUrl) }
                if (list.size >= 2) return list
            }
        }

        return emptyList()
    }

    private fun parseArticleElement(el: Element, pageUrl: String): SyndEntry? {
        // Find title & link
        val linkElement =
            el.selectFirst("header a[href]")
                ?: el.selectFirst("h1 a[href], h2 a[href], h3 a[href], h4 a[href]")
                ?: el.selectFirst(".title a[href], .headline a[href]")
                ?: el.select("a[href]").maxByOrNull { it.text().length }
                ?: return null

        val articleUrl = linkElement.absUrl("href")
        if (articleUrl.isBlank() || (!articleUrl.startsWith("http://") && !articleUrl.startsWith("https://"))) {
            return null
        }

        val articleTitle = linkElement.text().trim().takeIf { it.isNotBlank() }
            ?: linkElement.attr("title").trim().takeIf { it.isNotBlank() }
            ?: return null

        // Description
        val descElement =
            el.selectFirst(".description, .summary, .content, .entry-content, p")
                ?: el.select("p").firstOrNull { it.text().trim().length > 20 }

        val descriptionText = descElement?.text()?.trim() ?: ""

        // Date
        val articleDate = parseDate(el) ?: Date()

        // Author
        val authorText =
            el.selectFirst("[rel=author], .author, .byline, .creator, .details")?.text()?.trim()

        return SyndEntryImpl().apply {
            title = articleTitle
            link = articleUrl
            uri = articleUrl
            publishedDate = articleDate
            author = authorText

            if (descriptionText.isNotBlank()) {
                description = SyndContentImpl().apply {
                    type = "text/plain"
                    value = descriptionText
                }
            }
        }
    }

    private fun parseDate(element: Element): Date? {
        // 1. <time datetime="...">
        val timeAttr = element.selectFirst("time[datetime]")?.attr("datetime")?.trim()
        if (!timeAttr.isNullOrBlank()) {
            parseDateString(timeAttr)?.let { return it }
        }

        val text = element.text()

        // 2. English date regex: "7 March 2026" or "7 Mar 2026"
        val engMatch = englishDateRegex.find(text)
        if (engMatch != null) {
            val dateStr = engMatch.value
            parseDateWithFormat(dateStr, "d MMMM yyyy", Locale.ENGLISH)?.let { return it }
            parseDateWithFormat(dateStr, "d MMM yyyy", Locale.ENGLISH)?.let { return it }
        }

        // 3. ISO format: "2026-03-07"
        val isoMatch = isoDateRegex.find(text)
        if (isoMatch != null) {
            val dateStr = isoMatch.value
            parseDateString(dateStr)?.let { return it }
        }

        return null
    }

    private fun parseDateString(dateStr: String): Date? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
        )
        for (pattern in formats) {
            parseDateWithFormat(dateStr, pattern, Locale.US)?.let { return it }
        }
        return null
    }

    private fun parseDateWithFormat(str: String, pattern: String, locale: Locale): Date? {
        return runCatching {
            SimpleDateFormat(pattern, locale).parse(str)
        }.getOrNull()
    }
}
