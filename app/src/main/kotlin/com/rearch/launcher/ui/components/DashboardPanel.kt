package com.rearch.launcher.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.rearch.launcher.model.MediaAction
import com.rearch.launcher.model.MediaState
import com.rearch.launcher.ui.theme.*

enum class DashboardTab(val label: String) {
    DASHBOARD("Dashboard"),
    MEDIA("Media"),
    PERFORMANCE("Performance"),
    WORKSPACES("Workspaces")
}

@Composable
fun DashboardPanel(
    selectedTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    mediaState: MediaState,
    onMediaAction: (MediaAction) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(640.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(28.dp),
        color = CaelestiaColors.SurfaceOverlay,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(0.dp)) {

            // ─── Tab Bar (Dashboard | Media | Performance | Workspaces) ───
            DashboardTabBar(
                selectedTab = selectedTab,
                onTabChange = onTabChange,
                onDismiss   = onDismiss
            )

            Divider(color = CaelestiaColors.OnSurface.copy(alpha = 0.08f), thickness = 1.dp)

            // ─── Tab Content ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 480.dp)
            ) {
                when (selectedTab) {
                    DashboardTab.DASHBOARD    -> DashboardHomeTab(mediaState, onMediaAction)
                    DashboardTab.MEDIA        -> MediaTab(mediaState, onMediaAction)
                    DashboardTab.PERFORMANCE  -> PerformanceTab()
                    DashboardTab.WORKSPACES   -> WorkspacesTab()
                }
            }
        }
    }
}

// ─── Tab bar with icons ─────────────────────────────────────────────────────
@Composable
private fun DashboardTabBar(
    selectedTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashboardTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Column(
                modifier = Modifier
                    .clickable { onTabChange(tab) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when (tab) {
                        DashboardTab.DASHBOARD   -> Icons.Default.Dashboard
                        DashboardTab.MEDIA       -> Icons.Default.MusicNote
                        DashboardTab.PERFORMANCE -> Icons.Default.Speed
                        DashboardTab.WORKSPACES  -> Icons.Default.ViewQuilt
                    },
                    contentDescription = tab.label,
                    tint = if (isSelected) CaelestiaColors.Accent else CaelestiaColors.OnSurfaceMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = tab.label,
                    style = CaelestiaTypography.LabelSmall,
                    color = if (isSelected) CaelestiaColors.Accent else CaelestiaColors.OnSurfaceMuted
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(24.dp)
                            .background(CaelestiaColors.Accent, CircleShape)
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

// ─── Media Tab (exact match to screenshot) ───────────────────────────────────
@Composable
fun MediaTab(
    mediaState: MediaState,
    onAction: (MediaAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Spinning album art ring (matches screenshot)
        SpinningAlbumArt(
            artUri = mediaState.albumArtUri,
            isPlaying = mediaState.isPlaying,
            modifier = Modifier.size(120.dp)
        )

        // Track info + controls
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = mediaState.title.ifEmpty { "No media playing" },
                style = CaelestiaTypography.TitleMedium,
                color = CaelestiaColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mediaState.artist.ifEmpty { "—" },
                style = CaelestiaTypography.BodySmall,
                color = CaelestiaColors.OnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = mediaState.album,
                style = CaelestiaTypography.BodySmall,
                color = CaelestiaColors.OnSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress bar
            Slider(
                value = if (mediaState.duration > 0) mediaState.position.toFloat() / mediaState.duration else 0f,
                onValueChange = { frac ->
                    onAction(MediaAction.Seek((frac * mediaState.duration).toLong()))
                },
                colors = SliderDefaults.colors(
                    thumbColor = CaelestiaColors.Accent,
                    activeTrackColor = CaelestiaColors.Accent,
                    inactiveTrackColor = CaelestiaColors.AccentContainer
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Time labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(mediaState.position), style = CaelestiaTypography.LabelSmall, color = CaelestiaColors.OnSurfaceMuted)
                Text(formatTime(mediaState.duration), style = CaelestiaTypography.LabelSmall, color = CaelestiaColors.OnSurfaceMuted)
            }

            // Playback controls row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onAction(MediaAction.Previous) }) {
                    Icon(Icons.Default.SkipPrevious, null, tint = CaelestiaColors.OnSurface, modifier = Modifier.size(28.dp))
                }
                // Main play/pause — coral pill button matching screenshot
                FilledIconButton(
                    onClick = { onAction(if (mediaState.isPlaying) MediaAction.Pause else MediaAction.Play) },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = CaelestiaColors.Accent),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { onAction(MediaAction.Next) }) {
                    Icon(Icons.Default.SkipNext, null, tint = CaelestiaColors.OnSurface, modifier = Modifier.size(28.dp))
                }
            }

            // Source badges (Feishin / Spotify — matching screenshot)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                mediaState.sources.forEach { source ->
                    SourceBadge(source)
                }
            }
        }
    }
}

