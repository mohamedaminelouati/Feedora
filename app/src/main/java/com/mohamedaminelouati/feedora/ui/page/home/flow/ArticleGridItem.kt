package com.mohamedaminelouati.feedora.ui.page.home.flow

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import coil.size.Precision
import coil.size.Scale
import com.mohamedaminelouati.feedora.domain.model.article.ArticleWithFeed
import com.mohamedaminelouati.feedora.infrastructure.preference.FlowArticleListDescPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.FlowArticleReadIndicatorPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalFlowArticleListDesc
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalFlowArticleListFeedIcon
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalFlowArticleListFeedName
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalFlowArticleListImage
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalFlowArticleListReadIndicator
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalFlowArticleListTime
import com.mohamedaminelouati.feedora.ui.component.FeedIcon
import com.mohamedaminelouati.feedora.ui.component.base.RYAsyncImage
import com.mohamedaminelouati.feedora.ui.component.base.SIZE_1000
import com.mohamedaminelouati.feedora.ui.component.menu.AnimatedDropdownMenu
import com.mohamedaminelouati.feedora.ui.ext.requiresBidi
import com.mohamedaminelouati.feedora.ui.ext.surfaceColorAtElevation
import com.mohamedaminelouati.feedora.ui.theme.applyTextDirection

private val IMG_SRC_REGEX = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

private fun extractFirstImageUrl(html: String?): String? {
    if (html.isNullOrBlank()) return null
    return IMG_SRC_REGEX.find(html)?.groupValues?.get(1)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArticleGridItem(
    modifier: Modifier = Modifier,
    articleWithFeed: ArticleWithFeed,
    isUnread: Boolean = articleWithFeed.article.isUnread,
    articleListTonalElevation: Int = 0,
    isMenuEnabled: Boolean = true,
    onClick: (ArticleWithFeed) -> Unit = {},
    onToggleStarred: (ArticleWithFeed) -> Unit = {},
    onToggleRead: (ArticleWithFeed) -> Unit = {},
    onMarkAboveAsRead: ((ArticleWithFeed) -> Unit)? = null,
    onMarkBelowAsRead: ((ArticleWithFeed) -> Unit)? = null,
    onShare: ((ArticleWithFeed) -> Unit)? = null,
) {
    val feed = articleWithFeed.feed
    val article = articleWithFeed.article

    val articleListFeedIcon = LocalFlowArticleListFeedIcon.current
    val articleListFeedName = LocalFlowArticleListFeedName.current
    val articleListImage = LocalFlowArticleListImage.current
    val articleListDesc = LocalFlowArticleListDesc.current
    val articleListDate = LocalFlowArticleListTime.current
    val articleListReadIndicator = LocalFlowArticleListReadIndicator.current

    var isMenuExpanded by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current

    val itemShape = RoundedCornerShape(16.dp)
    val readAlpha = when (articleListReadIndicator) {
        FlowArticleReadIndicatorPreference.None -> 1f
        FlowArticleReadIndicatorPreference.AllRead -> if (isUnread) 1f else 0.5f
        FlowArticleReadIndicatorPreference.ExcludingStarred -> if (isUnread || article.isStarred) 1f else 0.5f
    }

    val imageUrl = remember(article.img, article.rawDescription) {
        if (!article.img.isNullOrBlank()) {
            article.img
        } else {
            extractFirstImageUrl(article.rawDescription)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(itemShape)
            .alpha(readAlpha)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    menuOffset = down.position.round()
                }
            }
            .combinedClickable(
                onClick = { onClick(articleWithFeed) },
                onLongClick = if (isMenuEnabled) {
                    { isMenuExpanded = true }
                } else null,
            ),
        shape = itemShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(articleListTonalElevation.dp),
        tonalElevation = articleListTonalElevation.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Mandatory Hero Image or Neutral Placeholder Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl != null) {
                    RYAsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        data = imageUrl,
                        contentScale = ContentScale.Crop,
                        scale = Scale.FILL,
                        size = SIZE_1000,
                        precision = Precision.INEXACT,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Article,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Feed Header (Icon + Name + Badges)
                if (articleListFeedIcon.value || articleListFeedName.value || article.isStarred || isUnread) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (articleListFeedIcon.value) {
                                FeedIcon(
                                    feedName = feed.name,
                                    iconUrl = feed.icon,
                                    size = 26.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (articleListFeedName.value) {
                                Text(
                                    text = feed.name,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (article.isStarred) {
                                StarredIcon()
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            if (isUnread) {
                                Icon(
                                    modifier = Modifier.size(10.dp),
                                    imageVector = Icons.Rounded.FiberManualRecord,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Article Title
                Text(
                    text = article.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall
                        .applyTextDirection(article.title.requiresBidi())
                        .merge(fontWeight = FontWeight.Bold, lineHeight = 18.sp),
                    maxLines = if (articleListDesc != FlowArticleListDescPreference.NONE) 2 else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Short Description
                if (
                    articleListDesc != FlowArticleListDescPreference.NONE &&
                    article.shortDescription.isNotBlank()
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.shortDescription,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                            .applyTextDirection(article.shortDescription.requiresBidi())
                            .merge(lineHeight = 16.sp),
                        maxLines = when (articleListDesc) {
                            FlowArticleListDescPreference.LONG -> 4
                            FlowArticleListDescPreference.SHORT -> 2
                            else -> 2
                        },
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Date Time String
                if (articleListDate.value && article.dateString != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = article.dateString ?: "",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Context Menu
            if (isMenuEnabled) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(
                            start = with(density) { menuOffset.x.toDp() },
                            top = with(density) { menuOffset.y.toDp() },
                        )
                ) {
                    AnimatedDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                    ) {
                        ArticleItemMenuContent(
                            articleWithFeed = articleWithFeed,
                            onToggleStarred = onToggleStarred,
                            onToggleRead = onToggleRead,
                            onMarkAboveAsRead = onMarkAboveAsRead,
                            onMarkBelowAsRead = onMarkBelowAsRead,
                            onShare = onShare,
                        ) {
                            isMenuExpanded = false
                        }
                    }
                }
            }
        }
    }
}
