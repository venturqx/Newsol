package org.schabi.newpipe.fragments.detail.compare

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val hostView = LocalView.current
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
                        hostView.parent?.requestDisallowInterceptTouchEvent(true)
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

                drawLollipopHead(
                    center = Offset(cx, cy),
                    color = color,
                    score = score,
                    painter = painters[i],
                    dimensionId = dimensions[i].id,
                    selected = i == activeIndex.intValue,
                    circleRadius = circleRadius,
                    strokeWidth = strokeWidth,
                    iconSize = iconSize,
                    textSizePx = textSizePx,
                    textGap = textGap,
                    labelPaint = labelPaint
                )
            }
        }
        if (showDescription) {
            LollipopDescriptionRow(activeIndex = activeIndex.intValue)
        }
    }
}

@Composable
internal fun LollipopPickerWithIntro(
    pairKey: Any?,
    scores: Map<String, Int>,
    onScoreChange: (String, Int) -> Unit,
    mainScore: Int,
    onMainScoreChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hoistedActiveIndex: MutableIntState? = null,
    hoistedDragScore: MutableState<Int?>? = null,
    hoistedPhase: MutableIntState? = null,
    showDescription: Boolean = true,
    onSubmit1Request: ((Int, () -> Unit) -> Unit)? = null
) {
    val phase = hoistedPhase ?: remember { mutableIntStateOf(1) }
    LaunchedEffect(pairKey) { phase.intValue = 1 }

    val phase1ActiveIndex = hoistedActiveIndex ?: remember { mutableIntStateOf(-1) }
    if (phase.intValue == 1) {
        LargelyRecommendedSlider(
            mainScore = mainScore,
            onMainScoreChange = onMainScoreChange,
            scores = scores,
            activeIndex = phase1ActiveIndex,
            onSubmit1 = {
                if (onSubmit1Request != null) {
                    onSubmit1Request(mainScore) { phase.intValue = 2 }
                } else {
                    phase.intValue = 2
                }
            },
            modifier = modifier
        )
    } else {
        LollipopPicker(
            scores = scores,
            onScoreChange = onScoreChange,
            mainScore = mainScore,
            onMainScoreChange = onMainScoreChange,
            modifier = modifier,
            hoistedActiveIndex = hoistedActiveIndex,
            hoistedDragScore = hoistedDragScore,
            showDescription = showDescription
        )
    }
}

