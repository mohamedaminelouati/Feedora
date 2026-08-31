package com.mohamedaminelouati.feedora.ui.page.settings.accounts.addition

import android.app.Activity
import android.security.KeyChain
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.ui.ext.findActivity
import com.mohamedaminelouati.feedora.ui.ext.showToastSuspend

@Composable
fun CertificateSelector(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = {
                KeyChain.choosePrivateKeyAlias(
                    context.findActivity() ?: context as Activity,
                    { alias ->
                        if (alias == null) {
                            scope.launch {
                                context.showToastSuspend("No client certificate found")
                            }
                        } else {
                            onValueChange(alias)
                        }
                    },
                    null,
                    null,
                    null,
                    null
                )
            }) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                )
            }
        },
        placeholder = {
            Text(stringResource(R.string.client_certificate))
        }, modifier = modifier
            .fillMaxWidth()
    )
}