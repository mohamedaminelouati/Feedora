package com.mohamedaminelouati.feedora.ui.page.home.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingFonts
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingTitleAlign
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingTitleBold
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalReadingTitleUpperCase
import com.mohamedaminelouati.feedora.ui.ext.formatAsString
import com.mohamedaminelouati.feedora.ui.ext.requiresBidi
import com.mohamedaminelouati.feedora.ui.theme.applyTextDirection
import java.util.Date

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Metadata(
    feedName: String,
    title: String,
    publishedDate: Date,
    modifier: Modifier = Modifier,
    author: String? = null,
) {
    val context = LocalContext.current
    val titleBold = LocalReadingTitleBold.current
    val titleUpperCase = LocalReadingTitleUpperCase.current
    val titleAlign = LocalReadingTitleAlign.current.toTextAlign()
    val dateString =
        remember(publishedDate) { publishedDate.formatAsString(context, atHourMinute = true) }
    val fontFamily = LocalReadingFonts.current.asFontFamily(context)

    val titleUpperCaseString by remember { derivedStateOf { title.uppercase() } }

    val labelColor = MaterialTheme.colorScheme.outline.copy(alpha = .7f)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(12.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = dateString,
            color = labelColor,
            style = MaterialTheme.typography.labelMedium.merge(fontFamily = fontFamily),
            textAlign = titleAlign,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = if (titleUpperCase.value) titleUpperCaseString else title,
            color = MaterialTheme.colorScheme.onSurface,
            style =
                MaterialTheme.typography.headlineLarge
                    .merge(
                        fontFamily = fontFamily,
                        fontWeight = if (titleBold.value) FontWeight.Bold else FontWeight.Medium,
                    )
                    .applyTextDirection(requiresBidi = title.requiresBidi()),
            textAlign = titleAlign,
        )
        Spacer(modifier = Modifier.height(4.dp))
        author?.let {
            if (it.isNotEmpty()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = it,
                    color = labelColor,
                    style = MaterialTheme.typography.labelMedium.merge(fontFamily = fontFamily),
                    textAlign = titleAlign,
                )
            }
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = feedName,
            color = labelColor,
            style = MaterialTheme.typography.labelMedium.merge(fontFamily = fontFamily),
            textAlign = titleAlign,
        )
    }
}