@Composable
private fun LargelyRecommendedSlider(
    mainScore: Int,
    onMainScoreChange: (Int) -> Unit,
    scores: Map<String, Int>,
    activeIndex: MutableIntState,
    onSubmit1: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val hostView = LocalView.current
    val blue = Color(0xFF4F9EFF)
    val red = Color(0xFFFF5A5F)
    val trackBg = Color.White.copy(alpha = 0.10f)
    val thumbColor = Color.White
    val neutralLabel = Color.White.copy(alpha = 0.55f)
    val trackHeightPx = with(density) { 6.dp.toPx() }
    val thumbWidthPx = with(density) { 4.dp.toPx() }
    val thumbHeightPx = with(density) { 22.dp.toPx() }
    val fillThumbGapPx = with(density) { 5.dp.toPx() }
    val sidePaddingPx = with(density) { 28.dp.toPx() }
    val canvasHeight = 32.dp
    val currentOnChange by rememberUpdatedState(onMainScoreChange)

    val qualifier = when {
        mainScore >= 70 -> "much more"
        mainScore >= 16 -> "slightly more"
        mainScore >= -15 -> "just as"
        mainScore >= -69 -> "slightly more"
        else -> "much more"
    }
    val leftArrow = when {
        mainScore <= -70 -> "<<"
        mainScore <= -16 -> "<"
        else -> ""
    }
    val rightArrow = when {
        mainScore >= 70 -> ">>"
        mainScore >= 16 -> ">"
        else -> ""
    }
    val scoreColor = when {
        mainScore < 0 -> blue
        mainScore > 0 -> red
        else -> neutralLabel
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Should be recommended..?",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
            Box(modifier = Modifier.width(48.dp)) {
                Text(
                    text = mainScore.toString(),
                    color = scoreColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        hostView.parent?.requestDisallowInterceptTouchEvent(true)
                        val left = sidePaddingPx
                        val right = size.width - sidePaddingPx
                        fun xToScore(x: Float): Int {
                            val ratio = ((x - left) / (right - left)).coerceIn(0f, 1f)
                            return (ratio * 200f - 100f)
                                .roundToInt()
                                .coerceIn(SCORE_MIN, SCORE_MAX)
                        }
                        currentOnChange(xToScore(down.position.x))
                        drag(down.id) { change ->
                            currentOnChange(xToScore(change.position.x))
                            change.consume()
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val left = sidePaddingPx
            val right = w - sidePaddingPx
            val centerX = (left + right) / 2f
            val cy = h / 2f
            val trackTop = cy - trackHeightPx / 2f
            val trackRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)

            // Background track
            drawRoundRect(
                color = trackBg,
                topLeft = Offset(left, trackTop),
                size = Size(right - left, trackHeightPx),
                cornerRadius = trackRadius
            )

            // Fill from center toward thumb, stopping short to leave a gap
            val ratio = (mainScore + 100) / 200f
            val thumbX = left + ratio * (right - left)
            if (mainScore != 0) {
                val fillColor = if (mainScore < 0) blue else red
                val rawFillLeft: Float
                val rawFillRight: Float
                if (mainScore < 0) {
                    rawFillLeft = thumbX + fillThumbGapPx
                    rawFillRight = centerX
                } else {
                    rawFillLeft = centerX
                    rawFillRight = thumbX - fillThumbGapPx
                }
                if (rawFillRight > rawFillLeft) {
                    clipRect(
                        left = rawFillLeft,
                        top = trackTop,
                        right = rawFillRight,
                        bottom = trackTop + trackHeightPx
                    ) {
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(left, trackTop),
                            size = Size(right - left, trackHeightPx),
                            cornerRadius = trackRadius
                        )
                    }
                }
            }

            // Thumb: tall white pill
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbX - thumbWidthPx / 2f, cy - thumbHeightPx / 2f),
                size = Size(thumbWidthPx, thumbHeightPx),
                cornerRadius = CornerRadius(thumbWidthPx / 2f, thumbWidthPx / 2f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text(
                text = leftArrow,
                color = blue,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(qualifier)
                    }
                    append(" recommended")
                },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            )
            Text(
                text = rightArrow,
                color = red,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            FilterChip(
                selected = false,
                onClick = { currentOnChange(0) },
                label = { Text(text = "Reset1") },
                colors = FilterChipDefaults.filterChipColors(),
                shape = RoundedCornerShape(8.dp),
                border = null
            )
            FilterChip(
                selected = true,
                onClick = onSubmit1,
                label = { Text(text = "Submit1") },
                colors = FilterChipDefaults.filterChipColors(),
                shape = RoundedCornerShape(8.dp),
                border = null
            )
        }
        LollipopHeadsRow(
            scores = scores,
            mainScore = mainScore,
            activeIndex = activeIndex,
            modifier = Modifier.padding(top = 4.dp)
        )
        LollipopDescriptionRow(activeIndex = activeIndex.intValue)
    }
}

