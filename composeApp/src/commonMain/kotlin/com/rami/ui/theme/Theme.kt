package com.rami.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Brand palette (Tunisian café aesthetic) ──────────────────────────────────

object RamiColors {
    val FeltGreen    = Color(0xFF1B4332)   // table surface
    val DarkGreen    = Color(0xFF0A2016)   // background / app bars
    val TableSurface = Color(0xFF1E5C3A)   // slightly lighter felt
    val Gold         = Color(0xFFD4AF37)   // primary accent
    val LightGold    = Color(0xFFFFD700)   // highlights
    val CardWhite    = Color(0xFFFFFDF7)   // card face background
    val CardRed      = Color(0xFFD32F2F)   // hearts / diamonds
    val CardBlack    = Color(0xFF1A1A1A)   // clubs / spades
    val TextLight    = Color(0xFFF5F5DC)   // beige text on dark backgrounds
    val JokerPurple  = Color(0xFF9C27B0)   // joker star colour
}

// ─── Material colour scheme ───────────────────────────────────────────────────

private val RamiColorScheme = darkColorScheme(
    primary       = RamiColors.Gold,
    onPrimary     = RamiColors.DarkGreen,
    secondary     = RamiColors.LightGold,
    onSecondary   = RamiColors.DarkGreen,
    background    = RamiColors.DarkGreen,
    onBackground  = RamiColors.TextLight,
    surface       = RamiColors.TableSurface,
    onSurface     = RamiColors.TextLight,
    error         = Color(0xFFCF6679),
    onError       = Color.Black
)

@Composable
fun RamiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RamiColorScheme,
        content     = content
    )
}
