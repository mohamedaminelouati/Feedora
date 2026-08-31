package com.mohamedaminelouati.feedora.ui.ext

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StringExtTest {

    @Test
    fun testExtractDomain() {
        Assert.assertEquals(null, "".extractDomain())
        Assert.assertEquals(null, null.extractDomain())
        var case = "https://example.com"
        Assert.assertEquals("example.com", case.extractDomain())
        case = "example.com"
        Assert.assertEquals("example.com", case.extractDomain())
        case = "https://example.com/blog/hello/"
        Assert.assertEquals("example.com", case.extractDomain())
        case = "http://example.com/blog/hello/"
        Assert.assertEquals("example.com", case.extractDomain())
        case = "file://example.com/blog"
        Assert.assertEquals("example.com", case.extractDomain())
        case = "file://127.0.0.1/blog"
        Assert.assertEquals("127.0.0.1", case.extractDomain())
        case = "ftp://127.0.0.1"
        Assert.assertEquals("127.0.0.1", case.extractDomain())
    }
}
