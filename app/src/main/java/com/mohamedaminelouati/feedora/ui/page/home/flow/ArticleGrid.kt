package com.mohamedaminelouati.feedora.ui.page.home.flow

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.mohamedaminelouati.feedora.domain.data.Diff
import com.mohamedaminelouati.feedora.domain.model.article.ArticleFlowItem
import com.mohamedaminelouati.feedora.domain.model.article.ArticleWithFeed

@Suppress("FunctionName")
fun LazyGridScope.ArticleGrid(
    pagingItems: LazyPagingItems<ArticleFlowItem>,
    diffMap: Map<String, Diff>,
    isShowFeedIcon: Boolean,
    articleListTonalElevation: Int,
    isMenuEnabled: Boolean = true,
    onClick: (ArticleWithFeed, Int) -> Unit = { _, _ -> },
    onToggleStarred: (ArticleWithFeed) -> Unit = {},
    onToggleRead: (ArticleWithFeed) -> Unit = {},
    onMarkAboveAsRead: ((ArticleWithFeed) -> Unit)? = null,
    onMarkBelowAsRead: ((ArticleWithFeed) -> Unit)? = null,
    onShare: ((ArticleWithFeed) -> Unit)? = null,
) {
    items(
        count = pagingItems.itemCount,
        key = pagingItems.itemKey(::key),
        contentType = pagingItems.itemContentType(::contentType),
        span = { GridItemSpan(1) },
    ) { index ->
        when (val item = pagingItems[index]) {
            is ArticleFlowItem.Article -> {
                val article = item.articleWithFeed.article
                ArticleGridItem(
                    articleWithFeed = item.articleWithFeed,
                    isUnread = diffMap[article.id]?.isUnread ?: article.isUnread,
                    articleListTonalElevation = articleListTonalElevation,
                    isMenuEnabled = isMenuEnabled,
                    onClick = { onClick(it, index) },
                    onToggleStarred = onToggleStarred,
                    onToggleRead = onToggleRead,
                    onMarkAboveAsRead = if (index <= 1) null else onMarkAboveAsRead,
                    onMarkBelowAsRead = if (index == pagingItems.itemCount - 1) null else onMarkBelowAsRead,
                    onShare = onShare,
                )
            }

            else -> {}
        }
    }
}

private fun key(item: ArticleFlowItem): String {
    return when (item) {
        is ArticleFlowItem.Article -> item.articleWithFeed.article.id
        is ArticleFlowItem.Date -> item.date
    }
}

private fun contentType(item: ArticleFlowItem): Int {
    return when (item) {
        is ArticleFlowItem.Article -> CONTENT_TYPE_ARTICLE
        is ArticleFlowItem.Date -> CONTENT_TYPE_DATE_HEADER
    }
}
