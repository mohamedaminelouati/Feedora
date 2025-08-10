package me.ash.reader.ui.page.home.feeds.drawer.feed

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.infrastructure.preference.LocalOpenLink
import me.ash.reader.infrastructure.preference.LocalOpenLinkSpecificBrowser
import me.ash.reader.ui.component.ChangeUrlDialog
import me.ash.reader.ui.component.FeedIcon
import me.ash.reader.ui.component.RenameDialog
import me.ash.reader.ui.component.base.M3BottomDrawer
import me.ash.reader.ui.component.base.TextFieldDialog
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.ext.openURL
import me.ash.reader.ui.ext.roundClick
import me.ash.reader.ui.ext.showToast
import me.ash.reader.ui.page.home.feeds.FeedOptionView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedOptionDrawer(
    drawerState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    feedOptionViewModel: FeedOptionViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val openLink = LocalOpenLink.current
    val openLinkSpecificBrowser = LocalOpenLinkSpecificBrowser.current
    val scope = rememberCoroutineScope()
    val feedOptionUiState = feedOptionViewModel.feedOptionUiState.collectAsStateValue()
    val feed = feedOptionUiState.feed
    val toastString = stringResource(R.string.rename_toast, feedOptionUiState.newName)

    BackHandler(drawerState.isVisible) { scope.launch { onDismiss() } }

    M3BottomDrawer(
        drawerState = drawerState,
        onDismiss = onDismiss,
        sheetContent = {
            Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    FeedIcon(
                        modifier = Modifier.clickable { feedOptionViewModel.reloadIcon() },
                        feedName = feed?.name,
                        iconUrl = feed?.icon,
                        size = 24.dp,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        modifier =
                            Modifier.roundClick {
                                if (feedOptionViewModel.rssService.get().updateSubscription) {
                                    feedOptionViewModel.showRenameDialog()
                                }
                            },
                        text = feed?.name ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                FeedOptionView(
                    modifier = Modifier,
                    link = feed?.url ?: stringResource(R.string.unknown),
                    groups = feedOptionUiState.groups,
                    selectedAllowNotificationPreset =
                        feedOptionUiState.feed?.isNotification ?: false,
                    selectedParseFullContentPreset = feedOptionUiState.feed?.isFullContent ?: false,
                    selectedOpenInBrowserPreset = feedOptionUiState.feed?.isBrowser ?: false,
                    isMoveToGroup = true,
                    showGroup = feedOptionViewModel.rssService.get().moveSubscription,
                    showUnsubscribe = feedOptionViewModel.rssService.get().deleteSubscription,
                    notSubscribeMode = true,
                    selectedGroupId = feedOptionUiState.feed?.groupId ?: "",
                    allowNotificationPresetOnClick = {
                        feedOptionViewModel.changeAllowNotificationPreset()
                    },
                    parseFullContentPresetOnClick = {
                        feedOptionViewModel.changeParseFullContentPreset()
                    },
                    openInBrowserPresetOnClick = {
                        feedOptionViewModel.changeOpenInBrowserPreset()
                    },
                    clearArticlesOnClick = { feedOptionViewModel.showClearDialog() },
                    unsubscribeOnClick = { feedOptionViewModel.showDeleteDialog() },
                    onGroupClick = { feedOptionViewModel.selectedGroup(it) },
                    onAddNewGroup = { feedOptionViewModel.showNewGroupDialog() },
                    onFeedUrlClick = {
                        context.openURL(feed?.url, openLink, openLinkSpecificBrowser)
                    },
                    onFeedUrlLongClick = {
                        if (feedOptionViewModel.rssService.get().updateSubscription) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            feedOptionViewModel.showFeedUrlDialog()
                        }
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
    )

    DeleteFeedDialog(feedName = feed?.name ?: "", onConfirm = { onDismiss() })

    ClearFeedDialog(feedName = feed?.name ?: "", onConfirm = { onDismiss() })

    TextFieldDialog(
        visible = feedOptionUiState.newGroupDialogVisible,
        title = stringResource(R.string.create_new_group),
        icon = Icons.Outlined.CreateNewFolder,
        value = feedOptionUiState.newGroupContent,
        placeholder = stringResource(R.string.name),
        onValueChange = { feedOptionViewModel.inputNewGroup(it) },
        onDismissRequest = { feedOptionViewModel.hideNewGroupDialog() },
        onConfirm = { feedOptionViewModel.addNewGroup() },
    )

    RenameDialog(
        visible = feedOptionUiState.renameDialogVisible,
        value = feedOptionUiState.newName,
        onValueChange = { feedOptionViewModel.inputNewName(it) },
        onDismissRequest = { feedOptionViewModel.hideRenameDialog() },
        onConfirm = {
            feedOptionViewModel.renameFeed()
            onDismiss()
            context.showToast(toastString)
        },
    )

    ChangeUrlDialog(
        visible = feedOptionUiState.changeUrlDialogVisible,
        value = feedOptionUiState.newUrl,
        onValueChange = { feedOptionViewModel.inputNewUrl(it) },
        onDismissRequest = { feedOptionViewModel.hideFeedUrlDialog() },
        onConfirm = {
            feedOptionViewModel.changeFeedUrl()
            onDismiss()
        },
    )
}
