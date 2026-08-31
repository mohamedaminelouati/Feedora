package com.mohamedaminelouati.feedora.infrastructure.rss.provider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mohamedaminelouati.feedora.infrastructure.rss.provider.feedbin.FeedbinDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbinProviderTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun testFeedbinSubscriptionsParsing() {
        val json = """
            [
                {
                    "id": 1,
                    "created_at": "2023-01-01T00:00:00.000000Z",
                    "feed_id": 50,
                    "title": "Feedbin Blog",
                    "feed_url": "https://feedbin.com/blog.xml",
                    "site_url": "https://feedbin.com"
                }
            ]
        """.trimIndent()
        val type = object : TypeToken<List<FeedbinDTO.Subscription>>() {}.type
        val subs: List<FeedbinDTO.Subscription> = gson.fromJson(json, type)
        assertEquals(1, subs.size)
        assertEquals(1L, subs[0].id)
        assertEquals(50L, subs[0].feedId)
        assertEquals("Feedbin Blog", subs[0].title)
    }

    @Test
    fun testFeedbinTaggingsParsing() {
        val json = """
            [
                {
                    "id": 10,
                    "feed_id": 50,
                    "name": "Design"
                }
            ]
        """.trimIndent()
        val type = object : TypeToken<List<FeedbinDTO.Tagging>>() {}.type
        val tags: List<FeedbinDTO.Tagging> = gson.fromJson(json, type)
        assertEquals(1, tags.size)
        assertEquals(10L, tags[0].id)
        assertEquals(50L, tags[0].feedId)
        assertEquals("Design", tags[0].name)
    }

    @Test
    fun testFeedbinEntriesParsing() {
        val json = """
            [
                {
                    "id": 200,
                    "feed_id": 50,
                    "title": "Welcome to Feedbin",
                    "author": "Ben",
                    "summary": "Short summary",
                    "content": "<p>Article body</p>",
                    "url": "https://feedbin.com/",
                    "published": "2023-01-01T12:00:00.000000Z"
                }
            ]
        """.trimIndent()
        val type = object : TypeToken<List<FeedbinDTO.Entry>>() {}.type
        val entries: List<FeedbinDTO.Entry> = gson.fromJson(json, type)
        assertEquals(1, entries.size)
        assertEquals(200L, entries[0].id)
        assertEquals("Welcome to Feedbin", entries[0].title)
        assertEquals("Ben", entries[0].author)
    }

    @Test
    fun testFeedbinUnreadUpdatesSerialization() {
        val update = FeedbinDTO.UnreadEntriesUpdate(unreadEntries = listOf(101L, 102L))
        val json = gson.toJson(update)
        assertTrue(json.contains("\"unread_entries\":[101,102]"))
    }
}
