package com.mohamedaminelouati.feedora.infrastructure.rss.provider

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mohamedaminelouati.feedora.infrastructure.rss.provider.ttrss.TTRSSDTO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TTRSSProviderTest {

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun testTTRSSLoginParsing() {
        val json = """{"seq": 0, "status": 0, "content": {"session_id": "sess_12345", "api_level": 14}}"""
        val type = object : TypeToken<TTRSSDTO.Response<TTRSSDTO.LoginResponse>>() {}.type
        val resp: TTRSSDTO.Response<TTRSSDTO.LoginResponse> = gson.fromJson(json, type)
        assertEquals(0, resp.status)
        assertNotNull(resp.content)
        assertEquals("sess_12345", resp.content?.sessionId)
        assertEquals(14, resp.content?.apiLevel)
    }

    @Test
    fun testTTRSSHeadlinesParsing() {
        val json = """
            {
                "seq": 0,
                "status": 0,
                "content": [
                    {
                        "id": 99,
                        "title": "TTRSS Article",
                        "link": "https://example.com/ttrss/1",
                        "updated": 1690000000,
                        "content": "Full story content",
                        "unread": true,
                        "marked": true,
                        "author": "Bob",
                        "feed_id": 10
                    }
                ]
            }
        """.trimIndent()
        val type = object : TypeToken<TTRSSDTO.Response<List<TTRSSDTO.Headline>>>() {}.type
        val resp: TTRSSDTO.Response<List<TTRSSDTO.Headline>> = gson.fromJson(json, type)
        assertEquals(0, resp.status)
        assertEquals(1, resp.content?.size)
        assertEquals(99L, resp.content?.get(0)?.id)
        assertEquals("TTRSS Article", resp.content?.get(0)?.title)
        assertTrue(resp.content?.get(0)?.unread == true)
        assertTrue(resp.content?.get(0)?.marked == true)
    }
}
