package com.rearch.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.rearch.launcher.ui.theme.*

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    currentTime: String,
    currentDate: String,
    batteryLevel: Int,
    isCharging: Boolean,
    networkStrength: Int,
    onSearchOpen: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Clock (left side, compact)
        Column(modifier = Modifier.wrapContentWidth()) {
            Text(currentTime, style = CaelestiaTypography.TitleMedium, color = CaelestiaColors.OnSurface.copy(alpha = 0.9f))
            Text(currentDate, style = CaelestiaTypography.BodySmall, color = CaelestiaColors.OnSurfaceMuted)
        }

        Spacer(Modifier.weight(1f))

        // Search bar pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CaelestiaColors.SurfaceOverlay)
                .clickable(onClick = onSearchOpen)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = CaelestiaColors.OnSurfaceMuted, modifier = Modifier.size(16.dp))
            Text("Apps & actions", style = CaelestiaTypography.BodySmall, color = CaelestiaColors.OnSurfaceMuted)
        }

        Spacer(Modifier.weight(1f))

        // Status icons row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Network
            Icon(
                imageVector = when {
                    networkStrength >= 4 -> Icons.Default.SignalCellular4Bar
                    networkStrength >= 3 -> Icons.Default.SignalCellular3Bar
                    networkStrength >= 2 -> Icons.Default.SignalCellular2Bar
                    networkStrength >= 1 -> Icons.Default.SignalCellular1Bar
                    else                 -> Icons.Default.SignalCellular0Bar
                },
                contentDescription = "Network",
                tint = CaelestiaColors.OnSurface.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            // Battery
            Icon(
                imageVector = if (isCharging) Icons.Default.BatteryChargingFull else {
                    when {
                        batteryLevel > 80 -> Icons.Default.BatteryFull
                        batteryLevel > 50 -> Icons.Default.Battery5Bar
                        batteryLevel > 20 -> Icons.Default.Battery3Bar
                        else              -> Icons.Default.Battery1Bar
                    }
                },
                contentDescription = "Battery $batteryLevel%",
                tint = if (batteryLevel < 15) Color(0xFFE53935) else CaelestiaColors.OnSurface.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Text("$batteryLevel%", style = CaelestiaTypography.LabelSmall, color = CaelestiaColors.OnSurfaceMuted)
        }
    }
}
