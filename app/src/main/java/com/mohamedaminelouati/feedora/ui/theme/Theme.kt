package com.mohamedaminelouati.feedora.ui.theme

import android.os.Build
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalBasicFonts
import com.mohamedaminelouati.feedora.infrastructure.preference.LocalThemeIndex
import com.mohamedaminelouati.feedora.ui.theme.palette.LocalTonalPalettes
import com.mohamedaminelouati.feedora.ui.theme.palette.TonalPalettes
import com.mohamedaminelouati.feedora.ui.theme.palette.core.ProvideZcamViewingConditions
import com.mohamedaminelouati.feedora.ui.theme.palette.dynamic.extractTonalPalettesFromUserWallpaper
import com.mohamedaminelouati.feedora.ui.theme.palette.dynamicDarkColorScheme
import com.mohamedaminelouati.feedora.ui.theme.palette.dynamicLightColorScheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    useDarkTheme: Boolean,
    wallpaperPalettes: List<TonalPalettes> = extractTonalPalettesFromUserWallpaper(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current

    LaunchedEffect(useDarkTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (useDarkTheme) {
                view.windowInsetsController?.setSystemBarsAppearance(
                    0,
                    APPEARANCE_LIGHT_STATUS_BARS,
                )
            } else {
                view.windowInsetsController?.setSystemBarsAppearance(
                    APPEARANCE_LIGHT_STATUS_BARS,
                    APPEARANCE_LIGHT_STATUS_BARS,
                )
            }
        }
    }

    val themeIndex = LocalThemeIndex.current

    val tonalPalettes =
        wallpaperPalettes[
            if (themeIndex >= wallpaperPalettes.size) {
                when {
                    wallpaperPalettes.size == 5 -> 0
                    wallpaperPalettes.size > 5 -> 5
                    else -> 0
                }
            } else {
                themeIndex
            }]

    ProvideZcamViewingConditions {
        CompositionLocalProvider(
            LocalTonalPalettes provides tonalPalettes.apply { Preparing() },
            LocalTextStyle provides LocalTextStyle.current.applyTextDirection(),
        ) {
            val lightColors = dynamicLightColorScheme()
            val darkColors = dynamicDarkColorScheme()
            MaterialTheme(
                motionScheme = MotionScheme.expressive(),
                colorScheme = if (useDarkTheme) darkColors else lightColors,
                typography =
                    LocalBasicFonts.current
                        .asTypography(LocalContext.current)
                        .applyTextDirection(),
                shapes = Shapes,
                content = content,
            )
        }
    }
}
