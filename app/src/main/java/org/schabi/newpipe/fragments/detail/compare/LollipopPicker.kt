package org.schabi.newpipe.fragments.detail.compare

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.schabi.newpipe.R
import org.schabi.newpipe.fragments.detail.COMPACT_MAIN_CRITERION_ID
import org.schabi.newpipe.fragments.detail.CompareCriterion
import org.schabi.newpipe.fragments.detail.EXTRA_CRITERIA
import org.schabi.newpipe.fragments.detail.LOLLIPOP_COLORS
import org.schabi.newpipe.fragments.detail.LollipopDescriptionRow
import org.schabi.newpipe.fragments.detail.SCORE_MAX
import org.schabi.newpipe.fragments.detail.SCORE_MIN

@Composable
internal fun LollipopPicker(
    scores: Map<String, Int>,
    onScoreChange: (String, Int) -> Unit,
    mainScore: Int,
    onMainScoreChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hoistedActiveIndex: MutableIntState? = null,
    hoistedDragScore: MutableState<Int?>? = null,
    showDescription: Boolean = true
) {
    val density = LocalDensity.current
    val dimensions = remember {
        EXTRA_CRITERIA + CompareCriterion(
            id = COMPACT_MAIN_CRITERION_ID,
            labelRes = R.string.compare_criteria_largely_recommended,
            iconRes = R.drawable.logo_small
        )
    }
    val painters = dimensions.map { painterResource(it.iconRes) }
    val colors = dimensions.map { LOLLIPOP_COLORS[it.id] ?: Color.White }
    fun scoreAt(i: Int): Int = if (dimensions[i].id == COMPACT_MAIN_CRITERION_ID) {
        mainScore
    } else {
        scores[dimensions[i].id] ?: 0
    }
    fun setScoreAt(i: Int, value: Int) {
        if (dimensions[i].id == COMPACT_MAIN_CRITERION_ID) {
            onMainScoreChange(value)
        } else {
            onScoreChange(dimensions[i].id, value)
        }
    }
    val circleRadius = with(density) { 14.dp.toPx() }
    val barWidth = with(density) { 12.dp.toPx() }
    val iconSize = with(density) { 18.dp.toPx() }
    val strokeWidth = with(density) { 3.5.dp.toPx() }
    val textSizePx = with(density) { 10.dp.toPx() }
    val textGap = with(density) { 6.dp.toPx() }
    val topPaddingPx = with(density) { 22.dp.toPx() }
    val bottomPaddingPx = with(density) { 22.dp.toPx() }
    val totalHeight = 160.dp
    val activeIndex = hoistedActiveIndex ?: remember { mutableIntStateOf(-1) }
    val dragScore = hoistedDragScore ?: remember { mutableStateOf<Int?>(null) }
    val lastTapTime = remember { mutableLongStateOf(0L) }
    val lastTapIndex = remember { mutableIntStateOf(-1) }
    val currentOnScoreChange by rememberUpdatedState(onScoreChange)
    val currentOnMainScoreChange by rememberUpdatedState(onMainScoreChange)
    val labelPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    labelPaint.textSize = textSizePx

    fun yToScore(y: Float, drawTop: Float, drawBottom: Float): Int {
        val ratio = ((drawBottom - y) / (drawBottom - drawTop)).coerceIn(0f, 1f)
        return (ratio * 200f - 100f).roundToInt().coerceIn(SCORE_MIN, SCORE_MAX)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .pointerInput("doubleTap") {
                    detectTapGestures(
                        onPress = { offset ->
                            val n = dimensions.size
                            val slot = size.width.toFloat() / n
                            val i = (offset.x / slot).toInt().coerceIn(0, n - 1)
                            activeIndex.intValue = i
                            val now = System.currentTimeMillis()
                            if (i == lastTapIndex.intValue &&
                                now - lastTapTime.longValue < 300L
                            ) {
                                if (dimensions[i].id == COMPACT_MAIN_CRITERION_ID) {
                                    currentOnMainScoreChange(0)
                                } else {
                                    currentOnScoreChange(dimensions[i].id, 0)
                                }
                                lastTapTime.longValue = 0L
                                lastTapIndex.intValue = -1
                            } else {
                                lastTapTime.longValue = now
                                lastTapIndex.intValue = i
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    val n = dimensions.size
                    detectDragGestures(
                        onDragStart = { offset ->
                            val w = size.width.toFloat()
                            val slot = w / n
                            val drawTop = topPaddingPx + circleRadius
                            val drawBottom = size.height - bottomPaddingPx - circleRadius
                            var found = -1
                            for (i in 0 until n) {
                                val cx = slot * (i + 0.5f)
                                val score = scoreAt(i)
                                val cy = drawBottom -
                                    ((score + 100) / 200f) * (drawBottom - drawTop)
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val r = circleRadius + with(density) { 12.dp.toPx() }
                                if (dx * dx + dy * dy <= r * r) {
                                    found = i
                                    break
                                }
                            }
                            val hitHead = found >= 0
                            if (found < 0) {
                                // Fall back to nearest column horizontally
                                found = (offset.x / slot).toInt().coerceIn(0, n - 1)
                            }
                            val now = System.currentTimeMillis()
                            val isDoubleTap = hitHead &&
                                found == lastTapIndex.intValue &&
                                now - lastTapTime.longValue < 300L
                            if (isDoubleTap) {
                                setScoreAt(found, 0)
                                lastTapTime.longValue = 0L
                                lastTapIndex.intValue = -1
                                // Prevent subsequent onDrag from overwriting
                                activeIndex.intValue = -1
                                dragScore.value = null
                            } else {
                                lastTapTime.longValue = if (hitHead) now else 0L
                                lastTapIndex.intValue = if (hitHead) found else -1
                                activeIndex.intValue = found
                                val newScore = yToScore(offset.y, drawTop, drawBottom)
                                setScoreAt(found, newScore)
                                dragScore.value = newScore
                            }
                        },
                        onDrag = { change, _ ->
                            change.consumeAllChanges()
                            val i = activeIndex.intValue
                            if (i >= 0) {
                                val drawTop = topPaddingPx + circleRadius
                                val drawBottom = size.height - bottomPaddingPx - circleRadius
                                val newScore = yToScore(change.position.y, drawTop, drawBottom)
                                setScoreAt(i, newScore)
                                dragScore.value = newScore
                            }
                        },
                        onDragEnd = { dragScore.value = null },
                        onDragCancel = { dragScore.value = null }
                    )
                }
        ) {
            val n = dimensions.size
            val w = size.width
            val h = size.height
            val drawTop = topPaddingPx + circleRadius
            val drawBottom = h - bottomPaddingPx - circleRadius
            val zeroY = (drawTop + drawBottom) / 2f
            val slotWidth = w / n

            // Zero line
            drawLine(
                color = Color.White.copy(alpha = 0.25f),
                start = Offset(slotWidth * 0.3f, zeroY),
                end = Offset(w - slotWidth * 0.3f, zeroY),
                strokeWidth = with(density) { 1.dp.toPx() }
            )

            for (i in 0 until n) {
                val score = scoreAt(i)
                val color = colors[i]
                val cx = slotWidth * (i + 0.5f)
                val cy = drawBottom - ((score + 100) / 200f) * (drawBottom - drawTop)

                // Bar
                drawLine(
                    color = color,
                    start = Offset(cx, zeroY),
                    end = Offset(cx, cy),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Butt
                )

                // Selected outer ring (drawn first so head circle overdraws inner half,
                // leaving a halo flush with the border outer edge)
                if (i == activeIndex.intValue) {
                    drawCircle(
                        color = color.copy(alpha = 160f / 255f),
                        radius = circleRadius + with(density) { 3.dp.toPx() },
                        center = Offset(cx, cy),
                        style = Stroke(width = with(density) { 4.dp.toPx() })
                    )
                }
                // Background fill of circle
                drawCircle(
                    color = Color(0xFF0F0F0F),
                    radius = circleRadius,
                    center = Offset(cx, cy)
                )
                // Stroke
                drawCircle(
                    color = color,
                    radius = circleRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeWidth)
                )

                // Icon (inset by half the stroke so it sits inside the border)
                val painter = painters[i]
                val strokeInset = strokeWidth / 2f
                var halfW = iconSize / 2f - strokeInset
                var halfH = iconSize / 2f - strokeInset
                var iconOffsetY = 0f
                when (dimensions[i].id) {
                    "layman_friendly" -> {
                        halfW *= 0.88f
                        halfH *= 0.88f
                    }

                    "backfire_risk" -> {
                        iconOffsetY = with(density) { 1.dp.toPx() }
                    }
                }
                translate(left = cx - halfW, top = cy - halfH + iconOffsetY) {
                    with(painter) {
                        draw(size = Size(halfW * 2f, halfH * 2f))
                    }
                }

                // Score label
                labelPaint.color = color.toArgb()
                val txt = score.toString()
                val labelY = if (score >= 0) {
                    cy - circleRadius - textGap
                } else {
                    cy + circleRadius + textSizePx + textGap * 0.3f
                }
                drawContext.canvas.nativeCanvas.drawText(txt, cx, labelY, labelPaint)
            }
        }
        if (showDescription) {
            LollipopDescriptionRow(activeIndex = activeIndex.intValue)
        }
    }
}
