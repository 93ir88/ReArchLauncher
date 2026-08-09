package com.rearch.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.*
import com.rearch.launcher.ui.theme.*

@Composable
fun LeftSidebar(
    modifier: Modifier = Modifier,
    onDashboardToggle: () -> Unit,
    currentWorkspace: Int,
    onWorkspaceChange: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .background(CaelestiaColors.SurfaceOverlay.copy(alpha = 0.6f))
            .navigationBarsPadding()
            .statusBarsPadding()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Arch logo / launcher icon (top)
        SidebarIcon(icon = Icons.Default.Apps, onClick = onDashboardToggle, tint = CaelestiaColors.Accent)

        Spacer(Modifier.height(8.dp))

        // Navigation icons (matching left sidebar in screenshot)
        SidebarIcon(icon = Icons.Default.ArrowBack, onClick = { /* navigate back */ })
        SidebarIcon(icon = Icons.Default.Refresh, onClick = { /* reload */ })
        SidebarIcon(icon = Icons.Default.FormatListBulleted, onClick = { /* tasks */ })
        SidebarIcon(icon = Icons.Default.Sync, onClick = { /* sync */ })
        SidebarIcon(icon = Icons.Default.MoreVert, onClick = { /* more */ })

        Spacer(Modifier.weight(1f))

        // Workspace indicators (vertical pill dots matching screenshot)
        WorkspaceIndicators(
            count = 4,
            current = currentWorkspace,
            onSelect = onWorkspaceChange
        )

        Spacer(Modifier.height(8.dp))

        // Bottom system icons
        SidebarIcon(icon = Icons.Default.Bluetooth, onClick = { })
        SidebarIcon(icon = Icons.Default.Wifi, onClick = { })
        SidebarIcon(icon = Icons.Default.VolumeUp, onClick = { })
        SidebarIcon(icon = Icons.Default.Lock, onClick = { })
        SidebarIcon(icon = Icons.Default.PowerSettingsNew, onClick = { })
    }
}

@Composable
private fun SidebarIcon(
    icon: ImageVector,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = CaelestiaColors.OnSurfaceVariant
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun WorkspaceIndicators(count: Int, current: Int, onSelect: (Int) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(count) { i ->
            val idx = i + 1
            val isActive = idx == current
            Box(
                modifier = Modifier
                    .width(if (isActive) 6.dp else 4.dp)
                    .height(if (isActive) 16.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> CaelestiaColors.WorkspaceActive
                            else     -> CaelestiaColors.WorkspaceEmpty
                        }
                    )
                    .clickable { onSelect(idx) }
            )
        }
    }
}
