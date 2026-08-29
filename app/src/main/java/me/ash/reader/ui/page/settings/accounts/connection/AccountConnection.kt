package me.ash.reader.ui.page.settings.accounts.connection

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.ash.reader.R
import me.ash.reader.domain.model.account.Account
import me.ash.reader.domain.model.account.AccountType
import me.ash.reader.ui.component.base.Subtitle

@Composable
fun LazyItemScope.AccountConnection(
    account: Account,
) {
    if (account.type.id != AccountType.Local.id) {
        Subtitle(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(R.string.connection),
        )
    }
    when (account.type.id) {
        AccountType.Fever.id -> FeverConnection(account)
        AccountType.GoogleReader.id -> GoogleReaderConnection(account)
        AccountType.FreshRSS.id -> FreshRSSConnection(account)
        AccountType.Inoreader.id -> GoogleReaderConnection(account)
        AccountType.Miniflux.id -> GoogleReaderConnection(account)
        AccountType.TTRSS.id -> GoogleReaderConnection(account)
        AccountType.Feedbin.id -> GoogleReaderConnection(account)
        AccountType.Feedly.id -> GoogleReaderConnection(account)
    }
    if (account.type.id != AccountType.Local.id) {
        Spacer(modifier = Modifier.height(24.dp))
    }
}
