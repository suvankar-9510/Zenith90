package com.zenith90.app.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightZenithScheme = lightColorScheme(
    background = ZenithColors.BotanicalBg,
    surface = ZenithColors.BotanicalSurface,
    surfaceVariant = ZenithColors.MintContainer,
    onBackground = ZenithColors.TextHeading,
    onSurface = ZenithColors.TextHeading,
    onSurfaceVariant = ZenithColors.TextMuted,
    primary = ZenithColors.ForestGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = ZenithColors.MintContainer,
    onPrimaryContainer = ZenithColors.ForestGreenDark,
    outline = ZenithColors.BotanicalBorder,
    outlineVariant = ZenithColors.BotanicalDivider
)

private val DarkZenithScheme = darkColorScheme(
    background = ZenithColors.TrueObsidianBg,
    surface = ZenithColors.ElevatedMatteCard,
    surfaceVariant = ZenithColors.DarkSurfaceVariant,
    onBackground = ZenithColors.OffWhiteText,
    onSurface = ZenithColors.OffWhiteText,
    onSurfaceVariant = ZenithColors.MutedStoneSecondary,
    primary = ZenithColors.MintAccent,
    onPrimary = ZenithColors.ForestGreenDark,
    primaryContainer = ZenithColors.DarkSurfaceVariant,
    onPrimaryContainer = ZenithColors.MintAccent,
    outline = ZenithColors.GlassStrokeDark
)

@Composable
fun Zenith90Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Material You dynamic Monet theming on Android 12+ (Pixel devices)
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkZenithScheme
        else -> LightZenithScheme
    }
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

    fun errorBuzz(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun timerPulse(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 45, 60, 45)
                    val amplitudes = intArrayOf(0, 180, 0, 220)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 40, 50, 40), -1)
                }
            }
        } catch (_: Exception) {
            // Haptics fallback gracefully
        }
    }

    fun milestoneImpact(view: View, context: Context) {
        confirmCheck(view)
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 60, 40, 80, 40, 120)
                    val amplitudes = intArrayOf(0, 120, 0, 180, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(180)
                }
            }
        } catch (_: Exception) {
            // Fallback
        }
    }
}
