package org.schabi.newpipe.fragments.detail.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.schabi.newpipe.fragments.detail.SCORE_MAX

@Composable
internal fun CompactSwipeArea(
    value: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactScoreTrack(value = value)
        }
    }
}

@Composable
internal fun CompactScoreTrack(
    value: Int,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val fillColor = MaterialTheme.colorScheme.primary
    val centerLine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val trackHeight = size.height * 0.35f
        val centerY = size.height / 2f
        val radius = trackHeight / 2f
        val top = centerY - trackHeight / 2f
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, top),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(radius, radius)
        )
        drawLine(
            color = centerLine,
            start = Offset(size.width / 2f, top),
            end = Offset(size.width / 2f, top + trackHeight),
            strokeWidth = 1.dp.toPx()
        )
        val ratio = value.toFloat() / SCORE_MAX.toFloat()
        val fillWidth = (size.width / 2f) * abs(ratio)
        if (fillWidth > 0f) {
            val startX = if (ratio >= 0f) size.width / 2f else size.width / 2f - fillWidth
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(startX, top),
                size = Size(fillWidth, trackHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}
