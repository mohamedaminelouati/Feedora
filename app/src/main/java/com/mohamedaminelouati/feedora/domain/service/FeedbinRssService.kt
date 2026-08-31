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
import com.mohamedaminelouati.feedora.domain.model.account.security.FeedbinSecurityKey
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
import com.mohamedaminelouati.feedora.infrastructure.rss.provider.feedbin.FeedbinAPI
import com.mohamedaminelouati.feedora.ui.ext.decodeHTML
import com.mohamedaminelouati.feedora.ui.ext.dollarLast
import com.mohamedaminelouati.feedora.ui.ext.isFuture
import com.mohamedaminelouati.feedora.ui.ext.spacerDollar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class FeedbinRssService @Inject constructor(
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

    private fun getFeedbinAPI(): FeedbinAPI {
        val account = accountService.getCurrentAccount()
        val secKey = FeedbinSecurityKey(account.securityKey)
        return FeedbinAPI.getInstance(
            context = context,
            username = secKey.username ?: "",
            password = secKey.password ?: "",
            clientCertificateAlias = secKey.clientCertificateAlias,
        )
    }

    override suspend fun validCredentials(account: Account): Boolean = withContext(ioDispatcher) {
        val secKey = FeedbinSecurityKey(account.securityKey)
        val api = FeedbinAPI.getInstance(
            context = context,
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
        FeedbinAPI.clearInstance()
    }

    override suspend fun subscribe(
        feedLink: String,
        searchedFeed: SyndFeed,
        groupId: String,
        isNotification: Boolean,
        isFullContent: Boolean,
        isBrowser: Boolean,
    ) = withContext(ioDispatcher) {
        val api = getFeedbinAPI()
        val accountId = accountService.getCurrentAccountId()
        val sub = api.createSubscription(feedLink)
        val feed = Feed(
            id = accountId.spacerDollar(sub.feedId.toString()),
            name = (searchedFeed.title ?: sub.title).decodeHTML() ?: context.getString(R.string.empty),
            url = sub.feedUrl,
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
            try {
                val subs = getFeedbinAPI().getSubscriptions()
                val sub = subs.firstOrNull { it.feedId == feedRemoteId }
                if (sub != null) {
                    getFeedbinAPI().deleteSubscription(sub.id)
                }
            } catch (ignore: Exception) {}
        }
        super.deleteFeed(feed, onlyDeleteNoStarred)
    }

    override suspend fun sync(
        accountId: Int,
        feedId: String?,
        groupId: String?,
    ): ListenableWorker.Result = withContext(ioDispatcher) {
        try {
            val api = getFeedbinAPI()
            val preDate = Date()

            // 1. Sync taggings (categories/folders)
            val taggings = api.getTaggings()
            val tagNames = taggings.map { it.name }.distinct()
            val groups = tagNames.map {
                Group(
                    id = accountId.spacerDollar(it),
                    name = it,
                    accountId = accountId,
                )
            }
            if (groups.isNotEmpty()) {
                groupDao.insert(*groups.toTypedArray())
            }

            val feedToGroupMap = taggings.associate { it.feedId to accountId.spacerDollar(it.name) }

            // 2. Sync subscriptions
            val subs = api.getSubscriptions()
            val feeds = subs.map { sub ->
                Feed(
                    id = accountId.spacerDollar(sub.feedId.toString()),
                    name = sub.title.decodeHTML() ?: context.getString(R.string.empty),
                    url = sub.feedUrl,
                    groupId = feedToGroupMap[sub.feedId] ?: "",
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

            // 3. Sync entries
            val unreadIds = api.getUnreadEntryIds().toSet()
            val starredIds = api.getStarredEntryIds().toSet()

            val remoteEntries = api.getEntries(page = 1, perPage = 100)
            val articles = remoteEntries.map { entry ->
                val artId = accountId.spacerDollar(entry.id.toString())
                val fId = accountId.spacerDollar(entry.feedId.toString())
                val contentHtml = entry.content ?: entry.summary ?: ""
                val pubDate = parseIsoDate(entry.published)
                Article(
                    id = artId,
                    date = pubDate.takeIf { !it.isFuture(preDate) } ?: preDate,
                    title = (entry.title ?: "Untitled").decodeHTML() ?: context.getString(R.string.empty),
                    author = entry.author,
                    rawDescription = contentHtml,
                    shortDescription = Readability.parseToText(contentHtml, entry.url).take(280),
                    img = rssHelper.findThumbnail(contentHtml),
                    link = entry.url,
                    feedId = fId,
                    accountId = accountId,
                    isUnread = unreadIds.contains(entry.id),
                    isStarred = starredIds.contains(entry.id),
                    updateAt = preDate,
                )
            }
            if (articles.isNotEmpty()) {
                articleDao.insert(*articles.toTypedArray())
            }

            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("FeedbinRssService", "Sync error", e)
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
        val api = getFeedbinAPI()
        if (articleId != null) {
            val remoteId = articleId.dollarLast().toLongOrNull()
            if (remoteId != null) {
                if (isUnread) {
                    api.markAsUnread(listOf(remoteId))
                } else {
                    api.markAsRead(listOf(remoteId))
                }
            }
        }
        super.markAsRead(groupId, feedId, articleId, before, isUnread)
    }

    override suspend fun markAsStarred(articleId: String, isStarred: Boolean) = withContext(ioDispatcher) {
        val remoteId = articleId.dollarLast().toLongOrNull()
        if (remoteId != null) {
            if (isStarred) {
                getFeedbinAPI().markAsStarred(listOf(remoteId))
            } else {
                getFeedbinAPI().unmarkAsStarred(listOf(remoteId))
            }
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
