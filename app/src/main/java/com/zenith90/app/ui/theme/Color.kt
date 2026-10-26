package com.zenith90.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ZenithColors {
    // --- BOTANICAL "TIMEZY" PALETTE (Light Mode) ---
    val ForestGreenPrimary = Color(0xFF14361F)
    val ForestGreenDark = Color(0xFF0C2414)
    val ForestGreenMedium = Color(0xFF265835)
    val MintAccent = Color(0xFF52B788)
    val MintContainer = Color(0xFFE2F4E7)

    val BotanicalBg = Color(0xFFF3F7F3)
    val BotanicalSurface = Color(0xFFFFFFFF)
    val BotanicalSurfaceElevated = Color(0xFFF8FAF8)
    val BotanicalBorder = Color(0xFFE3EDE3)
    val BotanicalDivider = Color(0xFFE8EFE8)

    val TextHeading = Color(0xFF142417)
    val TextBody = Color(0xFF334638)
    val TextMuted = Color(0xFF75887B)

    // Legacy Fallbacks
    val AlabasterBg = BotanicalBg
    val SoftSandSurface = BotanicalSurface
    val SoftSandElevated = BotanicalSurfaceElevated
    val DeepCharcoalText = TextHeading
    val MutedSlateText = TextMuted
    val WarmTaupeAccent = ForestGreenPrimary

    // --- DARK MODE (Deep Obsidian Forest & Emerald) ---
    val TrueObsidianBg = Color(0xFF0B140E)
    val ElevatedMatteCard = Color(0xFF132017)
    val DarkSurfaceVariant = Color(0xFF1B2D21)
    val GlassStrokeDark = Color(0xFF263D2E)
    val GlassStrokeLight = BotanicalBorder
    val OffWhiteText = Color(0xFFE8F1E9)
    val MutedStoneSecondary = Color(0xFF8BA291)
    val MutedSageAccent = MintAccent

    // --- PASTEL TASK & TIME-BLOCK PALETTE (from Reference UI) ---
    val PastelRose = Color(0xFFFFE8EC)
    val PastelRoseText = Color(0xFFB83E55)
    val PastelRoseAccent = Color(0xFFF87171)

    val PastelAmber = Color(0xFFFEF6D8)
    val PastelAmberText = Color(0xFF9E7C1B)
    val PastelAmberAccent = Color(0xFFFBBF24)

    val PastelMint = Color(0xFFE2F4E7)
    val PastelMintText = Color(0xFF1E653C)
    val PastelMintAccent = Color(0xFF34D399)

    val PastelSky = Color(0xFFDDF4FA)
    val PastelSkyText = Color(0xFF167E96)
    val PastelSkyAccent = Color(0xFF38BDF8)

    val PastelLilac = Color(0xFFF1EBFB)
    val PastelLilacText = Color(0xFF6C4BA2)
    val PastelLilacAccent = Color(0xFFA78BFA)

    // Data Visualization Gradient Stops
    val AmberStart = Color(0xFFF6D365)
    val CoralEnd = Color(0xFFFDA085)
    val SageStart = Color(0xFF84FAB0)
    val TealEnd = Color(0xFF8FD3F4)
    val VioletStart = Color(0xFFA18CD1)
    val PinkEnd = Color(0xFFFBC2EB)

    // Battle Royale / Gaming Tiers (PUBG & Free Fire inspired)
    val BronzeGradient = Brush.linearGradient(listOf(Color(0xFFE08D43), Color(0xFF8C4A19)))
    val SilverGradient = Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8)))
    val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFDF00), Color(0xFFFFA500)))
    val PlatinumGradient = Brush.linearGradient(listOf(Color(0xFF00F5D4), Color(0xFF00BBF9)))
    val DiamondGradient = Brush.linearGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)))
    val HeroicGradient = Brush.linearGradient(listOf(Color(0xFFFF0844), Color(0xFFFFB199)))
    val ConquerorGradient = Brush.linearGradient(listOf(Color(0xFFB12A5B), Color(0xFFFF5E36), Color(0xFFFFAA00)))
    val MythicGradient = Brush.linearGradient(listOf(Color(0xFF7F00FF), Color(0xFFE100FF)))

    // Locked Grey Palette
    val LockedGreyCard = Color(0xFF16161C)
    val LockedGreySurface = Color(0xFF22222B)
    val LockedGreyStroke = Color(0xFF323240)
    val LockedGreyIcon = Color(0xFF78788C)
    val LockedGreyText = Color(0xFF9E9EAF)

    val StreakGradient = Brush.linearGradient(
        colors = listOf(AmberStart, CoralEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    val ForestGradient = Brush.linearGradient(
        colors = listOf(ForestGreenPrimary, ForestGreenMedium)
    )

    val MintGradient = Brush.linearGradient(
        colors = listOf(MintAccent, Color(0xFF2E8B57))
    )

    val StudyCompletionGradient = Brush.linearGradient(
        colors = listOf(MintAccent, TealEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    fun studyVerticalAreaBrush(alpha: Float = 0.35f) = Brush.verticalGradient(
        colors = listOf(
            MintAccent.copy(alpha = alpha),
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
