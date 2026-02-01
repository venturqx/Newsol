package org.schabi.newpipe.fragments.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.schabi.newpipe.R
import org.schabi.newpipe.database.history.model.StreamHistoryEntry
import org.schabi.newpipe.ui.components.items.stream.StreamThumbnail
import kotlin.math.abs
import kotlin.math.roundToInt
import android.graphics.Paint as AndroidPaint

@Composable
fun CompareScreen(
    state: CompareUiState,
    onSelectIndex: (Int) -> Unit,
    onScoreChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    onChangeMainScore: () -> Unit,
    onExtraScoreChange: (String, Int) -> Unit,
    onSubmitMore: () -> Unit,
    onDismissLogin: () -> Unit,
    onRegister: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    val currentEntry = state.selectedEntry
    val scrollState = rememberScrollState()
    val nestedScrollInterop = rememberNestedScrollInteropConnection()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollInterop)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFFE57373))) {
                    append("\u2191 Video A")
                }
                append(" should be more recommended than ")
                withStyle(SpanStyle(color = Color(0xFF64B5F6))) {
                    append("Video B \u2193")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 3.dp)
        )

        HistoryWheel(
            entries = state.historyEntries,
            selectedIndex = state.selectedIndex,
            historyMessageRes = state.historyMessageRes,
            onSelectIndex = onSelectIndex
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Video A",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE57373)
            )
            Text(
                text = "Video B",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64B5F6)
            )
        }

        ThickScoreSlider(
            value = state.score,
            onValueChange = onScoreChange,
            modifier = Modifier.fillMaxWidth()
        )

        val submitEnabled = currentEntry != null && !state.submitted && !state.submitInProgress
        val showChange = state.submittedConfirmed
        val changeEnabled = showChange &&
            currentEntry != null &&
            !state.changeInProgress &&
            (state.storedMainScore == null || state.score != state.storedMainScore)
        val submitLabel = stringResource(
            if (state.submitInProgress) {
                R.string.compare_submitting_label
            } else if (state.submitted) {
                R.string.compare_submitted_label
            } else {
                R.string.compare_submit_label
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable(enabled = submitEnabled) { onSubmit() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = submitLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (submitEnabled) {
                        lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (state.submitInProgress) {
                    SubmitSpinner(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.logo_small),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (showChange) {
                Spacer(modifier = Modifier.width(14.dp))
                Row(
                    modifier = Modifier.clickable(enabled = changeEnabled) { onChangeMainScore() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.compare_change_label),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (changeEnabled) {
                            lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (state.changeInProgress) {
                        SubmitSpinner(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (state.submitted) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.compare_more_criteria_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            AdditionalCriteriaSection(
                scores = state.extraScores,
                onScoreChange = onExtraScoreChange
            )

            val submitMoreEnabled = currentEntry != null &&
                !state.submitMoreInProgress &&
                !state.extraSubmitted
            val showUpdate = state.extraSubmitted
            val extraChanged = EXTRA_CRITERIA.any { criterion ->
                state.extraScores[criterion.id] != state.storedExtraScores[criterion.id]
            }
            val updateEnabled = showUpdate &&
                currentEntry != null &&
                !state.submitMoreInProgress &&
                extraChanged
            val submitMoreLabel = stringResource(
                if (state.submitMoreInProgress) {
                    R.string.compare_submitting_label
                } else if (state.extraSubmitted) {
                    R.string.compare_submitted_label
                } else {
                    R.string.compare_submit_more_label
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable(enabled = submitMoreEnabled) { onSubmitMore() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = submitMoreLabel,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (submitMoreEnabled) {
                            lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (state.submitMoreInProgress) {
                        SubmitSpinner(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.logo_small),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (showUpdate) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(
                        modifier = Modifier.clickable(enabled = updateEnabled) { onSubmitMore() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.compare_update_label),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (updateEnabled) {
                                lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (state.submitMoreInProgress) {
                            SubmitSpinner(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showLoginDialog) {
        TournesolLoginDialog(
            inProgress = state.loginInProgress,
            errorMessage = state.loginError,
            onDismiss = onDismissLogin,
            onRegister = onRegister,
            onLogin = onLogin
        )
    }
}

private const val COMPACT_MAIN_CRITERION_ID = "largely_recommended"
private val COMPACT_DIMENSIONS = listOf(
    CompareCriterion(
        id = COMPACT_MAIN_CRITERION_ID,
        labelRes = R.string.compare_criteria_largely_recommended,
        iconRes = R.drawable.logo_small
    )
) + EXTRA_CRITERIA

@Composable
fun CompareCompactScreen(
    state: CompareUiState,
    onScoreChange: (Int) -> Unit,
    onExtraScoreChange: (String, Int) -> Unit,
    onSubmitSelected: (Set<String>) -> Unit,
    onUpdateSelected: (Set<String>) -> Unit,
    showSubmitButton: Boolean,
    onDismissLogin: () -> Unit,
    onRegister: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    val dimensions = remember { COMPACT_DIMENSIONS }
    var activeIndex by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(dimensions.size) {
        if (activeIndex !in dimensions.indices) {
            activeIndex = 0
        }
    }
    val activeIndexSafe = activeIndex.coerceIn(0, dimensions.lastIndex)
    val activeDimension = dimensions[activeIndexSafe]
    val activeScore = dimensionScore(state, activeDimension)
    val animatedScore by animateFloatAsState(
        targetValue = activeScore.toFloat(),
        animationSpec = tween(durationMillis = 90),
        label = "compactScore"
    )
    val displayScore = animatedScore.roundToInt()
    val activeLabel = stringResource(activeDimension.labelRes)
    val activeDescription = stringResource(compactDescriptionRes(activeDimension.id))
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val currentEntry = state.selectedEntry
    var hasRatedMain by rememberSaveable(currentEntry?.streamId) {
        mutableStateOf(state.storedMainScore != null)
    }
    val onSelectionChange: (String, Boolean) -> Unit = { id, selected ->
        selectedIds = if (selected) {
            selectedIds + id
        } else {
            selectedIds - id
        }
    }
    val latestSelectionUpdater by rememberUpdatedState(onSelectionChange)
    val latestActiveDimensionId by rememberUpdatedState(activeDimension.id)
    var showRateFirstMessage by remember { mutableStateOf(false) }
    val triggerRateFirstMessage by rememberUpdatedState {
        showRateFirstMessage = true
    }
    val maxIndex = dimensions.lastIndex
    val density = LocalDensity.current
    val pxPerScore = with(density) { 4.dp.toPx() } / 2f
    val verticalStepPx = with(density) { 28.dp.toPx() } / 0.6f
    val onActiveScoreChange: (Int) -> Unit = { newValue ->
        val clamped = newValue.coerceIn(SCORE_MIN, SCORE_MAX)
        if (activeDimension.id == COMPACT_MAIN_CRITERION_ID) {
            onScoreChange(clamped)
        } else {
            onExtraScoreChange(activeDimension.id, clamped)
        }
    }
    val latestActiveScore by rememberUpdatedState(activeScore)
    val latestActiveIndex by rememberUpdatedState(activeIndexSafe)
    val latestScoreUpdater by rememberUpdatedState(onActiveScoreChange)
    val latestIndexUpdater by rememberUpdatedState { index: Int -> activeIndex = index }
    val latestMaxIndex by rememberUpdatedState(maxIndex)
    val swipeAreaHeight = 120.dp
    val miniPlayerHeight = dimensionResource(R.dimen.mini_player_height)
    val bottomOverlayPadding = swipeAreaHeight + miniPlayerHeight + 60.dp
    var showHistoryOverlay by remember { mutableStateOf(false) }
    var leftHistoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var rightHistoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var historyOverlayIndex by rememberSaveable { mutableIntStateOf(0) }
    var activeOverlayTarget by remember { mutableStateOf(OverlayTarget.LEFT) }
    val selectedHistoryEntryLeft = state.historyEntries.getOrNull(leftHistoryIndex)
        ?: state.historyEntries.firstOrNull()
    val selectedHistoryEntryRight = state.historyEntries.getOrNull(rightHistoryIndex)
        ?: state.historyEntries.firstOrNull()
    var leftHistoryBounds by remember { mutableStateOf<Rect?>(null) }
    var rightHistoryBounds by remember { mutableStateOf<Rect?>(null) }
    val historyListState = rememberLazyListState()
    var historyRowHeightPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.historyEntries) {
        if (state.historyEntries.isEmpty()) {
            leftHistoryIndex = 0
            rightHistoryIndex = 0
            historyOverlayIndex = 0
        } else {
            val maxIndex = state.historyEntries.lastIndex
            leftHistoryIndex = leftHistoryIndex.coerceIn(0, maxIndex)
            rightHistoryIndex = rightHistoryIndex.coerceIn(0, maxIndex)
            historyOverlayIndex = historyOverlayIndex.coerceIn(0, maxIndex)
        }
    }
    LaunchedEffect(selectedHistoryEntryLeft) {
        if (selectedHistoryEntryLeft == null) {
            leftHistoryBounds = null
        }
    }
    LaunchedEffect(selectedHistoryEntryRight) {
        if (selectedHistoryEntryRight == null) {
            rightHistoryBounds = null
        }
    }
    val hasStoredScores = state.storedMainScore != null || state.storedExtraScores.isNotEmpty()
    val isBusy = state.submitInProgress || state.submitMoreInProgress
    val canSubmit = selectedIds.isNotEmpty() && !isBusy
    val buttonLabel = if (hasStoredScores) {
        stringResource(R.string.compare_update_label)
    } else {
        stringResource(R.string.compare_submit_label)
    }
    val buttonAlpha = if (canSubmit) 1f else 0.55f

    LaunchedEffect(state.storedMainScore) {
        if (state.storedMainScore != null) {
            hasRatedMain = true
        }
    }
    LaunchedEffect(hasRatedMain) {
        if (!hasRatedMain && activeIndex != 0) {
            activeIndex = 0
        }
    }
    LaunchedEffect(showRateFirstMessage) {
        if (showRateFirstMessage) {
            delay(1600)
            showRateFirstMessage = false
        }
    }
    LaunchedEffect(showHistoryOverlay, historyRowHeightPx) {
        if (showHistoryOverlay) {
            historyListState.scrollToItem(historyOverlayIndex)
        }
    }

    val gestureModifier = Modifier.pointerInput(Unit) {
        var dragAxis: DragAxis? = null
        var accumulatedX = 0f
        var accumulatedY = 0f
        var currentIndex = 0
        var currentValue = 0
        var markedSelected = false
        detectDragGestures(
            onDragStart = {
                dragAxis = null
                accumulatedX = 0f
                accumulatedY = 0f
                currentIndex = latestActiveIndex
                currentValue = latestActiveScore
                markedSelected = false
            },
            onDragEnd = {
                dragAxis = null
                accumulatedX = 0f
                accumulatedY = 0f
            },
            onDragCancel = {
                dragAxis = null
                accumulatedX = 0f
                accumulatedY = 0f
            },
            onDrag = { change, dragAmount ->
                change.consumeAllChanges()
                if (dragAxis == null) {
                    dragAxis = if (abs(dragAmount.x) >= abs(dragAmount.y)) {
                        DragAxis.HORIZONTAL
                    } else {
                        DragAxis.VERTICAL
                    }
                    if (dragAxis == DragAxis.HORIZONTAL && !markedSelected) {
                        if (latestActiveDimensionId == COMPACT_MAIN_CRITERION_ID) {
                            hasRatedMain = true
                        }
                        latestSelectionUpdater(latestActiveDimensionId, true)
                        markedSelected = true
                    }
                }
                when (dragAxis) {
                    DragAxis.HORIZONTAL -> {
                        accumulatedX += dragAmount.x
                        val steps = (accumulatedX / pxPerScore).toInt()
                        if (steps != 0) {
                            currentValue =
                                (currentValue + steps).coerceIn(SCORE_MIN, SCORE_MAX)
                            latestScoreUpdater(currentValue)
                            if (!markedSelected) {
                                if (latestActiveDimensionId ==
                                    COMPACT_MAIN_CRITERION_ID
                                ) {
                                    hasRatedMain = true
                                }
                                latestSelectionUpdater(latestActiveDimensionId, true)
                                markedSelected = true
                            }
                            accumulatedX -= steps * pxPerScore
                        }
                    }
                    DragAxis.VERTICAL -> {
                        if (!hasRatedMain) {
                            triggerRateFirstMessage()
                            return@detectDragGestures
                        }
                        accumulatedY += dragAmount.y
                        while (abs(accumulatedY) >= verticalStepPx) {
                            val step = if (accumulatedY > 0f) 1 else -1
                            currentIndex =
                                (currentIndex + step).coerceIn(0, latestMaxIndex)
                            latestIndexUpdater(currentIndex)
                            accumulatedY -= step * verticalStepPx
                        }
                    }
                    null -> Unit
                }
            }
        )
    }
    val historyOverlayGestureModifier =
        Modifier.pointerInput(
            leftHistoryBounds,
            rightHistoryBounds,
            state.historyEntries.size,
            leftHistoryIndex,
            rightHistoryIndex
        ) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val target = when {
                    leftHistoryBounds?.contains(down.position) == true -> OverlayTarget.LEFT
                    rightHistoryBounds?.contains(down.position) == true -> OverlayTarget.RIGHT
                    else -> null
                } ?: return@awaitEachGesture
                val pointerId = down.id
                var lastY = down.position.y
                val overlaySensitivity = 1.5f
                activeOverlayTarget = target
                historyOverlayIndex = when (target) {
                    OverlayTarget.LEFT -> leftHistoryIndex
                    OverlayTarget.RIGHT -> rightHistoryIndex
                }.coerceAtLeast(0)
                showHistoryOverlay = true
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes
                            .firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            break
                        }
                        val entriesCount = state.historyEntries.size
                        val deltaY = (change.position.y - lastY) * overlaySensitivity
                        lastY = change.position.y
                        if (entriesCount > 0) {
                            historyListState.dispatchRawDelta(-deltaY)
                            val layoutInfo = historyListState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            if (visibleItems.isNotEmpty()) {
                                val center =
                                    (
                                        layoutInfo.viewportStartOffset +
                                        layoutInfo.viewportEndOffset
                                    ) / 2
                                val centeredIndex = visibleItems.minByOrNull { item ->
                                    val itemCenter = item.offset + item.size / 2
                                    abs(itemCenter - center)
                                }?.index
                                if (centeredIndex != null &&
                                    centeredIndex != historyOverlayIndex
                                ) {
                                    historyOverlayIndex = centeredIndex
                                }
                            }
                        }
                        change.consumeAllChanges()
                    }
                } finally {
                    when (target) {
                        OverlayTarget.LEFT -> leftHistoryIndex = historyOverlayIndex
                        OverlayTarget.RIGHT -> rightHistoryIndex = historyOverlayIndex
                    }
                    showHistoryOverlay = false
                }
            }
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(gestureModifier)
            .then(historyOverlayGestureModifier)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomOverlayPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactHeader(
                label = activeLabel,
                score = displayScore,
                description = activeDescription,
                iconRes = activeDimension.iconRes
            )

            CompactDimensionList(
                dimensions = dimensions,
                activeIndex = activeIndexSafe,
                scores = state,
                selectedIds = selectedIds,
                onToggleSelected = { id, selected -> onSelectionChange(id, selected) },
                onSelect = { index ->
                    if (hasRatedMain) {
                        activeIndex = index
                    }
                },
                modifier = Modifier
            )

            if (selectedHistoryEntryLeft != null || selectedHistoryEntryRight != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedHistoryEntryLeft != null) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coordinates ->
                                    leftHistoryBounds = coordinates.boundsInRoot()
                                },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF42A5F5)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                CompareVideoThumbnailCard(entry = selectedHistoryEntryLeft)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    if (selectedHistoryEntryRight != null) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coordinates ->
                                    rightHistoryBounds = coordinates.boundsInRoot()
                                },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE57373)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                CompareVideoThumbnailCard(entry = selectedHistoryEntryRight)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = miniPlayerHeight + 10.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompactSwipeArea(
                value = activeScore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(swipeAreaHeight)
            )
        }

        if (showHistoryOverlay) {
            val accentBlue = Color(0xFF42A5F5)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .zIndex(4f)
                    .padding(bottom = bottomOverlayPadding),
                contentAlignment = Alignment.Center
            ) {
                val highlightId =
                    state.historyEntries.getOrNull(historyOverlayIndex)?.streamId
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    val overlayHeightPx = with(density) { maxHeight.toPx() }
                    val rowHeightPx = historyRowHeightPx.takeIf { it > 0 } ?: 0
                    val halfPaddingPx =
                        ((overlayHeightPx - rowHeightPx) / 2f).coerceAtLeast(0f)
                    val halfPadding = with(density) { halfPaddingPx.toDp() }
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = historyListState,
                            contentPadding = PaddingValues(
                                top = halfPadding,
                                bottom = halfPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(state.historyEntries, key = { it.streamId }) { entry ->
                                val isHighlight = entry.streamId == highlightId
                                val rowAlpha = if (isHighlight) 0f else 1f
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer(alpha = rowAlpha)
                                        .onSizeChanged { size ->
                                            if (historyRowHeightPx != size.height) {
                                                historyRowHeightPx = size.height
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val stream = remember(entry) { entry.toStreamInfoItem() }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StreamThumbnail(
                                            stream = stream,
                                            showProgress = false,
                                            showDuration = false,
                                            modifier = Modifier.size(width = 144.dp, height = 80.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.streamEntity.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = entry.streamEntity.uploader,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val selectedEntry =
                            state.historyEntries.getOrNull(historyOverlayIndex)
                        if (selectedEntry != null) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = accentBlue
                            ) {
                                val stream = remember(selectedEntry) {
                                    selectedEntry.toStreamInfoItem()
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StreamThumbnail(
                                        stream = stream,
                                        showProgress = false,
                                        showDuration = false,
                                        modifier = Modifier.size(width = 144.dp, height = 80.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedEntry.streamEntity.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = selectedEntry.streamEntity.uploader,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubmitButton && !showHistoryOverlay) {
        Popup(
            alignment = Alignment.BottomCenter,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFD54F), RoundedCornerShape(10.dp))
                        .graphicsLayer(alpha = buttonAlpha)
                        .clickable(enabled = canSubmit) {
                            val snapshot = selectedIds.toSet()
                            if (hasStoredScores) {
                                onUpdateSelected(snapshot)
                            } else {
                                onSubmitSelected(snapshot)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .zIndex(2f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        }
    }

    if (state.showLoginDialog) {
        TournesolLoginDialog(
            inProgress = state.loginInProgress,
            errorMessage = state.loginError,
            onDismiss = onDismissLogin,
            onRegister = onRegister,
            onLogin = onLogin
        )
    }
}

private fun dimensionScore(state: CompareUiState, criterion: CompareCriterion): Int {
    return if (criterion.id == COMPACT_MAIN_CRITERION_ID) {
        state.score
    } else {
        state.extraScores[criterion.id] ?: 0
    }
}

@Composable
private fun CompactHeader(
    label: String,
    score: Int,
    description: String,
    iconRes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 150.dp)
        )
        Text(
            text = formatSignedScore(score),
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun compactDescriptionRes(criterionId: String): Int {
    return when (criterionId) {
        COMPACT_MAIN_CRITERION_ID -> R.string.compare_criteria_desc_largely_recommended
        "reliability" -> R.string.compare_criteria_desc_reliability
        "pedagogy" -> R.string.compare_criteria_desc_pedagogy
        "importance" -> R.string.compare_criteria_desc_importance
        "layman_friendly" -> R.string.compare_criteria_desc_layman_friendly
        "entertaining_relaxing" -> R.string.compare_criteria_desc_entertaining_relaxing
        "engaging" -> R.string.compare_criteria_desc_engaging
        "diversity_inclusion" -> R.string.compare_criteria_desc_diversity_inclusion
        "better_habits" -> R.string.compare_criteria_desc_better_habits
        "backfire_risk" -> R.string.compare_criteria_desc_backfire_risk
        else -> R.string.compare_criteria_desc_largely_recommended
    }
}

@Composable
private fun CompactDimensionList(
    dimensions: List<CompareCriterion>,
    activeIndex: Int,
    scores: CompareUiState,
    selectedIds: Set<String>,
    onToggleSelected: (String, Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        dimensions.forEachIndexed { index, criterion ->
            val score = dimensionScore(scores, criterion)
            CompactDimensionRow(
                criterion = criterion,
                score = score,
                isActive = index == activeIndex,
                isSelected = selectedIds.contains(criterion.id),
                onToggleSelected = { selected -> onToggleSelected(criterion.id, selected) },
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun CompactDimensionRow(
    criterion: CompareCriterion,
    score: Int,
    isActive: Boolean,
    isSelected: Boolean,
    onToggleSelected: (Boolean) -> Unit,
    onClick: () -> Unit
) {
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
            .padding(horizontal = 10.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .padding(end = 4.dp)
                .then(
                    if (isSelected) Modifier.clickable { onToggleSelected(false) } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Image(
            painter = painterResource(criterion.iconRes),
            contentDescription = null,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(criterion.labelRes),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val valueText =
            if (score == 0 && !isSelected && !isActive) "" else formatSignedScore(score)
        val scoreColor = when {
            score > 0 -> Color(0xFFE57373)
            score < 0 -> Color(0xFF64B5F6)
            else -> textColor.copy(alpha = 0.7f)
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelSmall,
            color = scoreColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        MiniScoreBar(
            value = score,
            isActive = isActive,
            modifier = Modifier
                .width(88.dp)
                .height(3.dp)
        )
    }
}

@Composable
private fun MiniScoreBar(
    value: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val baseBarColor = if (value >= 0) {
        Color(0xFFE57373)
    } else {
        Color(0xFF64B5F6)
    }
    val barColor = if (isActive) {
        baseBarColor
    } else {
        baseBarColor.copy(alpha = 0.7f)
    }
    val background = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val centerLine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    Canvas(modifier = modifier) {
        val radius = size.height / 2f
        drawRoundRect(
            color = background,
            cornerRadius = CornerRadius(radius, radius)
        )
        drawLine(
            color = centerLine,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = 1.dp.toPx()
        )
        val ratio = value.toFloat() / SCORE_MAX.toFloat()
        val fillWidth = (size.width / 2f) * abs(ratio)
        if (fillWidth > 0f) {
            val startX = if (ratio >= 0f) size.width / 2f else size.width / 2f - fillWidth
            drawRoundRect(
                color = barColor,
                topLeft = Offset(startX, 0f),
                size = Size(fillWidth, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

private enum class DragAxis {
    HORIZONTAL,
    VERTICAL
}

private enum class OverlayTarget {
    LEFT,
    RIGHT
}

@Composable
private fun CompactSwipeArea(
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
            Text(
                text = stringResource(R.string.compare_score_label, value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            CompactScoreTrack(value = value)
        }
    }
}

@Composable
private fun CompactScoreTrack(
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

@Composable
private fun CompactSubmitSection(
    state: CompareUiState,
    currentEntry: StreamHistoryEntry?,
    onSubmit: () -> Unit,
    onChangeMainScore: () -> Unit,
    onSubmitMore: () -> Unit
) {
    val submitEnabled = currentEntry != null && !state.submitted && !state.submitInProgress
    val showChange = state.submittedConfirmed
    val changeEnabled = showChange &&
        currentEntry != null &&
        !state.changeInProgress &&
        (state.storedMainScore == null || state.score != state.storedMainScore)
    val submitLabel = stringResource(
        if (state.submitInProgress) {
            R.string.compare_submitting_label
        } else if (state.submitted) {
            R.string.compare_submitted_label
        } else {
            R.string.compare_submit_label
        }
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable(enabled = submitEnabled) { onSubmit() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = submitLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (submitEnabled) {
                        lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (state.submitInProgress) {
                    SubmitSpinner(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.logo_small),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (showChange) {
                Spacer(modifier = Modifier.width(14.dp))
                Row(
                    modifier = Modifier.clickable(enabled = changeEnabled) { onChangeMainScore() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.compare_change_label),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (changeEnabled) {
                            lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (state.changeInProgress) {
                        SubmitSpinner(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (state.submitted) {
            val submitMoreEnabled = currentEntry != null &&
                !state.submitMoreInProgress &&
                !state.extraSubmitted
            val showUpdate = state.extraSubmitted
            val extraChanged = EXTRA_CRITERIA.any { criterion ->
                state.extraScores[criterion.id] != state.storedExtraScores[criterion.id]
            }
            val updateEnabled = showUpdate &&
                currentEntry != null &&
                !state.submitMoreInProgress &&
                extraChanged
            val submitMoreLabel = stringResource(
                if (state.submitMoreInProgress) {
                    R.string.compare_submitting_label
                } else if (state.extraSubmitted) {
                    R.string.compare_submitted_label
                } else {
                    R.string.compare_submit_more_label
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable(enabled = submitMoreEnabled) { onSubmitMore() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = submitMoreLabel,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (submitMoreEnabled) {
                            lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (state.submitMoreInProgress) {
                        SubmitSpinner(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.logo_small),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (showUpdate) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(
                        modifier = Modifier.clickable(enabled = updateEnabled) { onSubmitMore() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.compare_update_label),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (updateEnabled) {
                                lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (state.submitMoreInProgress) {
                            SubmitSpinner(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSignedScore(score: Int): String {
    return if (score > 0) {
        "+$score"
    } else {
        score.toString()
    }
}

@Composable
private fun TournesolLoginDialog(
    inProgress: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRegister: () -> Unit,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val statusText = when {
        localError != null -> localError
        inProgress -> stringResource(R.string.tournesol_login_in_progress)
        errorMessage != null -> errorMessage
        else -> null
    }
    val statusColor = if (localError != null || errorMessage != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val missingFieldsText = stringResource(R.string.tournesol_login_missing_fields)

    Dialog(onDismissRequest = { if (!inProgress) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.tournesol_login_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.compare_login_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        if (localError != null) {
                            localError = null
                        }
                    },
                    label = { Text(stringResource(R.string.tournesol_username_hint)) },
                    singleLine = true,
                    enabled = !inProgress,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (localError != null) {
                            localError = null
                        }
                    },
                    label = { Text(stringResource(R.string.tournesol_password_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !inProgress,
                    modifier = Modifier.fillMaxWidth()
                )
                if (statusText != null) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onRegister,
                        enabled = !inProgress
                    ) {
                        Text(stringResource(R.string.tournesol_register_button))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !inProgress
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank()) {
                                    localError = missingFieldsText
                                } else {
                                    localError = null
                                    onLogin(username.trim(), password.trim())
                                }
                            },
                            enabled = !inProgress
                        ) {
                            if (inProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.tournesol_login_button))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryWheel(
    entries: List<StreamHistoryEntry>,
    selectedIndex: Int,
    historyMessageRes: Int?,
    onSelectIndex: (Int) -> Unit
) {
    val itemHeight = 64.dp
    if (entries.isEmpty()) {
        val message = historyMessageRes?.let { stringResource(it) }.orEmpty()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = itemHeight),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
        return
    }

    val density = LocalDensity.current
    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceIn(0, entries.lastIndex)
    ) { entries.size }
    val coroutineScope = rememberCoroutineScope()
    val latestSelectedIndex by rememberUpdatedState(selectedIndex)
    val cameraDistance = with(density) { 22.dp.toPx() }
    val depthShift = with(density) { 18.dp.toPx() }
    val pageSizePx = with(density) { itemHeight.toPx() }
    val overlap = itemHeight * 0.75f
    val pageStepPx = with(density) { (itemHeight - overlap).toPx() }.coerceAtLeast(1f)

    LaunchedEffect(entries, selectedIndex) {
        val targetPage = selectedIndex.coerceIn(0, entries.lastIndex)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState, entries) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                val newIndex = page.coerceIn(0, entries.lastIndex)
                if (newIndex != latestSelectedIndex) {
                    onSelectIndex(newIndex)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * 2f)
            .pointerInput(pagerState, pageStepPx, entries.size) {
                val velocityTracker = VelocityTracker()
                var startPage = 0
                var startPosition = 0f
                var position = 0f
                var minPage = 0
                var maxPage = 0
                var dragDistance = 0f
                fun positionToPageOffset(currentPosition: Float): Pair<Int, Float> {
                    var clampedPosition = currentPosition.coerceIn(
                        minPage.toFloat(),
                        maxPage.toFloat()
                    )
                    var page = clampedPosition.toInt()
                    var offset = clampedPosition - page
                    if (offset > 0.5f && page < entries.lastIndex) {
                        page += 1
                        offset -= 1f
                    }
                    offset = offset.coerceIn(-0.5f, 0.5f)
                    return page to offset
                }
                detectDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                        startPage = pagerState.currentPage
                        startPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        position = startPosition
                        minPage = (startPage - 1).coerceAtLeast(0)
                        maxPage = (startPage + 1).coerceAtMost(entries.lastIndex)
                        dragDistance = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        dragDistance += dragAmount.y
                        val rawDelta = -dragDistance / pageStepPx
                        val absRawDelta = abs(rawDelta)
                        val visualDelta = if (absRawDelta <= 0.5f) {
                            rawDelta
                        } else {
                            val excess = absRawDelta - 0.5f
                            val damped = 0.5f + excess * 0.4f
                            if (rawDelta < 0f) -damped else damped
                        }
                        position = (startPosition + visualDelta).coerceIn(
                            minPage.toFloat(),
                            maxPage.toFloat()
                        )
                        val (targetPage, offset) = positionToPageOffset(position)
                        pagerState.requestScrollToPage(
                            targetPage,
                            offset
                        )
                    },
                    onDragCancel = {
                        val target = position.roundToInt()
                            .coerceIn(minPage, maxPage)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                target,
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                        }
                    },
                    onDragEnd = {
                        val deltaFromStart = -dragDistance / pageStepPx
                        val velocityY = velocityTracker.calculateVelocity().y
                        val velocityThreshold = pageStepPx * 2.5f
                        val minFlingOffset = 0.08f
                        val swipeThreshold = 0.28f
                        val minTarget = minPage.coerceIn(0, entries.lastIndex)
                        val maxTarget = maxPage.coerceIn(0, entries.lastIndex)
                        val target = when {
                            deltaFromStart > swipeThreshold -> startPage + 1
                            deltaFromStart < -swipeThreshold -> startPage - 1
                            kotlin.math.abs(velocityY) > velocityThreshold &&
                                kotlin.math.abs(deltaFromStart) > minFlingOffset ->
                                if (velocityY < 0f) startPage + 1 else startPage - 1
                            else -> startPage
                        }.coerceIn(minTarget, maxTarget)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                target,
                                animationSpec = tween(
                                    durationMillis = 320,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                        }
                    }
                )
            }
    ) {
        val contentPadding = PaddingValues(vertical = itemHeight / 2)
        VerticalPager(
            state = pagerState,
            contentPadding = contentPadding,
            pageSize = PageSize.Fixed(itemHeight),
            pageSpacing = -overlap,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val layoutInfo = pagerState.layoutInfo
            val pageInfo = layoutInfo.visiblePagesInfo.firstOrNull { it.index == page }
            val rawOffset = if (pageInfo != null && pageStepPx > 0f) {
                val pageCenter = pageInfo.offset + pageSizePx / 2f
                val viewportCenter =
                    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                (pageCenter - viewportCenter) / pageStepPx
            } else {
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            }
            val rawOffsetAbs = abs(rawOffset)
            val pageOffset = rawOffsetAbs.coerceIn(0f, 1f)
            val focusSpan = 1.35f
            val visibilitySpan = 1.6f
            val focusOffset = (rawOffsetAbs / focusSpan).coerceIn(0f, 1f)
            val focus = 1f - focusOffset
            val visibility = ((visibilitySpan - rawOffsetAbs) / visibilitySpan)
                .coerceIn(0f, 1f)
            val direction = when {
                rawOffset > 0f -> 1f
                rawOffset < 0f -> -1f
                else -> 0f
            }
            val scale = lerp(1f, 0.88f, pageOffset)
            val alpha = lerp(1f, 0.35f, pageOffset)
            val rotationX = lerp(0f, 10f, pageOffset) * -direction
            val translation = depthShift * pageOffset * direction
            val elevation = (10f * (1f - pageOffset)).dp
            val contentAlpha = alpha * visibility
            val rawBackgroundAlpha = (1f - rawOffsetAbs).coerceIn(0f, 1f)
            val computedBackgroundAlpha = rawBackgroundAlpha * rawBackgroundAlpha
            val backgroundAlpha = if (pagerState.currentPage == page &&
                !pagerState.isScrollInProgress
            ) {
                1f
            } else {
                computedBackgroundAlpha
            }
            HistoryCard(
                entry = entries[page],
                focus = focus,
                contentAlpha = contentAlpha,
                backgroundAlpha = backgroundAlpha,
                elevation = elevation,
                modifier = Modifier
                    .height(itemHeight)
                    .zIndex(focus)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        rotationX = rotationX,
                        translationY = translation,
                        cameraDistance = cameraDistance
                    )
            )
        }
    }
}

@Composable
private fun HistoryCard(
    entry: StreamHistoryEntry,
    focus: Float,
    contentAlpha: Float,
    backgroundAlpha: Float,
    elevation: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val clampedFocus = focus.coerceIn(0f, 1f)
    val clampedBackgroundAlpha = backgroundAlpha.coerceIn(0f, 1f)
    val background = lerp(
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.colorScheme.surfaceContainerHigh,
        clampedFocus
    ).copy(alpha = clampedBackgroundAlpha)
    val borderAlpha = (lerp(0.25f, 0.6f, clampedFocus) * clampedBackgroundAlpha)
        .coerceIn(0f, 1f)
    val borderColor = Color(0xFF64B5F6).copy(alpha = borderAlpha)
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = BorderStroke(
            1.5.dp,
            borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer(alpha = contentAlpha.coerceIn(0f, 1f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompareVideoRow(entry)
        }
    }
}

@Composable
private fun ThickScoreSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayValue = abs(value)
    val scoreDescription = stringResource(
        R.string.compare_score_accessibility,
        displayValue,
        0,
        SCORE_MAX
    )
    var sliderSize by remember { mutableStateOf(IntSize.Zero) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val leftAccent = Color(0xFFE57373)
    val rightAccent = Color(0xFF64B5F6)
    val accentColor = when {
        value < 0 -> leftAccent
        value > 0 -> rightAccent
        else -> Color(0xFF9E9E9E)
    }
    val fillColor = accentColor
    val indicatorColor = accentColor
    val labelFillColor = MaterialTheme.colorScheme.surface
    val labelStrokeColor = accentColor
    val labelShadowColor = accentColor.copy(alpha = 0.18f)
    val labelTextColor = accentColor
    val valueText = displayValue.toString()
    val deadZonePx = with(LocalDensity.current) { 10.dp.toPx() }

    fun updateFromPosition(x: Float, snapToCenter: Boolean) {
        val width = sliderSize.width.toFloat()
        if (width <= 0f) {
            return
        }
        val centerX = width / 2f
        val clamped = x.coerceIn(0f, width)
        val normalized = ((clamped - centerX) / centerX).coerceIn(-1f, 1f)
        var newValue = (normalized * SCORE_MAX)
            .roundToInt()
            .coerceIn(SCORE_MIN, SCORE_MAX)
        if (snapToCenter && abs(clamped - centerX) <= deadZonePx) {
            newValue = 0
        }
        if (newValue != value) {
            onValueChange(newValue)
        }
    }

    Canvas(
        modifier = modifier
            .height(72.dp)
            .padding(vertical = 6.dp)
            .onSizeChanged { sliderSize = it }
            .semantics { contentDescription = scoreDescription }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    updateFromPosition(offset.x, snapToCenter = true)
                }
            }
            .pointerInput(Unit) {
                var lastDragX = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        lastDragX = offset.x
                        updateFromPosition(offset.x, snapToCenter = false)
                    },
                    onDrag = { change, _ ->
                        lastDragX = change.position.x
                        updateFromPosition(change.position.x, snapToCenter = false)
                    },
                    onDragEnd = {
                        updateFromPosition(lastDragX, snapToCenter = true)
                    },
                    onDragCancel = {
                        updateFromPosition(lastDragX, snapToCenter = true)
                    }
                )
            }
    ) {
        val trackHeight = size.height * 0.84f
        val trackTop = (size.height - trackHeight) / 2f
        val trackRadius = trackHeight * 0.08f
        drawRoundRect(
            color = trackColor,
            topLeft = androidx.compose.ui.geometry.Offset(0f, trackTop),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackRadius, trackRadius)
        )

        val centerX = size.width / 2f
        val signedProgress = when {
            value > 0 -> value.toFloat() / SCORE_MAX.toFloat()
            value < 0 -> value.toFloat() / -SCORE_MIN.toFloat()
            else -> 0f
        }.coerceIn(-1f, 1f)
        val fillWidth = kotlin.math.abs(signedProgress) * centerX
        if (fillWidth > 0f) {
            val fillStartX = if (signedProgress >= 0f) centerX else centerX - fillWidth
            drawRoundRect(
                color = fillColor,
                topLeft = androidx.compose.ui.geometry.Offset(fillStartX, trackTop),
                size = Size(fillWidth, trackHeight),
                cornerRadius = CornerRadius(trackRadius, trackRadius)
            )
        }

        val indicatorCenterX = centerX + signedProgress * centerX
        val indicatorHeight = trackHeight * 1.28f
        val indicatorWidth = trackHeight * 0.14f
        val indicatorTop = (size.height - indicatorHeight) / 2f
        drawRect(
            color = indicatorColor,
            topLeft = androidx.compose.ui.geometry.Offset(
                indicatorCenterX - indicatorWidth / 2f,
                indicatorTop
            ),
            size = Size(indicatorWidth, indicatorHeight)
        )

        val labelHeight = trackHeight * 0.46f
        val labelRadius = labelHeight / 2f
        val labelCenterY = size.height / 2f
        val labelTop = labelCenterY - labelHeight / 2f
        val labelPaddingX = labelHeight * 0.5f
        val labelTextSize = labelHeight * 0.6f
        val labelPaint = AndroidPaint().apply {
            isAntiAlias = true
            color = labelTextColor.toArgb()
            textAlign = AndroidPaint.Align.CENTER
            textSize = labelTextSize
            isFakeBoldText = true
        }
        val textWidth = labelPaint.measureText(valueText)
        val labelWidth = kotlin.math.max(labelHeight, textWidth + labelPaddingX * 2f)
        val labelLeft = indicatorCenterX - labelWidth / 2f
        drawRoundRect(
            color = labelShadowColor,
            topLeft = androidx.compose.ui.geometry.Offset(
                labelLeft,
                labelTop + labelHeight * 0.12f
            ),
            size = Size(labelWidth, labelHeight),
            cornerRadius = CornerRadius(labelRadius, labelRadius)
        )
        drawRoundRect(
            color = labelFillColor,
            topLeft = androidx.compose.ui.geometry.Offset(labelLeft, labelTop),
            size = Size(labelWidth, labelHeight),
            cornerRadius = CornerRadius(labelRadius, labelRadius)
        )
        drawRoundRect(
            color = labelStrokeColor,
            topLeft = androidx.compose.ui.geometry.Offset(labelLeft, labelTop),
            size = Size(labelWidth, labelHeight),
            cornerRadius = CornerRadius(labelRadius, labelRadius),
            style = Stroke(width = labelHeight * 0.08f)
        )
        drawContext.canvas.nativeCanvas.apply {
            val metrics = labelPaint.fontMetrics
            val textY = labelCenterY - (metrics.ascent + metrics.descent) / 2f
            drawText(valueText, indicatorCenterX, textY, labelPaint)
        }
    }
}

@Composable
private fun AdditionalCriteriaSection(
    scores: Map<String, Int>,
    onScoreChange: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EXTRA_CRITERIA.forEach { criterion ->
            CriteriaScoreRow(
                criterion = criterion,
                score = scores[criterion.id] ?: 0,
                onScoreChange = { value -> onScoreChange(criterion.id, value) }
            )
        }
    }
}

@Composable
private fun CriteriaScoreRow(
    criterion: CompareCriterion,
    score: Int,
    onScoreChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(criterion.iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(criterion.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        ThickScoreSlider(
            value = score,
            onValueChange = onScoreChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CompareVideoRow(entry: StreamHistoryEntry) {
    val stream = remember(entry) { entry.toStreamInfoItem() }
    val thumbnailDescription = stringResource(R.string.compare_thumbnail_description)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamThumbnail(
            stream = stream,
            showProgress = false,
            modifier = Modifier
                .size(width = 88.dp, height = 48.dp)
                .semantics {
                    contentDescription = thumbnailDescription
                }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = stream.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stream.uploaderName.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64B5F6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompareVideoThumbnailCard(entry: StreamHistoryEntry) {
    val stream = remember(entry) { entry.toStreamInfoItem() }
    val thumbnailDescription = stringResource(R.string.compare_thumbnail_description)
    Column(modifier = Modifier.fillMaxWidth()) {
        StreamThumbnail(
            stream = stream,
            showProgress = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .semantics {
                    contentDescription = thumbnailDescription
                }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stream.name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stream.uploaderName.orEmpty(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color(0xFF64B5F6),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SubmitSpinner(
    modifier: Modifier = Modifier,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "submitSpinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "submitSpinnerRotation"
    )
    val sweep by transition.animateFloat(
        initialValue = 80f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "submitSpinnerSweep"
    )
    val brush = Brush.sweepGradient(
        listOf(
            color.copy(alpha = 0.15f),
            color,
            color.copy(alpha = 0.15f)
        )
    )
    Canvas(
        modifier = modifier.graphicsLayer { rotationZ = rotation }
    ) {
        val strokeWidth = size.minDimension * 0.18f
        drawArc(
            brush = brush,
            startAngle = 0f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
