package org.schabi.newpipe.fragments.detail.compare

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
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
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val n = dimensions.size
                        val slot = size.width.toFloat() / n
                        val drawTop = topPaddingPx + circleRadius
                        val drawBottom = size.height - bottomPaddingPx - circleRadius
                        val i = (down.position.x / slot).toInt().coerceIn(0, n - 1)
                        activeIndex.intValue = i

                        // Wait for either touch slop (→ drag) or release (→ tap).
                        val slopChange = awaitTouchSlopOrCancellation(down.id) { c, _ ->
                            c.consume()
                        }
                        if (slopChange != null) {
                            // Drag: clear any pending single-tap so a subsequent
                            // tap on same bar isn't misread as double-tap.
                            lastTapTime.longValue = 0L
                            lastTapIndex.intValue = -1
                            val initial = yToScore(
                                slopChange.position.y,
                                drawTop,
                                drawBottom
                            )
                            setScoreAt(i, initial)
                            dragScore.value = initial
                            drag(slopChange.id) { change ->
                                val s = yToScore(
                                    change.position.y,
                                    drawTop,
                                    drawBottom
                                )
                                setScoreAt(i, s)
                                dragScore.value = s
                                change.consume()
                            }
                            dragScore.value = null
                        } else {
                            // Tap: handle single/double-tap reset.
                            val now = System.currentTimeMillis()
                            if (i == lastTapIndex.intValue &&
                                now - lastTapTime.longValue < 300L
                            ) {
                                setScoreAt(i, 0)
                                lastTapTime.longValue = 0L
                                lastTapIndex.intValue = -1
                            } else {
                                lastTapTime.longValue = now
                                lastTapIndex.intValue = i
                            }
                        }
                    }
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
