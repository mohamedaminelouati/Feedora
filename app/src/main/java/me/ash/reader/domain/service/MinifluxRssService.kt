package me.ash.reader.domain.service

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import com.rometools.rome.feed.synd.SyndFeed
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.ash.reader.R
import me.ash.reader.domain.model.account.Account
import me.ash.reader.domain.model.account.security.MinifluxSecurityKey
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.model.feed.Feed
import me.ash.reader.domain.model.group.Group
import me.ash.reader.domain.repository.ArticleDao
import me.ash.reader.domain.repository.FeedDao
import me.ash.reader.domain.repository.GroupDao
import me.ash.reader.infrastructure.android.NotificationHelper
import me.ash.reader.infrastructure.di.DefaultDispatcher
import me.ash.reader.infrastructure.di.IODispatcher
import me.ash.reader.infrastructure.di.MainDispatcher
import me.ash.reader.infrastructure.html.Readability
import me.ash.reader.infrastructure.rss.RssHelper
import me.ash.reader.infrastructure.rss.provider.miniflux.MinifluxAPI
import me.ash.reader.ui.ext.decodeHTML
import me.ash.reader.ui.ext.dollarLast
import me.ash.reader.ui.ext.isFuture
import me.ash.reader.ui.ext.spacerDollar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class MinifluxRssService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao,
    private val rssHelper: RssHelper,
    private val notificationHelper: NotificationHelper,
    private val groupDao: GroupDao,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    workManager: WorkManager,
    private val accountService: AccountService,
) : AbstractRssRepository(
    articleDao,
    groupDao,
    feedDao,
    workManager,
    rssHelper,
    notificationHelper,
    ioDispatcher,
    defaultDispatcher,
    accountService,
) {

    override val importSubscription: Boolean = false
    override val addSubscription: Boolean = true
    override val moveSubscription: Boolean = true
    override val deleteSubscription: Boolean = true
    override val updateSubscription: Boolean = true

    private fun getMinifluxAPI(): MinifluxAPI {
        val account = accountService.getCurrentAccount()
        val secKey = MinifluxSecurityKey(account.securityKey)
        return MinifluxAPI.getInstance(
            context = context,
            serverUrl = secKey.serverUrl ?: "",
            username = secKey.username ?: "",
            password = secKey.password ?: "",
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
    }

    override suspend fun validCredentials(account: Account): Boolean = withContext(ioDispatcher) {
        val secKey = MinifluxSecurityKey(account.securityKey)
        val api = MinifluxAPI.getInstance(
            context = context,
            serverUrl = secKey.serverUrl ?: "",
            username = secKey.username ?: "",
            password = secKey.password ?: "",
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
        return@withContext api.validCredentials().also { success ->
            if (success) {
                try {
                    val user = api.getMe()
                    user.username?.let {
                        accountService.update(account.copy(name = it))
                    }
                } catch (ignore: Exception) {
                    Log.e("MinifluxRssService", "Failed to fetch user profile", ignore)
                }
            }
        }
    }

    override suspend fun clearAuthorization() {
        MinifluxAPI.clearInstance()
    }

    override suspend fun subscribe(
        feedLink: String,
        searchedFeed: SyndFeed,
        groupId: String,
        isNotification: Boolean,
        isFullContent: Boolean,
        isBrowser: Boolean,
    ) = withContext(ioDispatcher) {
        val api = getMinifluxAPI()
        val accountId = accountService.getCurrentAccountId()
        val catId = groupId.dollarLast().toLongOrNull()
        val resp = api.createFeed(feedLink, catId)
        val feedId = accountId.spacerDollar(resp.feedId.toString())
        val feed = Feed(
            id = feedId,
            name = searchedFeed.title?.decodeHTML() ?: feedLink,
            url = feedLink,
            groupId = groupId,
            accountId = accountId,
            icon = searchedFeed.icon?.link,
            isBrowser = isBrowser,
            isNotification = isNotification,
            isFullContent = isFullContent,
            lastSyncTime = System.currentTimeMillis(),
            lastSyncStatus = 1,
        )
        feedDao.insert(feed)
    }

    override suspend fun deleteFeed(feed: Feed, onlyDeleteNoStarred: Boolean?) = withContext(ioDispatcher) {
        val feedRemoteId = feed.id.dollarLast().toLongOrNull()
        if (feedRemoteId != null) {
            getMinifluxAPI().deleteFeed(feedRemoteId)
        }
        super.deleteFeed(feed, onlyDeleteNoStarred)
    }

    override suspend fun renameFeed(feed: Feed) = withContext(ioDispatcher) {
        val feedRemoteId = feed.id.dollarLast().toLongOrNull()
        val catId = feed.groupId?.dollarLast()?.toLongOrNull()
        if (feedRemoteId != null) {
            getMinifluxAPI().updateFeed(feedRemoteId, feed.name, catId)
        }
        super.renameFeed(feed)
    }

    override suspend fun moveFeed(originGroupId: String, feed: Feed) = withContext(ioDispatcher) {
        val feedRemoteId = feed.id.dollarLast().toLongOrNull()
        val catId = feed.groupId?.dollarLast()?.toLongOrNull()
        if (feedRemoteId != null && catId != null) {
            getMinifluxAPI().updateFeed(feedRemoteId, feed.name, catId)
        }
        super.moveFeed(originGroupId, feed)
    }

    override suspend fun sync(
        accountId: Int,
        feedId: String?,
        groupId: String?,
    ): ListenableWorker.Result = withContext(ioDispatcher) {
        try {
            val api = getMinifluxAPI()
            val preDate = Date()

            // Sync categories / groups
            val categories = api.getCategories()
            val groups = categories.map {
                Group(
                    id = accountId.spacerDollar(it.id.toString()),
                    name = it.title,
                    accountId = accountId,
                )
            }
            if (groups.isNotEmpty()) {
                groupDao.insert(*groups.toTypedArray())
            }

            // Sync feeds
            val remoteFeeds = api.getFeeds()
            val feeds = remoteFeeds.map {
                val gId = it.category?.id?.let { cid -> accountId.spacerDollar(cid.toString()) } ?: ""
                Feed(
                    id = accountId.spacerDollar(it.id.toString()),
                    name = it.title.decodeHTML() ?: context.getString(R.string.empty),
                    url = it.feedUrl,
                    groupId = gId,
                    accountId = accountId,
                    isBrowser = false,
                    isNotification = false,
                    isFullContent = false,
                    lastSyncTime = System.currentTimeMillis(),
                    lastSyncStatus = 1,
                )
            }
            if (feeds.isNotEmpty()) {
                feedDao.insert(*feeds.toTypedArray())
            }

            // Sync entries
            val remoteFeedId = feedId?.dollarLast()?.toLongOrNull()
            val remoteCatId = groupId?.dollarLast()?.toLongOrNull()
            val entriesResp = api.getEntries(
                limit = 100,
                feedId = remoteFeedId,
                categoryId = remoteCatId
            )
            val articles = (entriesResp.entries ?: emptyList()).map { entry ->
                val artId = accountId.spacerDollar(entry.id.toString())
                val fId = accountId.spacerDollar(entry.feedId.toString())
                val pubDate = parseIsoDate(entry.publishedAt)
                val contentHtml = entry.content ?: ""
                Article(
                    id = artId,
                    date = pubDate.takeIf { !it.isFuture(preDate) } ?: preDate,
                    title = entry.title.decodeHTML() ?: context.getString(R.string.empty),
                    author = entry.author,
                    rawDescription = contentHtml,
                    shortDescription = Readability.parseToText(contentHtml, entry.url).take(280),
                    img = rssHelper.findThumbnail(contentHtml),
                    link = entry.url,
                    feedId = fId,
                    accountId = accountId,
                    isUnread = entry.status != "read",
                    isStarred = entry.starred == true,
                    updateAt = preDate,
                )
            }
            if (articles.isNotEmpty()) {
                articleDao.insert(*articles.toTypedArray())
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("MinifluxRssService", "Sync error", e)
            ListenableWorker.Result.failure()
        }
    }

    override suspend fun markAsRead(
        groupId: String?,
        feedId: String?,
        articleId: String?,
        before: Date?,
        isUnread: Boolean,
    ) = withContext(ioDispatcher) {
        val api = getMinifluxAPI()
        if (articleId != null) {
            val remoteId = articleId.dollarLast().toLongOrNull()
            if (remoteId != null) {
                api.updateEntriesStatus(listOf(remoteId), if (isUnread) "unread" else "read")
            }
        }
        super.markAsRead(groupId, feedId, articleId, before, isUnread)
    }

    override suspend fun markAsStarred(articleId: String, isStarred: Boolean) = withContext(ioDispatcher) {
        val remoteId = articleId.dollarLast().toLongOrNull()
        if (remoteId != null) {
            getMinifluxAPI().toggleBookmark(remoteId)
        }
        super.markAsStarred(articleId, isStarred)
    }

    private fun parseIsoDate(dateStr: String?): Date {
        if (dateStr.isNullOrBlank()) return Date()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            try {
                val format2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                format2.parse(dateStr) ?: Date()
            } catch (e2: Exception) {
                Date()
            }
        }
    }
}
