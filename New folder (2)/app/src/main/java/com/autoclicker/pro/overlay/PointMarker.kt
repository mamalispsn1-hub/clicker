package com.autoclicker.pro.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A draggable crosshair used while the user is placing/adjusting a click point.
 * [onDrag] receives the incremental drag delta so the caller (the overlay
 * service, which owns the actual WindowManager LayoutParams for this view) can
 * update the on-screen position and the persisted X/Y percentage together.
 */
@Composable
fun PointMarker(
    label: String,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = Color(0xCC4F8CFF),
            modifier = Modifier
                .size(56.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    )
                }
        ) {}
        Surface(
            color = Color(0xE6141826),
            shape = CircleShape,
            modifier = Modifier.padding(top = 64.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
