package com.rearch.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.rearch.launcher.ui.theme.*

// ─── Wallpaper ──────────────────────────────────────────────────────────────
@Composable
fun WallpaperLayer(wallpaperUri: String?) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (wallpaperUri != null) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Default wallpaper: warm cream — matches Caelestia default
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F0EB))
            )
        }
    }
}

// ─── Desktop Clock (bottom-right of screenshot: "21\n40") ──────────────────
@Composable
fun DesktopClock(
    modifier: Modifier = Modifier,
    time: String,
    date: String
) {
    val parts = time.split(":")
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Hour large
        Text(
            text = parts.getOrElse(0) { "00" },
            style = CaelestiaTypography.DisplayLarge.copy(
                fontSize = 72.sp,
                color = CaelestiaColors.OnSurface.copy(alpha = 0.85f)
            )
        )
        // Minute large
        Text(
            text = parts.getOrElse(1) { "00" },
            style = CaelestiaTypography.DisplayLarge.copy(
                fontSize = 72.sp,
                color = CaelestiaColors.OnSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = date,
            style = CaelestiaTypography.BodySmall,
            color = CaelestiaColors.OnSurfaceMuted
        )
    }
}
