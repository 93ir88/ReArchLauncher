package com.rearch.launcher.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.rearch.launcher.model.OpenWindow
import com.rearch.launcher.ui.theme.*

/**
 * Freeform window layer — composable windows that sit on the launcher desktop.
 * Each window is draggable, resizable, closeable.
 * Actual app content is rendered via Android ActivityEmbedding / freeform mode (root).
 */
@Composable
fun FreeformWindowLayer(
    windows: List<OpenWindow>,
    onWindowMove: (id: String, dx: Float, dy: Float) -> Unit,
    onWindowResize: (id: String, dw: Float, dh: Float) -> Unit,
    onWindowClose: (id: String) -> Unit,
    onWindowFocus: (id: String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        windows.forEachIndexed { index, window ->
            FreeformWindowChrome(
                window = window,
                zOrder = index,
                onMove   = { dx, dy -> onWindowMove(window.id, dx, dy) },
                onResize = { dw, dh -> onWindowResize(window.id, dw, dh) },
                onClose  = { onWindowClose(window.id) },
                onFocus  = { onWindowFocus(window.id) }
            )
        }
    }
}

@Composable
private fun FreeformWindowChrome(
    window: OpenWindow,
    zOrder: Int,
    onMove: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit,
    onClose: () -> Unit,
    onFocus: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(window.x) }
    var offsetY by remember { mutableFloatStateOf(window.y) }
    var width   by remember { mutableFloatStateOf(window.width) }
    var height  by remember { mutableFloatStateOf(window.height) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
            .size(width.dp, height.dp)
            .zIndex(zOrder.toFloat())
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, CaelestiaColors.WindowBorder, RoundedCornerShape(16.dp))
            .background(CaelestiaColors.SurfaceBase)
            .pointerInput(window.id) { detectTapGestures(onTap = { onFocus() }) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Title bar (drag handle) ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(CaelestiaColors.SurfaceElevated)
                    .pointerInput(window.id) {
                        detectDragGestures { _, drag ->
                            offsetX += drag.x
                            offsetY += drag.y
                            onMove(drag.x, drag.y)
                        }
                    }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Window controls (traffic lights — close/minimize)
                Box(
                    modifier = Modifier.size(12.dp)
                        .background(Color(0xFFE07B6A), androidx.compose.foundation.shape.CircleShape)
                        .clickable { onClose() }
                )
                Box(
                    modifier = Modifier.size(12.dp)
                        .background(Color(0xFFE8C85A), androidx.compose.foundation.shape.CircleShape)
                )
                Box(
                    modifier = Modifier.size(12.dp)
                        .background(Color(0xFF5AC88A), androidx.compose.foundation.shape.CircleShape)
                )

                Spacer(Modifier.width(8.dp))
                Text(
                    text = window.appName,
                    style = CaelestiaTypography.LabelSmall,
                    color = CaelestiaColors.OnSurfaceVariant,
                    maxLines = 1
                )
            }

            // ── App content area ─────────────────────────────────────────
            // In a real freeform scenario the app renders inside Android's own
            // window system; this surface acts as a visual chrome placeholder
            // and click target.  The actual Activity launch is handled by
            // FreeformWindowHelper (root) which positions the real window
            // to match these coords.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CaelestiaColors.SurfaceBase.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Icon + label fallback while the real window loads
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (window.icon != null) {
                        Image(
                            bitmap = window.icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    } else {
                        Icon(Icons.Default.Android, null, tint = CaelestiaColors.OnSurfaceMuted, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(window.appName, style = CaelestiaTypography.BodySmall, color = CaelestiaColors.OnSurfaceMuted)
                }
            }
        }

        // ── Resize handle (bottom-right corner) ─────────────────────────
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.BottomEnd)
                .pointerInput(window.id) {
                    detectDragGestures { _, drag ->
                        width  = (width  + drag.x).coerceAtLeast(200f)
                        height = (height + drag.y).coerceAtLeast(150f)
                        onResize(drag.x, drag.y)
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Resize",
                tint = CaelestiaColors.OnSurfaceMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp).align(Alignment.Center)
            )
        }
    }
}
