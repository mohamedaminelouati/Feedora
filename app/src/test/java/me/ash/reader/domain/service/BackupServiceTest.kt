package me.ash.reader.domain.service

import android.content.Context
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.ash.reader.domain.model.account.Account
import me.ash.reader.domain.model.account.AccountType
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.model.feed.Feed
import me.ash.reader.domain.model.group.Group
import me.ash.reader.domain.repository.AccountDao
import me.ash.reader.domain.repository.ArticleDao
import me.ash.reader.domain.repository.FeedDao
import me.ash.reader.domain.repository.GroupDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner::class)
class BackupServiceTest {

    @Mock
    private lateinit var accountDao: AccountDao

    @Mock
    private lateinit var groupDao: GroupDao

    @Mock
    private lateinit var feedDao: FeedDao

    @Mock
    private lateinit var articleDao: ArticleDao

    private lateinit var backupService: BackupService

    @Before
    fun setUp() {
        backupService = BackupService(
            accountDao = accountDao,
            groupDao = groupDao,
            feedDao = feedDao,
            articleDao = articleDao,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun testImportFullBackupStream() = runBlocking {
        val context: Context = mock()

        val fullBackupJson = """
            {
              "version": 1,
              "appVersion": "0.16.2",
              "timestamp": 1700000000000,
              "preferences": {
                "darkTheme": 1
              },
              "accounts": [
                {
                  "id": 1,
                  "name": "Local Account",
                  "type": { "id": 1 }
                }
              ],
              "groups": [
                {
                  "id": "group-1",
                  "name": "Tech",
                  "accountId": 1
                }
              ],
              "feeds": [
                {
                  "id": "feed-1",
                  "name": "Ars Technica",
                  "url": "https://arstechnica.com/feed/",
                  "groupId": "group-1",
                  "accountId": 1,
                  "isNotification": true,
                  "isFullContent": false,
                  "isBrowser": false
                }
              ],
              "articles": [
                {
                  "id": "art-1",
                  "date": 1700000000000,
                  "title": "Exciting Tech News",
                  "author": "Editor",
                  "rawDescription": "<p>Content of article</p>",
                  "shortDescription": "Content preview",
                  "link": "https://example.com/article1",
                  "feedId": "feed-1",
                  "accountId": 1,
                  "isUnread": false,
                  "isStarred": true
                }
              ]
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(fullBackupJson.toByteArray(Charsets.UTF_8))
        val result = backupService.importBackup(context, inputStream)

        assertTrue(result.isSuccess)
        val importResult = result.getOrNull()
        assertTrue(importResult is BackupImportResult.Full)

        val full = importResult as BackupImportResult.Full
        assertEquals(1, full.accountsCount)
        assertEquals(1, full.groupsCount)
        assertEquals(1, full.feedsCount)
        assertEquals(1, full.articlesCount)
        assertTrue(full.hasPreferences)

        verify(accountDao).insertAllAccounts(any())
        verify(groupDao).insertAllGroups(any())
        verify(feedDao).insertAllFeeds(any())
        verify(articleDao).insertAllArticles(any())
    }

    @Test
    fun testImportPreferencesOnlyStream() = runBlocking {
        val context: Context = mock()

        val prefJson = """
            {
              "darkTheme": 1,
              "dynamicColor": true
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(prefJson.toByteArray(Charsets.UTF_8))
        val result = backupService.importPreferencesOnly(context, inputStream)

        assertTrue(result.isSuccess)
    }
}
