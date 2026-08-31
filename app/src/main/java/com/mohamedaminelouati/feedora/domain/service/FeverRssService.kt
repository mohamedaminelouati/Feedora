package com.mohamedaminelouati.feedora.domain.service

import android.content.Context
import android.util.Log
import androidx.annotation.CheckResult
import androidx.compose.ui.util.fastFilter
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import com.rometools.rome.feed.synd.SyndFeed
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import kotlin.collections.set
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.domain.model.account.Account
import com.mohamedaminelouati.feedora.domain.model.account.AccountType
import com.mohamedaminelouati.feedora.domain.model.account.security.FeverSecurityKey
import com.mohamedaminelouati.feedora.domain.model.article.Article
import com.mohamedaminelouati.feedora.domain.model.article.ArticleMeta
import com.mohamedaminelouati.feedora.domain.model.feed.Feed
import com.mohamedaminelouati.feedora.domain.model.group.Group
import com.mohamedaminelouati.feedora.domain.repository.ArticleDao
import com.mohamedaminelouati.feedora.domain.repository.FeedDao
import com.mohamedaminelouati.feedora.domain.repository.GroupDao
import com.mohamedaminelouati.feedora.infrastructure.android.NotificationHelper
import com.mohamedaminelouati.feedora.infrastructure.di.DefaultDispatcher
import com.mohamedaminelouati.feedora.infrastructure.di.IODispatcher
import com.mohamedaminelouati.feedora.infrastructure.di.MainDispatcher
import com.mohamedaminelouati.feedora.infrastructure.exception.FeverAPIException
import com.mohamedaminelouati.feedora.infrastructure.html.Readability
import com.mohamedaminelouati.feedora.infrastructure.rss.RssHelper
import com.mohamedaminelouati.feedora.infrastructure.rss.provider.fever.FeverAPI
import com.mohamedaminelouati.feedora.infrastructure.rss.provider.fever.FeverDTO
import com.mohamedaminelouati.feedora.ui.ext.decodeHTML
import com.mohamedaminelouati.feedora.ui.ext.dollarLast
import com.mohamedaminelouati.feedora.ui.ext.isFuture
import com.mohamedaminelouati.feedora.ui.ext.spacerDollar