private fun DrawScope.drawLollipopHead(
    center: Offset,
    color: Color,
    score: Int,
    painter: Painter,
    dimensionId: String,
    selected: Boolean,
    circleRadius: Float,
    strokeWidth: Float,
    iconSize: Float,
    textSizePx: Float,
    textGap: Float,
    labelPaint: AndroidPaint
) {
    val cx = center.x
    val cy = center.y
    // Selected outer ring (drawn first so head circle overdraws inner half,
    // leaving a halo flush with the border outer edge)
    if (selected) {
        drawCircle(
            color = color.copy(alpha = 160f / 255f),
            radius = circleRadius + 3.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 4.dp.toPx())
        )
    }
    drawCircle(
        color = Color(0xFF0F0F0F),
        radius = circleRadius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = color,
        radius = circleRadius,
        center = Offset(cx, cy),
        style = Stroke(width = strokeWidth)
    )

    val strokeInset = strokeWidth / 2f
    var halfW = iconSize / 2f - strokeInset
    var halfH = iconSize / 2f - strokeInset
    var iconOffsetY = 0f
    when (dimensionId) {
        "layman_friendly" -> {
            halfW *= 0.88f
            halfH *= 0.88f
        }

        "backfire_risk" -> {
            iconOffsetY = 1.dp.toPx()
        }
    }
    translate(left = cx - halfW, top = cy - halfH + iconOffsetY) {
        with(painter) {
            draw(size = Size(halfW * 2f, halfH * 2f))
        }
    }

    labelPaint.color = color.toArgb()
    val txt = score.toString()
    val labelY = if (score >= 0) {
        cy - circleRadius - textGap
    } else {
        cy + circleRadius + textSizePx + textGap * 0.3f
    }
    drawContext.canvas.nativeCanvas.drawText(txt, cx, labelY, labelPaint)
}

@Composable
private fun LollipopHead(
    painter: Painter,
    color: Color,
    score: Int,
    dimensionId: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val circleRadius = with(density) { 14.dp.toPx() }
    val strokeWidth = with(density) { 3.5.dp.toPx() }
    val iconSize = with(density) { 18.dp.toPx() }
    val textSizePx = with(density) { 10.dp.toPx() }
    val textGap = with(density) { 6.dp.toPx() }
    val labelPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
    labelPaint.textSize = textSizePx
    val interactionSource = remember { MutableInteractionSource() }
    Canvas(
        modifier = modifier
            .size(width = 36.dp, height = 54.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        drawLollipopHead(
            center = Offset(size.width / 2f, size.height / 2f),
            color = color,
            score = score,
            painter = painter,
            dimensionId = dimensionId,
            selected = selected,
            circleRadius = circleRadius,
            strokeWidth = strokeWidth,
            iconSize = iconSize,
            textSizePx = textSizePx,
            textGap = textGap,
            labelPaint = labelPaint
        )
    }
}

@Composable
private fun LollipopHeadsRow(
    scores: Map<String, Int>,
    mainScore: Int,
    activeIndex: MutableIntState,
    modifier: Modifier = Modifier
) {
    val mainCriterion = remember {
        CompareCriterion(
            id = COMPACT_MAIN_CRITERION_ID,
            labelRes = R.string.compare_criteria_largely_recommended,
            iconRes = R.drawable.logo_small
        )
    }
    // Row order: largely_recommended first, then the 10 extra criteria.
    // LollipopDescriptionRow indexes into (EXTRA_CRITERIA + main), so the
    // main head at row position 0 maps to description index EXTRA_CRITERIA.size.
    val rowDimensions = remember(mainCriterion) { listOf(mainCriterion) + EXTRA_CRITERIA }
    val mainDescIndex = EXTRA_CRITERIA.size
    val painters = rowDimensions.map { painterResource(it.iconRes) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        rowDimensions.forEachIndexed { rowIdx, dim ->
            val descIdx = if (rowIdx == 0) mainDescIndex else rowIdx - 1
            val score = if (dim.id == COMPACT_MAIN_CRITERION_ID) {
                mainScore
            } else {
                scores[dim.id] ?: 0
            }
            LollipopHead(
                painter = painters[rowIdx],
                color = LOLLIPOP_COLORS[dim.id] ?: Color.White,
                score = score,
                dimensionId = dim.id,
                selected = activeIndex.intValue == descIdx,
                onClick = { activeIndex.intValue = descIdx }
            )
        }
    }
}
