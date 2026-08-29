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
import me.ash.reader.domain.model.account.security.FeedlySecurityKey
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
import me.ash.reader.infrastructure.rss.provider.feedly.FeedlyAPI
import me.ash.reader.infrastructure.rss.provider.feedly.FeedlyDTO
import me.ash.reader.ui.ext.decodeHTML
import me.ash.reader.ui.ext.dollarLast
import me.ash.reader.ui.ext.isFuture
import me.ash.reader.ui.ext.spacerDollar
import java.util.Date
import javax.inject.Inject

class FeedlyRssService @Inject constructor(
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

    private fun getFeedlyAPI(): FeedlyAPI {
        val account = accountService.getCurrentAccount()
        val secKey = FeedlySecurityKey(account.securityKey)
        return FeedlyAPI.getInstance(
            context = context,
            accessToken = secKey.password ?: "",
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
    }

    override suspend fun validCredentials(account: Account): Boolean = withContext(ioDispatcher) {
        val secKey = FeedlySecurityKey(account.securityKey)
        val api = FeedlyAPI.getInstance(
            context = context,
            accessToken = secKey.password ?: "",
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
        return@withContext api.validCredentials().also { success ->
            if (success) {
                try {
                    val profile = api.getProfile()
                    (profile.givenName ?: profile.email)?.let {
                        accountService.update(account.copy(name = it))
                    }
                } catch (ignore: Exception) {}
            }
        }
    }

    override suspend fun clearAuthorization() {
        FeedlyAPI.clearInstance()
    }

    override suspend fun subscribe(
        feedLink: String,
        searchedFeed: SyndFeed,
        groupId: String,
        isNotification: Boolean,
        isFullContent: Boolean,
        isBrowser: Boolean,
    ) = withContext(ioDispatcher) {
        val api = getFeedlyAPI()
        val accountId = accountService.getCurrentAccountId()
        val feedStreamId = if (feedLink.startsWith("feed/")) feedLink else "feed/$feedLink"
        val title = searchedFeed.title?.decodeHTML() ?: feedLink
        api.subscribe(feedStreamId, title)
        val feed = Feed(
            id = accountId.spacerDollar(feedStreamId),
            name = title,
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
        val feedStreamId = feed.id.dollarLast()
        getFeedlyAPI().unsubscribe(feedStreamId)
        super.deleteFeed(feed, onlyDeleteNoStarred)
    }

    override suspend fun sync(
        accountId: Int,
        feedId: String?,
        groupId: String?,
    ): ListenableWorker.Result = withContext(ioDispatcher) {
        try {
            val api = getFeedlyAPI()
            val preDate = Date()

            // 1. Sync categories & subscriptions
            val categories = api.getCategories()
            val groups = categories.map {
                Group(
                    id = accountId.spacerDollar(it.id),
                    name = it.label,
                    accountId = accountId,
                )
            }
            if (groups.isNotEmpty()) {
                groupDao.insert(*groups.toTypedArray())
            }

            val subscriptions = api.getSubscriptions()
            val feeds = subscriptions.map { sub ->
                val primaryCatId = sub.categories?.firstOrNull()?.id?.let { cid -> accountId.spacerDollar(cid) }
                val feedUrl = if (sub.id.startsWith("feed/")) sub.id.removePrefix("feed/") else sub.id
                Feed(
                    id = accountId.spacerDollar(sub.id),
                    name = sub.title.decodeHTML() ?: context.getString(R.string.empty),
                    url = feedUrl,
                    groupId = primaryCatId ?: "",
                    accountId = accountId,
                    icon = sub.visualUrl,
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

            // 2. Sync stream contents
            val profile = api.getProfile()
            val targetStreamId = if (feedId != null) {
                feedId.dollarLast()
            } else if (groupId != null) {
                groupId.dollarLast()
            } else {
                "user/${profile.id}/category/global.all"
            }

            val streamContents = api.getStreamContents(streamId = targetStreamId, count = 100)
            val items = streamContents.items ?: emptyList()

            val articles = items.map { item ->
                val artId = accountId.spacerDollar(item.id)
                val itemFeedId = item.origin?.streamId ?: "unknown"
                val fId = accountId.spacerDollar(itemFeedId)
                val contentHtml = item.content?.content ?: item.summary?.content ?: ""
                val itemLink = item.alternate?.firstOrNull()?.href ?: ""
                val pubDate = if (item.published != null && item.published > 0) Date(item.published) else preDate
                val isStarred = item.tags?.any { it.id.endsWith("/tag/global.saved") } == true

                Article(
                    id = artId,
                    date = pubDate.takeIf { !it.isFuture(preDate) } ?: preDate,
                    title = (item.title ?: "Untitled").decodeHTML() ?: context.getString(R.string.empty),
                    author = item.author,
                    rawDescription = contentHtml,
                    shortDescription = Readability.parseToText(contentHtml, itemLink).take(280),
                    img = item.visual?.url ?: rssHelper.findThumbnail(contentHtml),
                    link = itemLink,
                    feedId = fId,
                    accountId = accountId,
                    isUnread = item.unread != false,
                    isStarred = isStarred,
                    updateAt = preDate,
                )
            }
            if (articles.isNotEmpty()) {
                articleDao.insert(*articles.toTypedArray())
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("FeedlyRssService", "Sync error", e)
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
        val api = getFeedlyAPI()
        if (articleId != null) {
            val remoteId = articleId.dollarLast()
            val action = if (isUnread) "keepUnread" else "markAsRead"
            api.updateMarkers(action = action, entryIds = listOf(remoteId))
        }
        super.markAsRead(groupId, feedId, articleId, before, isUnread)
    }

    override suspend fun markAsStarred(articleId: String, isStarred: Boolean) = withContext(ioDispatcher) {
        val remoteId = articleId.dollarLast()
        val action = if (isStarred) "markAsSaved" else "markAsUnsaved"
        getFeedlyAPI().updateMarkers(action = action, entryIds = listOf(remoteId))
        super.markAsStarred(articleId, isStarred)
    }
}