class FeverRssService
@Inject
constructor(
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
) :
    AbstractRssRepository(
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
    override val addSubscription: Boolean = false
    override val moveSubscription: Boolean = false
    override val deleteSubscription: Boolean = false
    override val updateSubscription: Boolean = false

    private suspend fun getFeverAPI() =
        FeverSecurityKey(accountService.getCurrentAccount().securityKey).run {
            FeverAPI.getInstance(
                context = context,
                serverUrl = serverUrl!!,
                username = username!!,
                password = password!!,
                httpUsername = null,
                httpPassword = null,
                clientCertificateAlias = clientCertificateAlias,
            )
        }

    override suspend fun validCredentials(account: Account): Boolean =
        getFeverAPI().validCredentials() > 0

    override suspend fun clearAuthorization() {
        FeverAPI.clearInstance()
    }

    override suspend fun subscribe(
        feedLink: String,
        searchedFeed: SyndFeed,
        groupId: String,
        isNotification: Boolean,
        isFullContent: Boolean,
        isBrowser: Boolean,
    ) {
        throw FeverAPIException("Unsupported")
    }

    override suspend fun addGroup(destFeed: Feed?, newGroupName: String): String {
        throw FeverAPIException("Unsupported")
    }

    override suspend fun renameGroup(group: Group) {
        throw FeverAPIException("Unsupported")
    }

    override suspend fun renameFeed(feed: Feed) {
        throw FeverAPIException("Unsupported")
    }

    override suspend fun deleteGroup(group: Group, onlyDeleteNoStarred: Boolean?) {
        throw FeverAPIException("Unsupported")
    }

    override suspend fun deleteFeed(feed: Feed, onlyDeleteNoStarred: Boolean?) {
        throw FeverAPIException("Unsupported")
    }

    override suspend fun moveFeed(originGroupId: String, feed: Feed) {
        throw FeverAPIException("Unsupported")
    }

    override suspend fun changeFeedUrl(feed: Feed) {
        throw FeverAPIException("Unsupported")
    }

    /**
     * Fever API synchronous processing with object's ID to ensure idempotence and handle foreign
     * key relationships such as read status, starred status, etc.
     *
     * When synchronizing articles, 50 articles will be pulled in each round. The ID of the 50th
     * article in this round will be recorded and used as the starting mark for the next pull until
     * the number of articles obtained is 0 or their quantity exceeds 250, at which point the
     * pulling process stops.
     * 1. Fetch the Fever groups (may need to remove orphaned groups)
     * 2. Fetch the Fever feeds (including favicons, may need to remove orphaned feeds)
     * 3. Fetch the Fever articles
     * 4. Synchronize read/unread and starred/un-starred items
     */
    override suspend fun sync(
        accountId: Int,
        feedId: String?,
        groupId: String?,
    ): ListenableWorker.Result = coroutineScope {
        try {
            val preTime = System.currentTimeMillis()
            val preDate = Date(preTime)
            val account = accountService.getAccountById(accountId)!!
            check(account.type.id == AccountType.Fever.id) { "account type is invalid" }

            val feverAPI = getFeverAPI()

            // 1. Fetch the Fever groups
            val groupsBody = feverAPI.getGroups()

            val groups =
                groupsBody.groups?.map {
                    Group(
                        id = accountId.spacerDollar(it.id!!),
                        name = it.title ?: context.getString(R.string.empty),
                        accountId = accountId,
                    )
                } ?: emptyList()
            groupDao.insertOrUpdate(groups)

            // 2. Fetch the Fever feeds
            val feedsBody = feverAPI.getFeeds()

            val feedsGroupsMap =
                buildMap<String, String> {
                    groupsBody.feeds_groups?.forEach { feedsGroups ->
                        feedsGroups.group_id?.toString()?.let { groupId ->
                            feedsGroups.feed_ids?.split(",")?.forEach { feedId ->
                                this[feedId] = groupId
                            }
                        }
                    }

                    feedsBody.feeds_groups?.forEach { feedsGroups ->
                        feedsGroups.group_id?.toString()?.let { groupId ->
                            feedsGroups.feed_ids?.split(",")?.forEach { feedId ->
                                this[feedId] = groupId
                            }
                        }
                    }
                }

            // Fetch the Fever favicons
            val faviconsById = feverAPI.getFavicons().favicons?.associateBy { it.id } ?: emptyMap()
            feedDao.insertOrUpdate(
                feedsBody.feeds?.map {
                    Feed(
                        id = accountId.spacerDollar(it.id!!),
                        name = it.title.decodeHTML() ?: context.getString(R.string.empty),
                        url = it.url!!,
                        groupId = accountId.spacerDollar(feedsGroupsMap[it.id.toString()]!!),
                        accountId = accountId,
                        icon = faviconsById[it.favicon_id]?.data,
                    )
                } ?: emptyList()
            )

            // Handle empty icon for feeds
            val noIconFeeds = feedDao.queryNoIcon(accountId)
            feedDao.update(
                *noIconFeeds
                    .map { it.copy(icon = rssHelper.queryRssIconLink(it.url)) }
                    .toTypedArray()
            )

            // 3. Fetch the Fever articles (up to unlimited counts)
            val allArticles = mutableListOf<Article>()

            var lastSeenId = account.lastArticleId?.dollarLast() ?: ""

            while (true) {
                val itemsBody = feverAPI.getItemsSince(lastSeenId)
                val fetchedItems = itemsBody.items

                if (fetchedItems.isNullOrEmpty()) {
                    break
                }

                val articlesFromBatch =
                    fetchedItems.map { item ->
                        Article(
                            id = accountId.spacerDollar(item.id!!),
                            date =
                                item.created_on_time
                                    ?.run { Date(this * 1000) }
                                    ?.takeIf { !it.isFuture(preDate) } ?: preDate,
                            title = item.title.decodeHTML() ?: context.getString(R.string.empty),
                            author = item.author,
                            rawDescription = item.html ?: "",
                            shortDescription =
                                Readability.parseToText(item.html, item.url).take(280),
                            img = rssHelper.findThumbnail(item.html),
                            link = item.url ?: "",
                            feedId = accountId.spacerDollar(item.feed_id!!),
                            accountId = accountId,
                            isUnread = (item.is_read ?: 0) <= 0,
                            isStarred = (item.is_saved ?: 0) > 0,
                            updateAt = preDate,
                        )
                    }

                allArticles.addAll(articlesFromBatch)

                lastSeenId = fetchedItems.lastOrNull()?.id ?: break

                if (fetchedItems.size < 50) {
                    break
                }
            }

            if (allArticles.isNotEmpty()) {
                articleDao.insert(*allArticles.toTypedArray())
                val notificationFeeds =
                    feedDao.queryNotificationEnabled(accountId).associateBy { it.id }
                val notificationFeedIds = notificationFeeds.keys
                allArticles
                    .fastFilter { it.isUnread && it.feedId in notificationFeedIds }
                    .groupBy { it.feedId }
                    .mapKeys { (feedId, _) -> notificationFeeds[feedId]!! }
                    .forEach { (feed, articles) -> notificationHelper.notify(feed, articles) }
            }

            // 4. Synchronize read/unread and starred/un-starred
            val unreadArticleIds = feverAPI.getUnreadItems().unread_item_ids?.split(",")
            val starredArticleIds = feverAPI.getSavedItems().saved_item_ids?.split(",")
            val articleMeta = articleDao.queryMetadataAll(accountId)
            for (meta: ArticleMeta in articleMeta) {
                val articleId = meta.id.dollarLast()
                val shouldBeUnread = unreadArticleIds?.contains(articleId)
                val shouldBeStarred = starredArticleIds?.contains(articleId)
                if (meta.isUnread != shouldBeUnread) {
                    articleDao.markAsReadByArticleId(accountId, meta.id, shouldBeUnread ?: true)
                }
                if (meta.isStarred != shouldBeStarred) {
                    articleDao.markAsStarredByArticleId(
                        accountId,
                        meta.id,
                        shouldBeStarred ?: false,
                    )
                }
            }

            // Remove orphaned groups and feeds, after synchronizing the starred/un-starred
            val groupIds = groups.map { it.id }
            groupDao.queryAll(accountId).forEach {
                if (!groupIds.contains(it.id)) {
                    super.deleteGroup(it, true)
                }
            }

            feedDao.queryAll(accountId).forEach {
                if (!feedsGroupsMap.contains(it.id.dollarLast())) {
                    super.deleteFeed(it, true)
                }
            }

            Log.i("RLog", "onCompletion: ${System.currentTimeMillis() - preTime}")
            accountService.update(
                account.copy(
                    updateAt = Date(),
                    lastArticleId =
                        if (lastSeenId.isNotEmpty()) {
                            accountId.spacerDollar(lastSeenId)
                        } else account.lastArticleId,
                )
            )
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            Log.e("RLog", "On sync exception: ${e.message}", e)
            //                withContext(mainDispatcher) {
            //                    context.showToast(e.message)
            //                }
            ListenableWorker.Result.failure()
        }
    }

    override suspend fun markAsRead(
        groupId: String?,
        feedId: String?,
        articleId: String?,
        before: Date?,
        isUnread: Boolean,
    ) {
        super.markAsRead(groupId, feedId, articleId, before, isUnread)
        val feverAPI = getFeverAPI()
        val beforeUnixTimestamp = (before?.time ?: Date(Long.MAX_VALUE).time) / 1000
        when {
            groupId != null -> {
                feverAPI.markGroup(
                    status = if (isUnread) FeverDTO.StatusEnum.Unread else FeverDTO.StatusEnum.Read,
                    id = groupId.dollarLast().toLong(),
                    before = beforeUnixTimestamp,
                )
            }

            feedId != null -> {
                feverAPI.markFeed(
                    status = if (isUnread) FeverDTO.StatusEnum.Unread else FeverDTO.StatusEnum.Read,
                    id = feedId.dollarLast().toLong(),
                    before = beforeUnixTimestamp,
                )
            }

            articleId != null -> {
                feverAPI.markItem(
                    status = if (isUnread) FeverDTO.StatusEnum.Unread else FeverDTO.StatusEnum.Read,
                    id = articleId.dollarLast(),
                )
            }

            else -> {
                feedDao.queryAll(accountService.getCurrentAccountId()).forEach {
                    feverAPI.markFeed(
                        status =
                            if (isUnread) FeverDTO.StatusEnum.Unread else FeverDTO.StatusEnum.Read,
                        id = it.id.dollarLast().toLong(),
                        before = beforeUnixTimestamp,
                    )
                }
            }
        }
    }

    @CheckResult
    override suspend fun syncReadStatus(articleIds: Set<String>, isUnread: Boolean): Set<String> {
        val feverAPI = getFeverAPI()
        val syncedEntries = mutableSetOf<String>()
        articleIds
            .takeIf { it.isNotEmpty() }
            ?.forEachIndexed { index, it ->
                Log.d("RLog", "sync markAsRead: ${index}/${articleIds.size} num")
                feverAPI.markItem(
                    status = if (isUnread) FeverDTO.StatusEnum.Unread else FeverDTO.StatusEnum.Read,
                    id = it.dollarLast(),
                )
                syncedEntries += it
            }
        return syncedEntries
    }

    override suspend fun markAsStarred(articleId: String, isStarred: Boolean) {
        super.markAsStarred(articleId, isStarred)
        val feverAPI = getFeverAPI()
        feverAPI.markItem(
            status = if (isStarred) FeverDTO.StatusEnum.Saved else FeverDTO.StatusEnum.Unsaved,
            id = articleId.dollarLast(),
        )
    }
}
