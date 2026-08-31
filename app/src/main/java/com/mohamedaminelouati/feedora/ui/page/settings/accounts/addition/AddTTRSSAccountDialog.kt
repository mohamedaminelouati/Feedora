package com.mohamedaminelouati.feedora.ui.page.settings.accounts.addition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.domain.model.account.Account
import com.mohamedaminelouati.feedora.domain.model.account.AccountType
import com.mohamedaminelouati.feedora.domain.model.account.security.TTRSSSecurityKey
import com.mohamedaminelouati.feedora.ui.component.base.RYDialog
import com.mohamedaminelouati.feedora.ui.component.base.RYOutlineTextField
import com.mohamedaminelouati.feedora.ui.ext.collectAsStateValue
import com.mohamedaminelouati.feedora.ui.ext.showToast
import com.mohamedaminelouati.feedora.ui.page.settings.accounts.AccountViewModel

@Composable
fun AddTTRSSAccountDialog(
    onBack: () -> Unit,
    onNavigateToAccountDetails: (Int) -> Unit,
    viewModel: AdditionViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState = viewModel.additionUiState.collectAsStateValue()
    val accountUiState = accountViewModel.accountUiState.collectAsStateValue()

    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var clientCertificateAlias by rememberSaveable { mutableStateOf("") }

    RYDialog(
        modifier = Modifier.padding(horizontal = 44.dp),
        visible = uiState.addTTRSSAccountDialogVisible,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = {
            focusManager.clearFocus()
            accountViewModel.cancelAdd()
            viewModel.hideAddTTRSSAccountDialog()
        },
        icon = {
            if (accountUiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.ic_ttrss),
                    contentDescription = stringResource(R.string.ttrss),
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.ttrss),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(10.dp))
                RYOutlineTextField(
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = accountUiState.isLoading,
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = stringResource(R.string.server_url),
                    placeholder = "https://tt-rss.example.com/plugins/api_greader/",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Spacer(modifier = Modifier.height(10.dp))
                RYOutlineTextField(
                    modifier = Modifier.fillMaxWidth(),
                    requestFocus = false,
                    readOnly = accountUiState.isLoading,
                    value = username,
                    onValueChange = { username = it },
                    label = stringResource(R.string.username),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Spacer(modifier = Modifier.height(10.dp))
                RYOutlineTextField(
                    modifier = Modifier.fillMaxWidth(),
                    requestFocus = false,
                    readOnly = accountUiState.isLoading,
                    value = password,
                    onValueChange = { password = it },
                    isPassword = true,
                    label = stringResource(R.string.password),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Spacer(modifier = Modifier.height(10.dp))
                CertificateSelector(
                    value = clientCertificateAlias,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    clientCertificateAlias = it
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !accountUiState.isLoading
                        && serverUrl.isNotBlank()
                        && username.isNotEmpty()
                        && password.isNotEmpty(),
                onClick = {
                    focusManager.clearFocus()
                    var normalizedUrl = serverUrl.trim()
                    if (!normalizedUrl.endsWith("/")) {
                        normalizedUrl += "/"
                    }
                    accountViewModel.addAccount(
                        Account(
                            type = AccountType.TTRSS,
                            name = context.getString(R.string.ttrss),
                            securityKey = TTRSSSecurityKey(
                                serverUrl = normalizedUrl,
                                username = username,
                                password = password,
                                clientCertificateAlias = clientCertificateAlias,
                            ).toString(),
                        )
                    ) { account, exception ->
                        if (account == null) {
                            context.showToast(exception?.message ?: "Not valid credentials")
                        } else {
                            viewModel.hideAddTTRSSAccountDialog()
                            onBack()
                            onNavigateToAccountDetails(account.id!!)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    accountViewModel.cancelAdd()
                    focusManager.clearFocus()
                    viewModel.hideAddTTRSSAccountDialog()
                }
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
