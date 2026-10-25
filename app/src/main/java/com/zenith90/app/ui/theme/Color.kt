package com.zenith90.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ZenithColors {
    // Light Mode (Warm Alabaster & Soft Sand)
    val AlabasterBg = Color(0xFFF8F7F4)
    val SoftSandSurface = Color(0xFFEFECE6)
    val SoftSandElevated = Color(0xFFE6E1D8)
    val DeepCharcoalText = Color(0xFF1C1B1F)
    val MutedSlateText = Color(0xFF605D66)
    val WarmTaupeAccent = Color(0xFF8C7A6B)

    // Dark Mode (OLED Pixel True Obsidian & Matte Grey)
    val TrueObsidianBg = Color(0xFF0F0F12)
    val ElevatedMatteCard = Color(0xFF1A1A1F)
    val GlassStrokeDark = Color(0xFF282830)
    val GlassStrokeLight = Color(0xFFDCD7CC)
    val OffWhiteText = Color(0xFFE6E1E5)
    val MutedStoneSecondary = Color(0xFF938F99)
    val MutedSageAccent = Color(0xFFA39B8B)

    // Data Visualization Gradient Stops
    val AmberStart = Color(0xFFF6D365)
    val CoralEnd = Color(0xFFFDA085)
    val SageStart = Color(0xFF84FAB0)
    val TealEnd = Color(0xFF8FD3F4)
    val VioletStart = Color(0xFFA18CD1)
    val PinkEnd = Color(0xFFFBC2EB)

    val StreakGradient = Brush.linearGradient(
        colors = listOf(AmberStart, CoralEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    val StudyCompletionGradient = Brush.linearGradient(
        colors = listOf(SageStart, TealEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    fun studyVerticalAreaBrush(alpha: Float = 0.35f) = Brush.verticalGradient(
        colors = listOf(
            SageStart.copy(alpha = alpha),
            TealEnd.copy(alpha = alpha * 0.4f),
            Color.Transparent
        )
    )

    fun wakeVerticalAreaBrush(alpha: Float = 0.35f) = Brush.verticalGradient(
        colors = listOf(
            AmberStart.copy(alpha = alpha),
            CoralEnd.copy(alpha = alpha * 0.4f),
            Color.Transparent
        )
    )
}
