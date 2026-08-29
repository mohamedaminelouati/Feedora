package me.ash.reader.infrastructure.rss.provider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.ash.reader.infrastructure.rss.provider.inoreader.InoreaderDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class InoreaderProviderTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun testInoreaderUserInfoParsing() {
        val json = """
            {
                "userId": "100500",
                "userName": "inouser",
                "userEmail": "user@example.com"
            }
        """.trimIndent()
        val info = gson.fromJson(json, InoreaderDTO.UserInfo::class.java)
        assertEquals("100500", info.userId)
        assertEquals("inouser", info.userName)
        assertEquals("user@example.com", info.userEmail)
    }

    @Test
    fun testInoreaderSubscriptionsParsing() {
        val json = """
            {
                "subscriptions": [
                    {
                        "id": "feed/http://example.com/rss",
                        "title": "Inoreader Feed",
                        "categories": [
                            {"id": "user/100500/label/News", "label": "News"}
                        ],
                        "url": "http://example.com/rss"
                    }
                ]
            }
        """.trimIndent()
        val resp = gson.fromJson(json, InoreaderDTO.SubscriptionsResponse::class.java)
        assertNotNull(resp.subscriptions)
        assertEquals(1, resp.subscriptions?.size)
        assertEquals("feed/http://example.com/rss", resp.subscriptions?.get(0)?.id)
        assertEquals("News", resp.subscriptions?.get(0)?.categories?.get(0)?.label)
    }
}
