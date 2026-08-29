package me.ash.reader.ui.page.settings.accounts.addition

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.ash.reader.domain.service.OpmlService
import me.ash.reader.domain.service.RssService
import me.ash.reader.infrastructure.android.AndroidStringsHelper
import me.ash.reader.infrastructure.rss.RssHelper
import javax.inject.Inject

@HiltViewModel
class AdditionViewModel @Inject constructor(
    private val opmlService: OpmlService,
    private val rssService: RssService,
    private val rssHelper: RssHelper,
    private val androidStringsHelper: AndroidStringsHelper,
) : ViewModel() {

    private val _additionUiState = MutableStateFlow(AdditionUiState())
    val additionUiState: StateFlow<AdditionUiState> = _additionUiState.asStateFlow()

    fun showAddLocalAccountDialog() {
        _additionUiState.update {
            it.copy(
                addLocalAccountDialogVisible = true,
            )
        }
    }

    fun hideAddLocalAccountDialog() {
        _additionUiState.update {
            it.copy(
                addLocalAccountDialogVisible = false,
            )
        }
    }

    fun showAddFeverAccountDialog() {
        _additionUiState.update {
            it.copy(
                addFeverAccountDialogVisible = true,
            )
        }
    }

    fun hideAddFeverAccountDialog() {
        _additionUiState.update {
            it.copy(
                addFeverAccountDialogVisible = false,
            )
        }
    }

    fun showAddGoogleReaderAccountDialog() {
        _additionUiState.update {
            it.copy(
                addGoogleReaderAccountDialogVisible = true,
            )
        }
    }

    fun hideAddGoogleReaderAccountDialog() {
        _additionUiState.update {
            it.copy(
                addGoogleReaderAccountDialogVisible = false,
            )
        }
    }

    fun showAddFreshRSSAccountDialog() {
        _additionUiState.update {
            it.copy(
                addFreshRSSAccountDialogVisible = true,
            )
        }
    }

    fun hideAddFreshRSSAccountDialog() {
        _additionUiState.update {
            it.copy(
                addFreshRSSAccountDialogVisible = false,
            )
        }
    }

    fun showAddMinifluxAccountDialog() {
        _additionUiState.update {
            it.copy(
                addMinifluxAccountDialogVisible = true,
            )
        }
    }

    fun hideAddMinifluxAccountDialog() {
        _additionUiState.update {
            it.copy(
                addMinifluxAccountDialogVisible = false,
            )
        }
    }

    fun showAddTTRSSAccountDialog() {
        _additionUiState.update {
            it.copy(
                addTTRSSAccountDialogVisible = true,
            )
        }
    }

    fun hideAddTTRSSAccountDialog() {
        _additionUiState.update {
            it.copy(
                addTTRSSAccountDialogVisible = false,
            )
        }
    }

    fun showAddInoreaderAccountDialog() {
        _additionUiState.update {
            it.copy(
                addInoreaderAccountDialogVisible = true,
            )
        }
    }

    fun hideAddInoreaderAccountDialog() {
        _additionUiState.update {
            it.copy(
                addInoreaderAccountDialogVisible = false,
            )
        }
    }

    fun showAddFeedbinAccountDialog() {
        _additionUiState.update {
            it.copy(
                addFeedbinAccountDialogVisible = true,
            )
        }
    }

    fun hideAddFeedbinAccountDialog() {
        _additionUiState.update {
            it.copy(
                addFeedbinAccountDialogVisible = false,
            )
        }
    }

    fun showAddFeedlyAccountDialog() {
        _additionUiState.update {
            it.copy(
                addFeedlyAccountDialogVisible = true,
            )
        }
    }

    fun hideAddFeedlyAccountDialog() {
        _additionUiState.update {
            it.copy(
                addFeedlyAccountDialogVisible = false,
            )
        }
    }
}

data class AdditionUiState(
    val addLocalAccountDialogVisible: Boolean = false,
    val addFeverAccountDialogVisible: Boolean = false,
    val addGoogleReaderAccountDialogVisible: Boolean = false,
    val addFreshRSSAccountDialogVisible: Boolean = false,
    val addMinifluxAccountDialogVisible: Boolean = false,
    val addTTRSSAccountDialogVisible: Boolean = false,
    val addInoreaderAccountDialogVisible: Boolean = false,
    val addFeedbinAccountDialogVisible: Boolean = false,
    val addFeedlyAccountDialogVisible: Boolean = false,
)
