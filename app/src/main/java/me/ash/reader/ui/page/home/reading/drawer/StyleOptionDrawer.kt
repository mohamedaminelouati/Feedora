package me.ash.reader.ui.page.home.reading.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.ash.reader.R
import me.ash.reader.ui.component.base.M3BottomDrawer
import me.ash.reader.ui.ext.collectAsStateValue
import me.ash.reader.ui.page.home.feeds.drawer.feed.FeedOptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleOptionDrawer(
    drawerState: SheetState,
    feedOptionViewModel: FeedOptionViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    content: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val feedOptionUiState = feedOptionViewModel.feedOptionUiState.collectAsStateValue()
    val feed = feedOptionUiState.feed
    val toastString = stringResource(R.string.rename_toast, feedOptionUiState.newName)

    BackHandler(drawerState.isVisible) {
        scope.launch {
            onDismiss()
        }
    }

    content()

    M3BottomDrawer(
        drawerState = drawerState,
        onDismiss = onDismiss,
        sheetContent = {
            Info()
        }
    )
}

@Composable
fun Info() {
    Column(modifier = Modifier.navigationBarsPadding()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Tab(selected = true, onClick = { /*TODO*/ })
        }
    }
}

@Preview
@Composable
fun Prev() {
    Tab(selected = true, onClick = { /*TODO*/ })
}
