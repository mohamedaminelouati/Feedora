package me.ash.reader.infrastructure.rss.provider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.ash.reader.infrastructure.rss.provider.feedly.FeedlyDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
                        "unread": true
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
    }
}
