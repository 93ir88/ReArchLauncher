package com.rearch.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rearch.launcher.R

// ─── Caelestia color system (from shell source) ───────────────────────────
object CaelestiaColors {
    // Surface / background cards
    val SurfaceBase        = Color(0xFFF5F0EB)   // warm cream
    val SurfaceElevated    = Color(0xFFFFFFFF)
    val SurfaceOverlay     = Color(0xE6F5F0EB)   // semi-transparent card
    val SurfacePopup       = Color(0xF2FAFAF5)

    // Text
    val OnSurface          = Color(0xFF1A1A1A)
    val OnSurfaceVariant   = Color(0xFF5A5A5A)
    val OnSurfaceMuted     = Color(0xFF8A8A8A)

    // Accent — the coral/salmon pink from the UI
    val Accent             = Color(0xFFE07B6A)   // pause button color
    val AccentVariant      = Color(0xFFD4A5A0)
    val AccentContainer    = Color(0xFFF5DDD9)

    // Secondary — the muted blue-gray
    val Secondary          = Color(0xFF7A9BB5)
    val SecondaryContainer = Color(0xFFD0E4F0)

    // Spotify green
    val SpotifyGreen       = Color(0xFF1DB954)

    // Feishin pink
    val FeishinPink        = Color(0xFFE8A4B8)

    // Workspace indicators
    val WorkspaceActive    = Color(0xFFE07B6A)
    val WorkspaceOccupied  = Color(0xFFD4A5A0)
    val WorkspaceEmpty     = Color(0x66AAAAAA)

    // Window chrome
    val WindowBorder       = Color(0x33E07B6A)
    val WindowShadow       = Color(0x44000000)

    // Overlay backdrop
    val Scrim              = Color(0x55000000)
}

// ─── Typography (Google Sans Flex, matching Caelestia shell) ──────────────
// Font loaded from assets matching caelestia/shell/assets/google-sans-flex/
val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Normal),
    Font(R.font.google_sans_flex_medium, FontWeight.Medium),
    Font(R.font.google_sans_flex_bold, FontWeight.Bold),
)

object CaelestiaTypography {
    val DisplayLarge  = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 36.sp)
    val TitleLarge    = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 24.sp)
    val TitleMedium   = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp)
    val BodyLarge     = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp)
    val BodySmall     = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val LabelSmall    = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
    val Clock         = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold,   fontSize = 28.sp, lineHeight = 32.sp)
}

// ─── Material3 color scheme (light — matches Caelestia default theme) ─────
private val LightColorScheme = lightColorScheme(
    primary          = CaelestiaColors.Accent,
    onPrimary        = Color.White,
    primaryContainer = CaelestiaColors.AccentContainer,
    secondary        = CaelestiaColors.Secondary,
    secondaryContainer = CaelestiaColors.SecondaryContainer,
    surface          = CaelestiaColors.SurfaceBase,
    onSurface        = CaelestiaColors.OnSurface,
    surfaceVariant   = CaelestiaColors.SurfaceElevated,
    background       = Color.Transparent,
    scrim            = CaelestiaColors.Scrim,
)

@Composable
fun ReArchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
