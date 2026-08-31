package com.mohamedaminelouati.feedora.domain.service

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import com.rometools.rome.feed.synd.SyndFeed
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.domain.model.account.Account
import com.mohamedaminelouati.feedora.domain.model.account.security.InoreaderSecurityKey
import com.mohamedaminelouati.feedora.domain.model.article.Article
import com.mohamedaminelouati.feedora.domain.model.feed.Feed
import com.mohamedaminelouati.feedora.domain.model.group.Group
import com.mohamedaminelouati.feedora.domain.repository.ArticleDao
import com.mohamedaminelouati.feedora.domain.repository.FeedDao
import com.mohamedaminelouati.feedora.domain.repository.GroupDao
import com.mohamedaminelouati.feedora.infrastructure.android.NotificationHelper
import com.mohamedaminelouati.feedora.infrastructure.di.DefaultDispatcher
import com.mohamedaminelouati.feedora.infrastructure.di.IODispatcher
import com.mohamedaminelouati.feedora.infrastructure.di.MainDispatcher
import com.mohamedaminelouati.feedora.infrastructure.html.Readability
import com.mohamedaminelouati.feedora.infrastructure.rss.RssHelper
import com.mohamedaminelouati.feedora.infrastructure.rss.provider.inoreader.InoreaderAPI
import com.mohamedaminelouati.feedora.ui.ext.decodeHTML
import com.mohamedaminelouati.feedora.ui.ext.dollarLast
import com.mohamedaminelouati.feedora.ui.ext.isFuture
import com.mohamedaminelouati.feedora.ui.ext.spacerDollar
import java.util.Date
import javax.inject.Inject

class InoreaderRssService @Inject constructor(
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

    private fun getInoreaderAPI(): InoreaderAPI {
        val account = accountService.getCurrentAccount()
        val secKey = InoreaderSecurityKey(account.securityKey)
        return InoreaderAPI.getInstance(
            context = context,
            tokenOrPassword = secKey.password ?: "",
            username = secKey.username,
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
    }

    override suspend fun validCredentials(account: Account): Boolean = withContext(ioDispatcher) {
        val secKey = InoreaderSecurityKey(account.securityKey)
        val api = InoreaderAPI.getInstance(
            context = context,
            tokenOrPassword = secKey.password ?: "",
            username = secKey.username,
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
        return@withContext api.validCredentials().also { success ->
            if (success) {
                try {
                    val info = api.getUserInfo()
                    (info.userName ?: info.userEmail)?.let {
                        accountService.update(account.copy(name = it))
                    }
                } catch (ignore: Exception) {}
            }
        }
    }

    override suspend fun clearAuthorization() {
        InoreaderAPI.clearInstance()
    }

    override suspend fun subscribe(
        feedLink: String,
        searchedFeed: SyndFeed,
        groupId: String,
        isNotification: Boolean,
        isFullContent: Boolean,
        isBrowser: Boolean,
    ) = withContext(ioDispatcher) {
        val api = getInoreaderAPI()
        val accountId = accountService.getCurrentAccountId()
        val resp = api.quickAddSubscription(feedLink)
        val feedId = accountId.spacerDollar(resp.streamId ?: feedLink)
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
        val streamId = feed.id.dollarLast()
        getInoreaderAPI().unsubscribe(streamId)
        super.deleteFeed(feed, onlyDeleteNoStarred)
    }

    override suspend fun sync(
        accountId: Int,
        feedId: String?,
        groupId: String?,
    ): ListenableWorker.Result = withContext(ioDispatcher) {
        try {
            val api = getInoreaderAPI()
            val preDate = Date()

            // 1. Sync subscriptions & categories
            val subsResp = api.getSubscriptions()
            val subscriptions = subsResp.subscriptions ?: emptyList()

            val groupsMap = mutableMapOf<String, Group>()
            val feeds = mutableListOf<Feed>()

            subscriptions.forEach { sub ->
                var assignedGroupId: String? = null
                val cat = sub.categories?.firstOrNull()
                if (cat != null) {
                    val gId = accountId.spacerDollar(cat.label)
                    assignedGroupId = gId
                    groupsMap[gId] = Group(
                        id = gId,
                        name = cat.label,
                        accountId = accountId,
                    )
                }

                feeds.add(
                    Feed(
                        id = accountId.spacerDollar(sub.id),
                        name = sub.title.decodeHTML() ?: context.getString(R.string.empty),
                        url = sub.url,
                        groupId = assignedGroupId ?: "",
                        accountId = accountId,
                        icon = sub.iconUrl,
                        isBrowser = false,
                        isNotification = false,
                        isFullContent = false,
                        lastSyncTime = System.currentTimeMillis(),
                        lastSyncStatus = 1,
                    )
                )
            }

            if (groupsMap.isNotEmpty()) {
                groupDao.insert(*groupsMap.values.toTypedArray())
            }
            if (feeds.isNotEmpty()) {
                feedDao.insert(*feeds.toTypedArray())
            }

            // 2. Sync stream contents
            val streamId = if (feedId != null) feedId.dollarLast() else "user/-/state/com.google/reading-list"
            val streamResp = api.getStreamContents(streamId = streamId, limit = 100)
            val items = streamResp.items ?: emptyList()

            val articles = items.map { item ->
                val artId = accountId.spacerDollar(item.id)
                val itemFeedId = item.origin?.streamId ?: "unknown"
                val fId = accountId.spacerDollar(itemFeedId)
                val isRead = item.categories?.any { it.endsWith("/state/com.google/read") } == true
                val isStarred = item.categories?.any { it.endsWith("/state/com.google/starred") } == true
                val pubDate = if (item.published != null && item.published > 0) Date(item.published * 1000) else preDate
                val contentHtml = item.summary?.content ?: ""
                val itemLink = item.canonical?.firstOrNull()?.href
                    ?: item.alternate?.firstOrNull()?.href
                    ?: ""

                Article(
                    id = artId,
                    date = pubDate.takeIf { !it.isFuture(preDate) } ?: preDate,
                    title = (item.title ?: "Untitled").decodeHTML() ?: context.getString(R.string.empty),
                    author = item.author,
                    rawDescription = contentHtml,
                    shortDescription = Readability.parseToText(contentHtml, itemLink).take(280),
                    img = rssHelper.findThumbnail(contentHtml),
                    link = itemLink,
                    feedId = fId,
                    accountId = accountId,
                    isUnread = !isRead,
                    isStarred = isStarred,
                    updateAt = preDate,
                )
            }
            if (articles.isNotEmpty()) {
                articleDao.insert(*articles.toTypedArray())
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("InoreaderRssService", "Sync error", e)
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
        val api = getInoreaderAPI()
        if (articleId != null) {
            val remoteId = articleId.dollarLast()
            val readTag = "user/-/state/com.google/read"
            if (isUnread) {
                api.editTag(itemId = remoteId, removeTag = readTag)
            } else {
                api.editTag(itemId = remoteId, addTag = readTag)
            }
        }
        super.markAsRead(groupId, feedId, articleId, before, isUnread)
    }

    override suspend fun markAsStarred(articleId: String, isStarred: Boolean) = withContext(ioDispatcher) {
        val remoteId = articleId.dollarLast()
        val starredTag = "user/-/state/com.google/starred"
        if (isStarred) {
            getInoreaderAPI().editTag(itemId = remoteId, addTag = starredTag)
        } else {
            getInoreaderAPI().editTag(itemId = remoteId, removeTag = starredTag)
        }
        super.markAsStarred(articleId, isStarred)
    }
}
