package org.schabi.newpipe.fragments.detail.compare

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.delay
import org.schabi.newpipe.R
import org.schabi.newpipe.fragments.detail.CompareCriterion
import org.schabi.newpipe.fragments.detail.MiniScoreBar

private const val WEEKLY_GOAL = 2500
private const val DAILY_GOAL = 2

@Composable
private fun GoalRing(
    current: Int,
    goal: Int,
    label: String,
    ringColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val progress = current.toFloat() / goal
    val emoji = when {
        progress > 1.25f -> "\u2764\uFE0F\u200D\uD83D\uDD25"
        progress > 1f -> "\uD83E\uDD73\uD83C\uDF89"
        progress > 0.75f -> "\uD83C\uDF3B"
        progress > 0.5f -> "\uD83C\uDF37"
        progress > 0.25f -> "\uD83C\uDF40"
        else -> "\uD83C\uDF31"
    }

    val scale = LocalCompareScale.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "goalRing"
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(44.dp * scale)) {
                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = stroke
                )
            }
            Text(
                text = emoji,
                fontSize = 16.sp * scale
            )
        }
        Spacer(modifier = Modifier.height(2.dp * scale))
        Text(
            text = "$current / $goal",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 12.sp * scale,
            lineHeight = 14.sp * scale
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            fontSize = 11.sp * scale,
            lineHeight = 13.sp * scale
        )
    }
}

@Composable
internal fun UserGreetingBanner(
    username: String?,
    comparisonCount: Int?,
    weeklyComparisons: Int?,
    dailyComparisons: Int?,
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    val scale = LocalCompareScale.current
    if (username != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp * scale),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.compare_greeting, username),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (comparisonCount != null) {
                    Text(
                        text = stringResource(R.string.compare_greeting_comparisons, comparisonCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (dailyComparisons != null) {
                    GoalRing(
                        current = dailyComparisons,
                        goal = DAILY_GOAL,
                        label = "Your daily",
                        ringColor = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (weeklyComparisons != null) {
                    GoalRing(
                        current = weeklyComparisons,
                        goal = WEEKLY_GOAL,
                        label = "Weekly collective"
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp * scale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.compare_login_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp * scale)) {
                Button(
                    onClick = onLogin,
                    contentPadding = PaddingValues(horizontal = 16.dp * scale, vertical = 6.dp * scale)
                ) {
                    Text(text = stringResource(R.string.tournesol_login_button))
                }
                OutlinedButton(
                    onClick = onRegister,
                    contentPadding = PaddingValues(horizontal = 16.dp * scale, vertical = 6.dp * scale)
                ) {
                    Text(text = stringResource(R.string.tournesol_register_button))
                }
            }
        }
    }
}

@Composable
internal fun CompactInitialPromptHeader(
    modifier: Modifier = Modifier
) {
    val scale = LocalCompareScale.current
    val template = stringResource(R.string.compare_compact_initial_prompt)
    val parts = template.split("%1\$s", limit = 2)
    val prefix = parts.getOrNull(0).orEmpty()
    val suffix = parts.getOrNull(1).orEmpty()
    val textStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Start
            )
        }
        Image(
            painter = painterResource(R.drawable.logo_small),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 4.dp * scale)
                .size(16.dp * scale)
        )
        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Start,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun CompactHeader(
    description: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    val scale = LocalCompareScale.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp * scale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp * scale)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Start,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun CompactDimensionRow(
    criterion: CompareCriterion,
    criterionLabel: String,
    score: Int,
    isActive: Boolean,
    isSelected: Boolean,
    isMainCriterion: Boolean,
    showMainAttention: Boolean,
    rowAlpha: Float,
    firstColumnWidth: androidx.compose.ui.unit.Dp,
    onFirstColumnMeasured: (Int) -> Unit,
    onToggleSelected: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val scale = LocalCompareScale.current
    val highlight = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    val textColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlight, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(
                horizontal = 10.dp * scale,
                vertical = if (isMainCriterion) 3.dp * scale else 1.dp * scale
            )
            .graphicsLayer(alpha = rowAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = firstColumnWidth)
                .onGloballyPositioned { coordinates ->
                    onFirstColumnMeasured(coordinates.size.width)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp * scale)
                    .padding(end = 4.dp * scale)
                    .then(
                        if (isSelected) Modifier.clickable { onToggleSelected(false) } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Image(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp * scale)
                    )
                }
            }
            Image(
                painter = painterResource(criterion.iconRes),
                contentDescription = null,
                modifier = Modifier.size(21.dp * scale)
            )
            Spacer(modifier = Modifier.width(6.dp * scale))
            Text(
                text = criterionLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 13.5.sp * scale,
                    lineHeight = 15.sp * scale,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }

        val valueText =
            if (score == 0 && !isSelected && !isActive) "" else formatScoreMagnitude(score)
        val scoreColor = when {
            score > 0 -> Color(0xFFE57373)
            score < 0 -> Color(0xFF64B5F6)
            else -> textColor.copy(alpha = 0.7f)
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp * scale,
                    lineHeight = 10.sp * scale
                ),
                color = scoreColor,
                textAlign = TextAlign.End,
                modifier = Modifier.width(24.dp * scale)
            )
            Spacer(modifier = Modifier.width(6.dp * scale))
            MiniScoreBar(
                value = score,
                isActive = isActive,
                barHeight = if (isMainCriterion) 5.dp * scale else 3.dp * scale,
                showAttentionRing = showMainAttention,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}

@Composable
internal fun SwipeHintWaveText(
    modifier: Modifier = Modifier
) {
    val text = stringResource(R.string.compare_swipe_hint)
    var waveCenter by remember { mutableFloatStateOf(-2f) }
    LaunchedEffect(text) {
        while (true) {
            waveCenter += 0.18f
            if (waveCenter > text.length + 2f) {
                waveCenter = -2f
            }
            delay(28)
        }
    }
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)
    val waveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, char ->
            if (char == ' ') {
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                val distance = abs(waveCenter - index.toFloat())
                val glow = (1f - distance / 2.4f).coerceIn(0f, 1f)
                val animatedColor = lerp(baseColor, waveColor, glow * 0.7f)
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = animatedColor
                )
            }
        }
    }
}
