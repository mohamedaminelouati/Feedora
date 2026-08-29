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
import me.ash.reader.domain.model.account.security.TTRSSSecurityKey
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
import me.ash.reader.infrastructure.rss.provider.ttrss.TTRSSAPI
import me.ash.reader.ui.ext.decodeHTML
import me.ash.reader.ui.ext.dollarLast
import me.ash.reader.ui.ext.isFuture
import me.ash.reader.ui.ext.spacerDollar
import java.util.Date
import javax.inject.Inject

class TTRSSRssService @Inject constructor(
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
    override val moveSubscription: Boolean = false
    override val deleteSubscription: Boolean = true
    override val updateSubscription: Boolean = false

    private fun getTTRSSAPI(): TTRSSAPI {
        val account = accountService.getCurrentAccount()
        val secKey = TTRSSSecurityKey(account.securityKey)
        return TTRSSAPI.getInstance(
            context = context,
            serverUrl = secKey.serverUrl ?: "",
            username = secKey.username ?: "",
            password = secKey.password ?: "",
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
    }

    override suspend fun validCredentials(account: Account): Boolean = withContext(ioDispatcher) {
        val secKey = TTRSSSecurityKey(account.securityKey)
        val api = TTRSSAPI.getInstance(
            context = context,
            serverUrl = secKey.serverUrl ?: "",
            username = secKey.username ?: "",
            password = secKey.password ?: "",
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
        return@withContext api.validCredentials().also { success ->
            if (success) {
                secKey.username?.let {
                    accountService.update(account.copy(name = it))
                }
            }
        }
    }

    override suspend fun clearAuthorization() {
        TTRSSAPI.clearInstance()
    }

    override suspend fun subscribe(
        feedLink: String,
        searchedFeed: SyndFeed,
        groupId: String,
        isNotification: Boolean,
        isFullContent: Boolean,
        isBrowser: Boolean,
    ) = withContext(ioDispatcher) {
        val api = getTTRSSAPI()
        val catId = groupId.dollarLast().toLongOrNull()
        api.subscribeToFeed(feedLink, catId)
    }

    override suspend fun deleteFeed(feed: Feed, onlyDeleteNoStarred: Boolean?) = withContext(ioDispatcher) {
        val feedRemoteId = feed.id.dollarLast().toLongOrNull()
        if (feedRemoteId != null) {
            getTTRSSAPI().unsubscribeFeed(feedRemoteId)
        }
        super.deleteFeed(feed, onlyDeleteNoStarred)
    }

    override suspend fun sync(
        accountId: Int,
        feedId: String?,
        groupId: String?,
    ): ListenableWorker.Result = withContext(ioDispatcher) {
        try {
            val api = getTTRSSAPI()
            val preDate = Date()

            // Sync categories
            val categories = api.getCategories()
            val groups = categories.filter { it.id > 0 }.map {
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
            val remoteFeeds = api.getFeeds(-4)
            val feeds = remoteFeeds.filter { it.id > 0 }.map {
                val gId = it.catId?.takeIf { c -> c > 0 }?.let { cid -> accountId.spacerDollar(cid.toString()) } ?: ""
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

            // Sync headlines
            val targetFeedRemoteId = feedId?.dollarLast()?.toLongOrNull() ?: -4
            val headlines = api.getHeadlines(feedId = targetFeedRemoteId, limit = 100)
            val articles = headlines.mapNotNull { h ->
                val hFeedId = h.feedId ?: return@mapNotNull null
                val artId = accountId.spacerDollar(h.id.toString())
                val fId = accountId.spacerDollar(hFeedId.toString())
                val pubDate = if (h.updated != null && h.updated > 0) Date(h.updated * 1000) else preDate
                val contentHtml = h.content ?: ""
                Article(
                    id = artId,
                    date = pubDate.takeIf { !it.isFuture(preDate) } ?: preDate,
                    title = h.title.decodeHTML() ?: context.getString(R.string.empty),
                    author = h.author,
                    rawDescription = contentHtml,
                    shortDescription = Readability.parseToText(contentHtml, h.link).take(280),
                    img = rssHelper.findThumbnail(contentHtml),
                    link = h.link,
                    feedId = fId,
                    accountId = accountId,
                    isUnread = h.unread == true,
                    isStarred = h.marked == true,
                    updateAt = preDate,
                )
            }
            if (articles.isNotEmpty()) {
                articleDao.insert(*articles.toTypedArray())
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("TTRSSRssService", "Sync error", e)
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
        val api = getTTRSSAPI()
        if (articleId != null) {
            val remoteId = articleId.dollarLast().toLongOrNull()
            if (remoteId != null) {
                // field 2 = unread, mode 0 = set unread, mode 1 = set read
                val mode = if (isUnread) 0 else 1
                api.updateArticle(remoteId, mode = mode, field = 2)
            }
        }
        super.markAsRead(groupId, feedId, articleId, before, isUnread)
    }

    override suspend fun markAsStarred(articleId: String, isStarred: Boolean) = withContext(ioDispatcher) {
        val remoteId = articleId.dollarLast().toLongOrNull()
        if (remoteId != null) {
            // field 0 = marked (starred), mode 1 = set marked, mode 0 = unset marked
            val mode = if (isStarred) 1 else 0
            getTTRSSAPI().updateArticle(remoteId, mode = mode, field = 0)
        }
        super.markAsStarred(articleId, isStarred)
    }
}
