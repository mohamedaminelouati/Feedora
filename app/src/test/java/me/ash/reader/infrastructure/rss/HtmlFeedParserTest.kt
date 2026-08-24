package me.ash.reader.infrastructure.rss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlFeedParserTest {

    @Test
    fun testParseGnomeNewsArchiveHtml() {
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>GNOME News</title></head>
            <body>
                <h1>GNOME News Archive</h1>
                <article class="post">
                    <header>
                        <h2><a href="https://news.gnome.org/2026/03/gnome-49-released/">GNOME 49 Released</a></h2>
                        <time datetime="2026-03-15T16:45:00+00:00">March 15, 2026</time>
                    </header>
                    <p class="summary">The GNOME project is proud to announce the general availability of GNOME 49.</p>
                </article>
                <article class="post">
                    <header>
                        <h2><a href="https://news.gnome.org/2026/02/guadec-2026-call-for-papers/">GUADEC 2026 Call for Papers</a></h2>
                        <span class="post-date">February 20, 2026 at 10:30 AM</span>
                    </header>
                    <p class="summary">We invite proposals for talks and workshops for GUADEC 2026.</p>
                </article>
            </body>
            </html>
        """.trimIndent()

        val feed = HtmlFeedParser.parse("https://news.gnome.org/archives", sampleHtml.toByteArray())
        assertNotNull(feed)
        assertEquals(2, feed!!.entries.size)

        val entry1 = feed.entries[0]
        assertEquals("GNOME 49 Released", entry1.title)
        assertNotNull(entry1.publishedDate)

        val entry2 = feed.entries[1]
        assertEquals("GUADEC 2026 Call for Papers", entry2.title)
        assertNotNull(entry2.publishedDate)
    }
}
