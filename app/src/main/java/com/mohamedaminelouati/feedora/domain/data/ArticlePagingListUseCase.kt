package com.mohamedaminelouati.feedora.domain.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.paging.ItemSnapshotList
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.cachedIn
import javax.inject.Inject
import kotlin.text.trim
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.domain.model.article.ArticleFlowItem
import com.mohamedaminelouati.feedora.domain.model.article.mapPagingFlowItem
import com.mohamedaminelouati.feedora.domain.service.AccountService
import com.mohamedaminelouati.feedora.domain.service.RssService
import com.mohamedaminelouati.feedora.infrastructure.android.AndroidStringsHelper
import com.mohamedaminelouati.feedora.infrastructure.di.ApplicationScope
import com.mohamedaminelouati.feedora.infrastructure.di.IODispatcher
import com.mohamedaminelouati.feedora.infrastructure.preference.SettingsProvider

class ArticlePagingListUseCase
@Inject
constructor(
    private val rssService: RssService,
    private val androidStringsHelper: AndroidStringsHelper,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val settingsProvider: SettingsProvider,
    private val filterStateUseCase: FilterStateUseCase,
    private val accountService: AccountService,
) {

    private val mutablePagerFlow =
        MutableStateFlow<PagerData>(
            PagerData(filterState = filterStateUseCase.filterStateFlow.value)
        )
    val pagerFlow: StateFlow<PagerData> = mutablePagerFlow

    var itemSnapshotList by
        mutableStateOf(
            ItemSnapshotList<ArticleFlowItem>(
                placeholdersBefore = 0,
                placeholdersAfter = 0,
                items = emptyList(),
            )
        )
        private set

    val pagingDataPresenter =
        object : PagingDataPresenter<ArticleFlowItem>() {
            override suspend fun presentPagingDataEvent(event: PagingDataEvent<ArticleFlowItem>) {
                itemSnapshotList = snapshot()
            }
        }

    init {
        applicationScope.launch(ioDispatcher) {
            combine(
                filterStateUseCase.filterStateFlow,
                accountService.currentAccountIdFlow,
                settingsProvider.settingsFlow.map { it.flowLayout }.distinctUntilChanged(),
            ) { filterState, _, flowLayout ->
                filterState to flowLayout
            }
            .collect { (filterState, flowLayout) ->
                val searchContent = filterState.searchContent

                mutablePagerFlow.value =
                    PagerData(
                        Pager(
                                config = PagingConfig(pageSize = 50, enablePlaceholders = false)
                            ) {
                                if (!searchContent.isNullOrBlank()) {
                                    rssService
                                        .get()
                                        .searchArticles(
                                            content = searchContent.trim(),
                                            groupId = filterState.group?.id,
                                            feedId = filterState.feed?.id,
                                            isStarred = filterState.filter.isStarred(),
                                            isUnread = filterState.filter.isUnread(),
                                            sortAscending =
                                                settingsProvider.settings.flowSortUnreadArticles
                                                    .value,
                                        )
                                } else {
                                    rssService
                                        .get()
                                        .pullArticles(
                                            groupId = filterState.group?.id,
                                            feedId = filterState.feed?.id,
                                            isStarred = filterState.filter.isStarred(),
                                            isUnread = filterState.filter.isUnread(),
                                            sortAscending =
                                                settingsProvider.settings.flowSortUnreadArticles
                                                    .value,
                                        )
                                }
                            }
                            .flow
                            .map { it.mapPagingFlowItem(androidStringsHelper, includeDateSeparators = !flowLayout.isGrid()) }
                            .cachedIn(applicationScope),
                        filterState = filterState,
                    )
            }
        }
        applicationScope.launch {
            pagerFlow.collectLatest { (pager, _) ->
                pager.collectLatest { pagingDataPresenter.collectFrom(it) }
            }
        }
    }
}

data class PagerData(
    val pager: Flow<PagingData<ArticleFlowItem>> = emptyFlow(),
    val filterState: FilterState = FilterState(),
)
