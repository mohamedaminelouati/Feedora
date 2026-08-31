package com.mohamedaminelouati.feedora.domain.data

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.domain.model.feed.Feed
import com.mohamedaminelouati.feedora.domain.model.general.Filter
import com.mohamedaminelouati.feedora.domain.model.group.Group
import com.mohamedaminelouati.feedora.domain.repository.FeedDao
import com.mohamedaminelouati.feedora.domain.repository.GroupDao
import com.mohamedaminelouati.feedora.infrastructure.di.ApplicationScope
import com.mohamedaminelouati.feedora.infrastructure.preference.SettingsProvider
import javax.inject.Singleton

@Singleton
class FilterStateUseCase
@Inject
constructor(
    settingsProvider: SettingsProvider,
    private val feedDao: FeedDao,
    private val groupDao: GroupDao,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) {

    private val _filterUiState =
        MutableStateFlow(FilterState(filter = settingsProvider.settings.initialFilter.toFilter()))
    val filterStateFlow = _filterUiState.asStateFlow()
    private val filterState
        get() = filterStateFlow.value

    fun updateFilterState(
        feed: Feed? = filterState.feed,
        group: Group? = filterState.group,
        filter: Filter = filterState.filter,
        searchContent: String? = filterState.searchContent,
    ) {
        _filterUiState.update {
            it.copy(feed = feed, group = group, searchContent = searchContent, filter = filter)
        }
    }

    fun updateFilterState(filterState: FilterState) {
        _filterUiState.update { filterState }
    }

    fun init(feedId: String?, groupId: String?, filter: Filter = Filter.Unread) {
        coroutineScope.launch {
            val feed = feedId?.let { feedDao.queryById(it) }
            val group = groupId?.let { groupDao.queryById(it) }
            updateFilterState(feed = feed, group = group, filter = filter)
        }
    }
}

data class FilterState(
    val group: Group? = null,
    val feed: Feed? = null,
    val filter: Filter = Filter.All,
    val searchContent: String? = null,
)