package me.ash.reader.ui.page.home.feeds

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import me.ash.reader.domain.model.feed.Feed
import me.ash.reader.infrastructure.preference.LocalFeedsShowSyncStatus
import me.ash.reader.ui.component.FeedIcon
import me.ash.reader.ui.component.base.RYExtensibleVisibility
import me.ash.reader.ui.page.home.feeds.drawer.feed.FeedOptionViewModel

@Composable
private fun contentPadding(isLastItem: Boolean): PaddingValues =
    if (isLastItem) {
        PaddingValues(bottom = 22.dp, start = 14.dp, end = 14.dp, top = 14.dp)
    } else {
        PaddingValues(14.dp)
    }

private fun formatSyncDateTime(lastSyncTime: Long, lastSyncStatus: Int): Pair<String, Color> {
    if (lastSyncTime <= 0L || lastSyncStatus == 0) {
        return Pair("Non synchronisé", Color(0xFF9E9E9E))
    }

    val syncDate = Date(lastSyncTime)
    val now = Date()
    val calSync = Calendar.getInstance().apply { time = syncDate }
    val calNow = Calendar.getInstance().apply { time = now }

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormatter.format(syncDate)

    val dateStr =
        when {
            calSync.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calSync.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) -> {
                "Aujourd'hui à $timeStr"
            }
            calSync.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calSync.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR) - 1 -> {
                "Hier à $timeStr"
            }
            else -> {
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateFormatter.format(syncDate)
            }
        }

    return if (lastSyncStatus == 1) {
        Pair("Synchro : $dateStr", Color(0xFF4CAF50))
    } else {
        Pair("Échec : $dateStr", Color(0xFFF44336))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedItemImpl(
    feed: Feed,
    isLastItem: () -> Boolean = { false },
    onLongClickCallback: (String) -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = {
                        onLongClick()
                        scope.launch { onLongClickCallback(feed.id) }
                    },
                )
                .padding(contentPadding(isLastItem())),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                FeedIcon(feedName = feed.name, iconUrl = feed.icon, modifier = Modifier)
                val showSyncStatus = LocalFeedsShowSyncStatus.current.value
                Column(modifier = Modifier.padding(start = 12.dp, end = 6.dp)) {
                    Text(
                        text = feed.name,
                        style =
                            MaterialTheme.typography.labelLarge.merge(
                                lineHeight = 20.sp,
                                lineHeightStyle =
                                    LineHeightStyle(
                                        trim = LineHeightStyle.Trim.Both,
                                        alignment = LineHeightStyle.Alignment.Center,
                                    ),
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showSyncStatus) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val (statusText, statusColor) =
                                formatSyncDateTime(feed.lastSyncTime, feed.lastSyncStatus)
                            Box(
                                modifier =
                                    Modifier.size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (feed.important != 0) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.outline,
                    content = {
                        Text(
                            text = feed.important.toString(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedItem(
    feed: Feed,
    isLastItem: () -> Boolean = { false },
    isExpanded: () -> Boolean,
    feedOptionViewModel: FeedOptionViewModel = hiltViewModel(),
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    RYExtensibleVisibility(visible = isExpanded()) {
        FeedItemImpl(
            feed = feed,
            isLastItem = isLastItem,
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickCallback = {
                scope.launch { feedOptionViewModel.fetchFeed(it) }
            },
        )
    }
}