package com.mohamedaminelouati.feedora.ui.page.settings.tips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.infrastructure.preference.OpenLinkPreference
import com.mohamedaminelouati.feedora.ui.component.base.RYAsyncImage
import com.mohamedaminelouati.feedora.ui.ext.openURL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorDialog(modifier: Modifier = Modifier, onDismissRequest: () -> Unit) {
    ModalBottomSheet(modifier = modifier, onDismissRequest = onDismissRequest) {
        SponsorDialogContent()
    }
}


private fun githubAvatar(login: String): String = "https://github.com/${login}.png"

@Composable
private fun SponsorDialogContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.become_a_sponsor),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.sponsor_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SponsorItem(
            model = githubAvatar("mohamedaminelouati"),
            name = "Mohamed Amine Louati",
            description = "Lead Developer / Maintainer",
        ) {
            context.openURL("https://github.com/mohamedaminelouati", openLink = OpenLinkPreference.default)
        }
        SponsorItem(
            model = githubAvatar("Ashinch"),
            name = "Ash",
            description = "Original Author",
        ) {
            context.openURL("https://github.com/Ashinch", openLink = OpenLinkPreference.default)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SponsorItem(
    modifier: Modifier = Modifier,
    model: Any?,
    name: String,
    description: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .padding(vertical = 12.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RYAsyncImage(
            data = model,
            modifier = Modifier
                .size(64.dp)
                .aspectRatio(1f)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalButton(onClick = onClick, interactionSource = interactionSource) {
            Text(stringResource(R.string.sponsor))
        }
    }
}