// ─── Spinning vinyl ring (the animated album art circle from screenshot) ────
@Composable
fun SpinningAlbumArt(
    artUri: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "albumRotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer dashed/dotted ring (matches screenshot aesthetic)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (isPlaying) rotation else 0f)
        ) {
            val strokeWidth = 4.dp.toPx()
            val radius = (size.minDimension / 2f) - strokeWidth
            drawCircle(
                color = Color(0xFF8B6A6A),
                radius = radius,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(6f, 4f), 0f
                    )
                )
            )
            // Inner accent ring
            drawCircle(
                color = Color(0xFFE07B6A).copy(alpha = 0.3f),
                radius = radius - 8.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Album art in center circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(CaelestiaColors.SurfaceBase)
        ) {
            if (artUri != null) {
                AsyncImage(
                    model = artUri,
                    contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = CaelestiaColors.OnSurfaceMuted,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )
            }
        }
    }
}

// ─── Source badge (Feishin / Spotify) ───────────────────────────────────────
@Composable
private fun SourceBadge(source: String) {
    val (bgColor, textColor) = when (source.lowercase()) {
        "spotify"  -> CaelestiaColors.SpotifyGreen.copy(alpha = 0.15f) to CaelestiaColors.SpotifyGreen
        "feishin"  -> CaelestiaColors.FeishinPink.copy(alpha = 0.15f)  to CaelestiaColors.FeishinPink
        else       -> CaelestiaColors.SurfaceElevated to CaelestiaColors.OnSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = source,
            style = CaelestiaTypography.LabelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ─── Dashboard Home Tab ──────────────────────────────────────────────────────
@Composable
private fun DashboardHomeTab(mediaState: MediaState, onAction: (MediaAction) -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Mini media widget in dashboard tab
        Text("Now Playing", style = CaelestiaTypography.TitleMedium, color = CaelestiaColors.OnSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SpinningAlbumArt(artUri = mediaState.albumArtUri, isPlaying = mediaState.isPlaying, modifier = Modifier.size(64.dp))
            Column {
                Text(mediaState.title.ifEmpty { "—" }, style = CaelestiaTypography.BodyLarge, color = CaelestiaColors.OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(mediaState.artist, style = CaelestiaTypography.BodySmall, color = CaelestiaColors.OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ─── Performance Tab ─────────────────────────────────────────────────────────
@Composable
private fun PerformanceTab() {
    val cpu by remember { mutableFloatStateOf(0f) } // populated by PerformanceViewModel
    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Performance", style = CaelestiaTypography.TitleMedium, color = CaelestiaColors.OnSurface)
        PerformanceCard(label = "CPU", value = cpu)
        PerformanceCard(label = "Memory", value = 0f)
        PerformanceCard(label = "Network ↓↑", value = 0f)
    }
}

@Composable
private fun PerformanceCard(label: String, value: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = CaelestiaTypography.BodySmall, color = CaelestiaColors.OnSurfaceVariant)
            Text("${(value * 100).toInt()}%", style = CaelestiaTypography.BodySmall, color = CaelestiaColors.Accent)
        }
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier.fillMaxWidth(),
            color = CaelestiaColors.Accent,
            trackColor = CaelestiaColors.AccentContainer
        )
    }
}

// ─── Workspaces Tab ──────────────────────────────────────────────────────────
@Composable
private fun WorkspacesTab() {
    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Workspaces", style = CaelestiaTypography.TitleMedium, color = CaelestiaColors.OnSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            (1..4).forEach { i ->
                WorkspaceCard(index = i, isActive = i == 1)
            }
        }
    }
}

@Composable
private fun WorkspaceCard(index: Int, isActive: Boolean) {
    Surface(
        modifier = Modifier.size(80.dp, 56.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) CaelestiaColors.AccentContainer else CaelestiaColors.SurfaceBase,
        border = if (isActive) BorderStroke(2.dp, CaelestiaColors.Accent) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$index", style = CaelestiaTypography.TitleMedium, color = if (isActive) CaelestiaColors.Accent else CaelestiaColors.OnSurfaceMuted)
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
