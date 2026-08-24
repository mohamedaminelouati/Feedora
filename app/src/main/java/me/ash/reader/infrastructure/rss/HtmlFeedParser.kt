package me.ash.reader.infrastructure.rss

import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.feed.synd.SyndImageImpl
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object HtmlFeedParser {

    private val englishDateWithTimeRegex =
        Regex(
            """\b(?:(\d{1,2})\s+(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)|(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+(\d{1,2}))(?:,)?\s+(\d{4})(?:\s+(?:at|,|@)?\s*(\d{1,2}:\d{2}(?::\d{2})?(?:\s*(?:AM|PM|am|pm))?))?\b""",
            RegexOption.IGNORE_CASE,
        )

    private val frenchDateWithTimeRegex =
        Regex(
            """\b(\d{1,2})\s+(janvier|février|fevrier|mars|avril|mai|juin|juillet|août|aout|septembre|octobre|novembre|décembre|decembre|janv|févr|fevr|avr|juil|sept|oct|nov|déc|dec)\s+(\d{4})(?:\s+(?:à|a|,)?\s*(\d{1,2}(?:h|:)\d{2}(?::\d{2})?))?\b""",
            RegexOption.IGNORE_CASE,
        )

    private val isoDateRegex =
        Regex("""\b(\d{4}-\d{2}-\d{2}(?:[T ]\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?(?:Z|[+-]\d{2}:?\d{2})?)?)\b""")

    private val relativeTimeRegex =
        Regex("""\b(\d+)\s*(hour|hr|minute|min|sec|day|jour|heure|minute)s?\s*(ago|passé)?\b""", RegexOption.IGNORE_CASE)

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
            val list = articleElements.mapIndexedNotNull { index, el -> parseArticleElement(el, pageUrl, index) }
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
            ".article-card",
            ".news-list > li",
            ".posts-list > li",
        )

        for (selector in containerSelectors) {
            val elements = doc.select(selector)
            if (elements.size >= 2) {
                val list = elements.mapIndexedNotNull { index, el -> parseArticleElement(el, pageUrl, index) }
                if (list.size >= 2) return list
            }
        }

        return emptyList()
    }

    private fun parseArticleElement(el: Element, pageUrl: String, itemIndex: Int = 0): SyndEntry? {
        // Find title & link
        val linkElement =
            el.selectFirst("header a[href]")
                ?: el.selectFirst("h1 a[href], h2 a[href], h3 a[href], h4 a[href]")
                ?: el.selectFirst(".title a[href], .headline a[href], .entry-title a[href]")
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
        val articleDate = parseDate(el, itemIndex) ?: Date(System.currentTimeMillis() - (itemIndex * 60_000L))

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

    private fun parseDate(element: Element, itemIndex: Int = 0): Date? {
        // 1. Check time/datetime attributes across all date-related elements
        val dateElements = element.select("time, [datetime], [data-time], [data-timestamp], .date, .time, .post-date, .entry-date, .published, .meta, .byline, footer, header")
        for (dateEl in dateElements) {
            val attrValues = listOf(
                dateEl.attr("datetime"),
                dateEl.attr("data-time"),
                dateEl.attr("title"),
                dateEl.attr("aria-label"),
            )
            for (attr in attrValues) {
                if (attr.isNotBlank()) {
                    parseDateString(attr)?.let { return adjustDateIfMidnight(it, itemIndex) }
                }
            }

            // Check timestamp number in ms or seconds
            val dataTimestamp = dateEl.attr("data-timestamp").trim()
            if (dataTimestamp.matches(Regex("""^\d{10,13}$"""))) {
                val ts = dataTimestamp.toLongOrNull()
                if (ts != null) {
                    val ms = if (dataTimestamp.length == 10) ts * 1000L else ts
                    return Date(ms)
                }
            }

            val text = dateEl.text().trim()
            if (text.isNotBlank()) {
                parseDateFromText(text)?.let { return adjustDateIfMidnight(it, itemIndex) }
            }
        }

        // 2. Check full element text
        val elementText = element.text()
        parseDateFromText(elementText)?.let { return adjustDateIfMidnight(it, itemIndex) }

        return null
    }

    private fun parseDateFromText(text: String): Date? {
        // Relative time check ("2 hours ago", "il y a 30 minutes")
        val relMatch = relativeTimeRegex.find(text)
        if (relMatch != null) {
            val amount = relMatch.groupValues[1].toLongOrNull() ?: 1L
            val unit = relMatch.groupValues[2].lowercase()
            val millis = when {
                unit.startsWith("sec") -> amount * 1000L
                unit.startsWith("min") -> amount * 60 * 1000L
                unit.startsWith("hour") || unit.startsWith("hr") || unit.startsWith("heur") -> amount * 3600 * 1000L
                unit.startsWith("day") || unit.startsWith("jour") -> amount * 86400 * 1000L
                else -> amount * 60 * 1000L
            }
            return Date(System.currentTimeMillis() - millis)
        }

        // ISO format check: "2026-03-07T14:32:00Z"
        val isoMatch = isoDateRegex.find(text)
        if (isoMatch != null) {
            parseDateString(isoMatch.value)?.let { return it }
        }

        // English format: "March 7, 2026 at 3:45 PM" or "7 March 2026 14:30"
        val engMatch = englishDateWithTimeRegex.find(text)
        if (engMatch != null) {
            val fullMatch = engMatch.value.replace(Regex("""\s+at\s+|\s*,\s*"""), " ").trim()
            val formats = listOf(
                "d MMMM yyyy h:mm a",
                "d MMMM yyyy HH:mm:ss",
                "d MMMM yyyy HH:mm",
                "MMMM d yyyy h:mm a",
                "MMMM d yyyy HH:mm:ss",
                "MMMM d yyyy HH:mm",
                "d MMM yyyy h:mm a",
                "d MMM yyyy HH:mm:ss",
                "d MMM yyyy HH:mm",
                "MMM d yyyy h:mm a",
                "MMM d yyyy HH:mm",
                "d MMMM yyyy",
                "MMMM d yyyy",
                "d MMM yyyy",
                "MMM d yyyy",
            )
            for (fmt in formats) {
                parseDateWithFormat(fullMatch, fmt, Locale.ENGLISH)?.let { return it }
            }
        }

        // French format: "7 mars 2026 à 14h30"
        val frMatch = frenchDateWithTimeRegex.find(text)
        if (frMatch != null) {
            val cleanStr = frMatch.value
                .replace(" à ", " ")
                .replace(" a ", " ")
                .replace("h", ":")
                .replace(Regex("""\s*,\s*"""), " ")
                .trim()
            val frFormats = listOf(
                "d MMMM yyyy HH:mm:ss",
                "d MMMM yyyy HH:mm",
                "d MMM yyyy HH:mm",
                "d MMMM yyyy",
                "d MMM yyyy",
            )
            for (fmt in frFormats) {
                parseDateWithFormat(cleanStr, fmt, Locale.FRENCH)?.let { return it }
            }
        }

        return null
    }

    private fun parseDateString(dateStr: String): Date? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
        )
        for (pattern in formats) {
            parseDateWithFormat(dateStr.trim(), pattern, Locale.US)?.let { return it }
        }
        return null
    }

    private fun parseDateWithFormat(str: String, pattern: String, locale: Locale): Date? {
        return runCatching {
            val sdf = SimpleDateFormat(pattern, locale)
            if (pattern.endsWith("'Z'")) {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(str)
        }.getOrNull()
    }

    /**
     * If the parsed date only had a date part (hour, minute, second are all 0),
     * give it a natural monotonic time during that day based on item index,
     * so that it doesn't display as a flat "00:00" and preserves ordering.
     */
    private fun adjustDateIfMidnight(date: Date, itemIndex: Int): Date {
        val cal = Calendar.getInstance().apply { time = date }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)

        if (hour == 0 && minute == 0 && second == 0) {
            // Set time towards evening (20:00) minus itemIndex minutes
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, (59 - (itemIndex % 60)).coerceAtLeast(0))
            cal.set(Calendar.SECOND, 0)
            return cal.time
        }
        return date
    }
}
