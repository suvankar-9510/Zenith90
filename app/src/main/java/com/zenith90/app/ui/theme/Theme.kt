package com.zenith90.app.ui.theme

import android.app.Activity
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightZenithScheme = lightColorScheme(
    background = ZenithColors.AlabasterBg,
    surface = ZenithColors.SoftSandSurface,
    surfaceVariant = ZenithColors.SoftSandElevated,
    onBackground = ZenithColors.DeepCharcoalText,
    onSurface = ZenithColors.DeepCharcoalText,
    onSurfaceVariant = ZenithColors.MutedSlateText,
    primary = ZenithColors.WarmTaupeAccent,
    onPrimary = Color.White,
    outline = ZenithColors.GlassStrokeLight
)

private val DarkZenithScheme = darkColorScheme(
    background = ZenithColors.TrueObsidianBg,
    surface = ZenithColors.ElevatedMatteCard,
    surfaceVariant = Color(0xFF23232A),
    onBackground = ZenithColors.OffWhiteText,
    onSurface = ZenithColors.OffWhiteText,
    onSurfaceVariant = ZenithColors.MutedStoneSecondary,
    primary = ZenithColors.MutedSageAccent,
    onPrimary = ZenithColors.TrueObsidianBg,
    outline = ZenithColors.GlassStrokeDark
)

@Composable
fun Zenith90Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkZenithScheme else LightZenithScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZenithTypography,
        content = content
    )
}

object PixelHaptics {
    fun crispTick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun confirmCheck(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun heavyUnlock(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
