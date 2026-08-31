package com.mohamedaminelouati.feedora.infrastructure.rss.provider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mohamedaminelouati.feedora.infrastructure.rss.provider.feedly.FeedlyDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedlyProviderTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun testFeedlyProfileParsing() {
        val json = """
            {
                "id": "c08v7-feedly-user",
                "email": "user@feedly.com",
                "givenName": "Feedly",
                "familyName": "User"
            }
        """.trimIndent()
        val profile = gson.fromJson(json, FeedlyDTO.Profile::class.java)
        assertEquals("c08v7-feedly-user", profile.id)
        assertEquals("user@feedly.com", profile.email)
        assertEquals("Feedly", profile.givenName)
    }

    @Test
    fun testFeedlySubscriptionsParsing() {
        val json = """
            [
                {
                    "id": "feed/https://example.com/rss",
                    "title": "Feedly Sub",
                    "categories": [
                        {"id": "user/c08v7/category/Tech", "label": "Tech"}
                    ],
                    "website": "https://example.com"
                }
            ]
        """.trimIndent()
        val type = object : TypeToken<List<FeedlyDTO.Subscription>>() {}.type
        val subs: List<FeedlyDTO.Subscription> = gson.fromJson(json, type)
        assertEquals(1, subs.size)
        assertEquals("feed/https://example.com/rss", subs[0].id)
        assertEquals("Tech", subs[0].categories?.get(0)?.label)
    }

    @Test
    fun testFeedlyStreamContentsParsing() {
        val json = """
            {
                "id": "user/c08v7/category/global.all",
                "title": "All",
                "items": [
                    {
                        "id": "item_12345",
                        "title": "Feedly Article",
                        "published": 1690000000,
                        "author": "Editor",
                        "content": {"content": "<p>Article text</p>"},
                        "unread": true,
                        "tags": [
                            {"id": "user/c08v7/tag/global.saved", "label": "Saved"}
                        ]
                    }
                ]
            }
        """.trimIndent()
        val stream = gson.fromJson(json, FeedlyDTO.StreamContents::class.java)
        assertNotNull(stream.items)
        assertEquals(1, stream.items?.size)
        assertEquals("item_12345", stream.items?.get(0)?.id)
        assertEquals("Feedly Article", stream.items?.get(0)?.title)
        assertEquals("<p>Article text</p>", stream.items?.get(0)?.content?.content)
        assertTrue(stream.items?.get(0)?.tags?.any { it.id.endsWith("global.saved") } == true)
    }

    @Test
    fun testFeedlyErrorResponseExtraction() {
        val errorJson = """{"errorCode":404,"requestId":"a32aa30dd8ccc611-MRS","errorMessage":"API version not found"}"""
        val errorMap = gson.fromJson(errorJson, Map::class.java)
        assertEquals("API version not found", errorMap["errorMessage"])
        assertEquals(404.0, errorMap["errorCode"])
    }

    @Test
    fun testFeedlyMarkersPayloadSerialization() {
        val marker = FeedlyDTO.MarkersUpdate(
            action = "markAsRead",
            type = "entries",
            entryIds = listOf("entry_1", "entry_2")
        )
        val json = gson.toJson(marker)
        assertTrue(json.contains("\"action\":\"markAsRead\""))
        assertTrue(json.contains("\"entryIds\":[\"entry_1\",\"entry_2\"]"))
    }
}
