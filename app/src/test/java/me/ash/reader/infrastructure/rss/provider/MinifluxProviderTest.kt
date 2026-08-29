package me.ash.reader.infrastructure.rss.provider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.ash.reader.infrastructure.rss.provider.miniflux.MinifluxDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinifluxProviderTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun testMinifluxUserParsing() {
        val json = """{"id": 1, "username": "admin", "is_admin": true}"""
        val user = gson.fromJson(json, MinifluxDTO.User::class.java)
        assertEquals(1L, user.id)
        assertEquals("admin", user.username)
        assertEquals(true, user.isAdmin)
    }

    @Test
    fun testMinifluxFeedsParsing() {
        val json = """
            [
                {
                    "id": 42,
                    "user_id": 1,
                    "feed_url": "https://example.com/feed.xml",
                    "site_url": "https://example.com",
                    "title": "Example Feed",
                    "category": {
                        "id": 5,
                        "title": "Tech"
                    }
                }
            ]
        """.trimIndent()
        val feeds = gson.fromJson(json, Array<MinifluxDTO.Feed>::class.java)
        assertEquals(1, feeds.size)
        assertEquals(42L, feeds[0].id)
        assertEquals("Example Feed", feeds[0].title)
        assertEquals("Tech", feeds[0].category?.title)
    }

    @Test
    fun testMinifluxEntriesParsing() {
        val json = """
            {
                "total": 1,
                "entries": [
                    {
                        "id": 101,
                        "user_id": 1,
                        "feed_id": 42,
                        "title": "Hello World",
                        "url": "https://example.com/post/1",
                        "status": "unread",
                        "starred": true,
                        "content": "<p>Test article content</p>",
                        "author": "Alice"
                    }
                ]
            }
        """.trimIndent()
        val entriesResp = gson.fromJson(json, MinifluxDTO.EntriesResponse::class.java)
        assertEquals(1, entriesResp.total)
        assertNotNull(entriesResp.entries)
        assertEquals(101L, entriesResp.entries!![0].id)
        assertEquals("Hello World", entriesResp.entries!![0].title)
        assertEquals("unread", entriesResp.entries!![0].status)
        assertTrue(entriesResp.entries!![0].starred == true)
    }
}
