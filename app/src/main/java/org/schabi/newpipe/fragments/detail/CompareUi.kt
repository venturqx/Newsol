package org.schabi.newpipe.fragments.detail

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.schabi.newpipe.R
import org.schabi.newpipe.database.history.model.StreamHistoryEntry
import org.schabi.newpipe.ui.components.items.stream.StreamThumbnail

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
    onLogin: (String, String) -> Unit,
    onNavigateToVideo: ((Int, String, String) -> Unit)? = null
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
            text = stringResource(
                R.string.compare_instruction_text,
                stringResource(R.string.compare_current_video_header),
                stringResource(R.string.compare_last_viewed_header)
            ),
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
            onSelectIndex = onSelectIndex,
            onNavigateToVideo = onNavigateToVideo
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.compare_current_video_header),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE57373)
            )
            Text(
                text = stringResource(R.string.compare_last_viewed_header),
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
        val chipColors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colorResource(R.color.tournesol_chip_bg_selected),
            containerColor = colorResource(R.color.tournesol_chip_bg_unselected),
            selectedLabelColor = colorResource(R.color.tournesol_chip_text_selected),
            labelColor = MaterialTheme.colorScheme.onSurface
        )
        val chipShape = RoundedCornerShape(8.dp)
        val chipTextStyle = TextStyle(
            fontFamily = FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL)),
            fontSize = 14.sp,
            letterSpacing = TextUnit(0.04f, TextUnitType.Em)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = true,
                onClick = { onSubmit() },
                enabled = submitEnabled,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.submitInProgress) {
                            SubmitSpinner(
                                modifier = Modifier.size(16.dp),
                                color = colorResource(R.color.tournesol_chip_text_selected)
                            )
                        }
                        Text(text = submitLabel, style = chipTextStyle)
                    }
                },
                colors = chipColors,
                shape = chipShape,
                border = null
            )
            if (showChange) {
                FilterChip(
                    selected = true,
                    onClick = { onChangeMainScore() },
                    enabled = changeEnabled,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.changeInProgress) {
                                SubmitSpinner(
                                    modifier = Modifier.size(16.dp),
                                    color = colorResource(R.color.tournesol_chip_text_selected)
                                )
                            } else {
                                Image(
                                    painter = painterResource(R.drawable.ic_refresh),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.compare_change_label),
                                style = chipTextStyle
                            )
                        }
                    },
                    colors = chipColors,
                    shape = chipShape,
                    border = null
                )
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
                    modifier = Modifier.clickable(
                        enabled = submitMoreEnabled,
                        onClick = { onSubmitMore() }
                    ),
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
                    if (state.submitMoreInProgress) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SubmitSpinner(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (showUpdate) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(
                        modifier = Modifier.clickable(
                            enabled = updateEnabled,
                            onClick = { onSubmitMore() }
                        ),
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
@OptIn(ExperimentalFoundationApi::class)
internal fun CompareCompactScreen(
    state: CompareUiState,
    onScoreChange: (Int) -> Unit,
    onExtraScoreChange: (String, Int) -> Unit,
    onSubmitSelected: (Set<String>) -> Unit,
    onUpdateSelected: (Set<String>) -> Unit,
    onPairSelectionChange: (ComparePairSelection?) -> Unit,
    onDismissRecommendations: () -> Unit,
    onDismissLogin: () -> Unit,
    onRegister: () -> Unit,
    onLogin: (String, String) -> Unit,
    onNavigateToVideo: ((Int, String, String) -> Unit)? = null,
    onRandomizeLeft: () -> Unit = {},
    onRandomizeRight: () -> Unit = {}
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
    val activeDescription = stringResource(compactDescriptionRes(activeDimension.id))
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val onSelectionChange: (String, Boolean) -> Unit = { id, selected ->
        selectedIds = if (selected) {
            selectedIds + id
        } else {
            selectedIds - id
        }
        if (!selected) {
            if (id == COMPACT_MAIN_CRITERION_ID) {
                onScoreChange(0)
            } else {
                onExtraScoreChange(id, 0)
            }
        }
    }
    val latestSelectionUpdater by rememberUpdatedState(onSelectionChange)
    val latestActiveDimensionId by rememberUpdatedState(activeDimension.id)
    val maxIndex = dimensions.lastIndex
    val context = LocalContext.current
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
    val miniPlayerHeight = dimensionResource(R.dimen.mini_player_height)
    val bottomContentPadding = miniPlayerHeight + 12.dp
    var showHistoryOverlay by remember { mutableStateOf(false) }
    var hasOpenedHistoryOverlay by rememberSaveable { mutableStateOf(false) }
    val overlayGridColumns = 1
    var overlayRowsPerPage by rememberSaveable { mutableIntStateOf(4) }
    val overlayPageSize = overlayGridColumns * overlayRowsPerPage
    val leftEntries = remember(state.historyEntries, state.currentEntry) {
        val current = state.currentEntry
        if (current == null) {
            state.historyEntries
        } else {
            listOf(current) + state.historyEntries
        }
    }
    val rightEntries = state.historyEntries
    var leftHistoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var rightHistoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var historyOverlayIndex by rememberSaveable { mutableIntStateOf(0) }
    var historyOverlayPage by rememberSaveable { mutableIntStateOf(0) }
    var activeOverlayTarget by remember { mutableStateOf(OverlayTarget.LEFT) }
    val overlayEntries = if (activeOverlayTarget == OverlayTarget.RIGHT) {
        rightEntries
    } else {
        leftEntries
    }
    val selectedHistoryEntryLeft = leftEntries.getOrNull(leftHistoryIndex)
        ?: leftEntries.firstOrNull()
    val selectedHistoryEntryRight = rightEntries.getOrNull(rightHistoryIndex)
        ?: rightEntries.firstOrNull()
    val hasSuggestedLeft = state.suggestedLeft != null
    val hasSuggestedRight = state.suggestedRight != null
    val hasSuggestions = hasSuggestedLeft || hasSuggestedRight
    val suggestedLeftTitle = state.suggestedLeft?.title.orEmpty()
    val suggestedLeftUploader = state.suggestedLeft?.uploader.orEmpty()
    val suggestedRightTitle = state.suggestedRight?.title.orEmpty()
    val suggestedRightUploader = state.suggestedRight?.uploader.orEmpty()
    val currentPairSelection = if (hasSuggestedLeft && hasSuggestedRight) {
        val leftUrl = state.suggestedLeft!!.videoUrl
        val rightUrl = state.suggestedRight!!.videoUrl
        if (leftUrl != null && rightUrl != null && state.suggestedLeft.uid != state.suggestedRight.uid) {
            ComparePairSelection(
                leftServiceId = CompareRepository.uidToServiceId(state.suggestedLeft.uid),
                leftUrl = leftUrl,
                rightServiceId = CompareRepository.uidToServiceId(state.suggestedRight.uid),
                rightUrl = rightUrl
            )
        } else {
            null
        }
    } else if (selectedHistoryEntryLeft != null &&
        selectedHistoryEntryRight != null &&
        !(
            selectedHistoryEntryLeft.streamEntity.serviceId ==
                selectedHistoryEntryRight.streamEntity.serviceId &&
                selectedHistoryEntryLeft.streamEntity.url ==
                selectedHistoryEntryRight.streamEntity.url
            )
    ) {
        ComparePairSelection(
            leftServiceId = selectedHistoryEntryLeft.streamEntity.serviceId,
            leftUrl = selectedHistoryEntryLeft.streamEntity.url,
            rightServiceId = selectedHistoryEntryRight.streamEntity.serviceId,
            rightUrl = selectedHistoryEntryRight.streamEntity.url
        )
    } else {
        null
    }
    val currentEntry = state.currentEntry
    val isLeftCurrent = !hasSuggestedLeft && selectedHistoryEntryLeft != null &&
        currentEntry != null &&
        selectedHistoryEntryLeft.streamEntity.serviceId == currentEntry.streamEntity.serviceId &&
        selectedHistoryEntryLeft.streamEntity.url == currentEntry.streamEntity.url
    val isRightCurrent = !hasSuggestedRight && selectedHistoryEntryRight != null &&
        currentEntry != null &&
        selectedHistoryEntryRight.streamEntity.serviceId == currentEntry.streamEntity.serviceId &&
        selectedHistoryEntryRight.streamEntity.url == currentEntry.streamEntity.url
    val leftStreamId = selectedHistoryEntryLeft?.streamId
    val rightStreamId = selectedHistoryEntryRight?.streamId
    var lastLeftStreamId by remember { mutableStateOf(leftStreamId) }
    var lastRightStreamId by remember { mutableStateOf(rightStreamId) }
    var overlayGridBounds by remember { mutableStateOf<Rect?>(null) }
    var overlayGridSize by remember { mutableStateOf(IntSize.Zero) }
    var overlayDragPreviewIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(leftEntries, rightEntries) {
        if (leftEntries.isEmpty() && rightEntries.isEmpty()) {
            leftHistoryIndex = 0
            rightHistoryIndex = 0
            historyOverlayIndex = 0
            historyOverlayPage = 0
        } else {
            if (leftEntries.isNotEmpty()) {
                val leftMax = leftEntries.lastIndex
                leftHistoryIndex = leftHistoryIndex.coerceIn(0, leftMax)
            } else {
                leftHistoryIndex = 0
            }
            if (rightEntries.isNotEmpty()) {
                val rightMax = rightEntries.lastIndex
                rightHistoryIndex = rightHistoryIndex.coerceIn(0, rightMax)
            } else {
                rightHistoryIndex = 0
            }
        }
    }
    BackHandler(enabled = showHistoryOverlay) {
        showHistoryOverlay = false
    }
    LaunchedEffect(showHistoryOverlay) {
        if (showHistoryOverlay) {
            hasOpenedHistoryOverlay = true
        } else {
            overlayGridBounds = null
            overlayGridSize = IntSize.Zero
            overlayDragPreviewIndex = null
        }
    }
    LaunchedEffect(leftStreamId, rightStreamId) {
        val hasChanged =
            leftStreamId != lastLeftStreamId || rightStreamId != lastRightStreamId
        if (hasChanged) {
            lastLeftStreamId = leftStreamId
            lastRightStreamId = rightStreamId
            onScoreChange(0)
            COMPACT_DIMENSIONS.forEach { criterion ->
                if (criterion.id != COMPACT_MAIN_CRITERION_ID) {
                    onExtraScoreChange(criterion.id, 0)
                }
            }
            selectedIds = emptySet()
        }
    }
    LaunchedEffect(showHistoryOverlay, activeOverlayTarget, overlayEntries.size, overlayPageSize) {
        if (showHistoryOverlay && overlayEntries.isNotEmpty()) {
            historyOverlayIndex = historyOverlayIndex.coerceIn(0, overlayEntries.lastIndex)
            val maxPage = ((overlayEntries.size - 1) / overlayPageSize).coerceAtLeast(0)
            historyOverlayPage = (historyOverlayIndex / overlayPageSize).coerceIn(0, maxPage)
        }
    }
    LaunchedEffect(currentPairSelection) {
        onPairSelectionChange(currentPairSelection)
    }
    val hasStoredScores = state.storedMainScore != null || state.storedExtraScores.isNotEmpty()
    val hasExistingComparison = state.submitted || hasStoredScores
    val isBusy = state.submitInProgress || state.submitMoreInProgress
    val hasValidPairSelection = currentPairSelection != null
    val canSubmit = hasValidPairSelection && selectedIds.isNotEmpty() && !isBusy
    val submitButtonLabel = stringResource(
        when {
            isBusy -> R.string.compare_submitting_label
            hasExistingComparison -> R.string.compare_update_label
            else -> R.string.compare_submit_label
        }
    )
    val submitOrUpdateAction = {
        val snapshot = selectedIds.toSet()
        if (hasExistingComparison) {
            onUpdateSelected(snapshot)
        } else {
            onSubmitSelected(snapshot)
        }
    }
    val openHistoryOverlay: (OverlayTarget) -> Unit = { target ->
        val targetEntries = if (target == OverlayTarget.RIGHT) {
            rightEntries
        } else {
            leftEntries
        }
        if (targetEntries.isNotEmpty()) {
            activeOverlayTarget = target
            historyOverlayIndex = when (target) {
                OverlayTarget.LEFT -> leftHistoryIndex
                OverlayTarget.RIGHT -> rightHistoryIndex
            }.coerceIn(0, targetEntries.lastIndex)
            historyOverlayPage = historyOverlayIndex / overlayPageSize.coerceAtLeast(1)
            hasOpenedHistoryOverlay = true
            showHistoryOverlay = true
        }
    }

    val gestureModifier = Modifier.pointerInput(Unit) {
        val horizontalSensitivity = 1.2f
        val verticalSensitivity = 1.3f
        var dragAxis: DragAxis? = null
        var accumulatedX = 0f
        var accumulatedY = 0f
        var totalDragX = 0f
        var totalDragY = 0f
        var verticalStepTriggered = false
        var currentIndex = 0
        var currentValue = 0
        var markedSelected = false
        detectDragGestures(
            onDragStart = {
                dragAxis = null
                accumulatedX = 0f
                accumulatedY = 0f
                totalDragX = 0f
                totalDragY = 0f
                verticalStepTriggered = false
                currentIndex = latestActiveIndex
                currentValue = latestActiveScore
                markedSelected = false
            },
            onDragEnd = {
                if (dragAxis == DragAxis.VERTICAL &&
                    !verticalStepTriggered &&
                    abs(totalDragY) > abs(totalDragX) &&
                    abs(totalDragY) > 0f &&
                    abs(totalDragY) < verticalStepPx
                ) {
                    val step = if (totalDragY > 0f) 1 else -1
                    val fallbackIndex = (currentIndex + step).coerceIn(0, latestMaxIndex)
                    if (fallbackIndex != currentIndex) {
                        latestIndexUpdater(fallbackIndex)
                    }
                }
                dragAxis = null
                accumulatedX = 0f
                accumulatedY = 0f
                totalDragX = 0f
                totalDragY = 0f
                verticalStepTriggered = false
            },
            onDragCancel = {
                dragAxis = null
                accumulatedX = 0f
                accumulatedY = 0f
                totalDragX = 0f
                totalDragY = 0f
                verticalStepTriggered = false
            },
            onDrag = { change, dragAmount ->
                change.consumeAllChanges()
                totalDragX += dragAmount.x
                totalDragY += dragAmount.y
                if (dragAxis == null) {
                    dragAxis = if (abs(dragAmount.x) >= abs(dragAmount.y)) {
                        DragAxis.HORIZONTAL
                    } else {
                        DragAxis.VERTICAL
                    }
                    if (dragAxis == DragAxis.HORIZONTAL && !markedSelected) {
                        latestSelectionUpdater(latestActiveDimensionId, true)
                        markedSelected = true
                    }
                }
                when (dragAxis) {
                    DragAxis.HORIZONTAL -> {
                        accumulatedX += dragAmount.x * horizontalSensitivity
                        val steps = (accumulatedX / pxPerScore).toInt()
                        if (steps != 0) {
                            currentValue =
                                (currentValue + steps).coerceIn(SCORE_MIN, SCORE_MAX)
                            latestScoreUpdater(currentValue)
                            if (!markedSelected) {
                                latestSelectionUpdater(latestActiveDimensionId, true)
                                markedSelected = true
                            }
                            accumulatedX -= steps * pxPerScore
                        }
                    }

                    DragAxis.VERTICAL -> {
                        accumulatedY += dragAmount.y * verticalSensitivity
                        while (abs(accumulatedY) >= verticalStepPx) {
                            val step = if (accumulatedY > 0f) 1 else -1
                            val nextIndex = (currentIndex + step).coerceIn(0, latestMaxIndex)
                            if (nextIndex != currentIndex) {
                                currentIndex = nextIndex
                                latestIndexUpdater(currentIndex)
                                verticalStepTriggered = true
                            }
                            accumulatedY -= step * verticalStepPx
                        }
                    }

                    null -> Unit
                }
            }
        )
    }
    val hostView = LocalView.current
    val criteriaTouchLockModifier = Modifier.pointerInteropFilter { motionEvent ->
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                requestDisallowParentIntercept(hostView, true)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                requestDisallowParentIntercept(hostView, false)
            }
        }
        false
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomContentPadding)
                .zIndex(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactDimensionList(
                dimensions = dimensions,
                activeIndex = activeIndexSafe,
                scores = state,
                selectedIds = selectedIds,
                onToggleSelected = { id, selected -> onSelectionChange(id, selected) },
                onSelect = { index ->
                    activeIndex = index
                },
                modifier = Modifier
                    .then(criteriaTouchLockModifier)
                    .then(gestureModifier)
            )

            val showLeftContent = hasSuggestedLeft || selectedHistoryEntryLeft != null
            val showRightContent = hasSuggestedRight || selectedHistoryEntryRight != null
            if (showLeftContent || showRightContent) {
                val effectiveLeftTitle = if (hasSuggestedLeft) {
                    suggestedLeftTitle
                } else {
                    selectedHistoryEntryLeft?.streamEntity?.title.orEmpty()
                }
                val effectiveLeftUploader = if (hasSuggestedLeft) {
                    suggestedLeftUploader
                } else {
                    selectedHistoryEntryLeft?.streamEntity?.uploader.orEmpty()
                }
                val effectiveRightTitle = if (hasSuggestedRight) {
                    suggestedRightTitle
                } else {
                    selectedHistoryEntryRight?.streamEntity?.title.orEmpty()
                }
                val effectiveRightUploader = if (hasSuggestedRight) {
                    suggestedRightUploader
                } else {
                    selectedHistoryEntryRight?.streamEntity?.uploader.orEmpty()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = {
                                    if (!hasSuggestedLeft) {
                                        openHistoryOverlay(OverlayTarget.LEFT)
                                    }
                                },
                                onLongClick = {
                                    if (hasSuggestedLeft) {
                                        val sl = state.suggestedLeft
                                        if (sl?.videoUrl != null) {
                                            onNavigateToVideo?.invoke(
                                                CompareRepository.uidToServiceId(sl.uid),
                                                sl.videoUrl,
                                                sl.title
                                            )
                                        }
                                    } else {
                                        selectedHistoryEntryLeft?.streamEntity?.let {
                                            onNavigateToVideo?.invoke(it.serviceId, it.url, it.title)
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        CompareSideLabel(
                            title = effectiveLeftTitle,
                            uploader = effectiveLeftUploader,
                            textAlign = TextAlign.End
                        )
                    }

                    Box(
                        modifier = Modifier.weight(2f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (showLeftContent) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        if (!hasSuggestedLeft) {
                                                            openHistoryOverlay(OverlayTarget.LEFT)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (hasSuggestedLeft) {
                                                            val sl = state.suggestedLeft
                                                            if (sl?.videoUrl != null) {
                                                                onNavigateToVideo?.invoke(
                                                                    CompareRepository.uidToServiceId(sl.uid),
                                                                    sl.videoUrl,
                                                                    sl.title
                                                                )
                                                            }
                                                        } else {
                                                            selectedHistoryEntryLeft?.streamEntity?.let {
                                                                onNavigateToVideo?.invoke(it.serviceId, it.url, it.title)
                                                            }
                                                        }
                                                    }
                                                ),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(2.dp, Color(0xFF42A5F5)),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                if (hasSuggestedLeft) {
                                                    SuggestedVideoThumbnail(
                                                        thumbnailUrl = state.suggestedLeft?.thumbnailUrl,
                                                        thumbnailHeight = 52.dp
                                                    )
                                                } else {
                                                    CompareVideoThumbnailCard(
                                                        entry = selectedHistoryEntryLeft!!,
                                                        thumbnailHeight = 52.dp,
                                                        showMeta = false,
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                                if (isLeftCurrent) {
                                                    CurrentBadge(
                                                        modifier = Modifier.align(Alignment.TopEnd)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                if (showRightContent) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        if (!hasSuggestedRight) {
                                                            openHistoryOverlay(OverlayTarget.RIGHT)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (hasSuggestedRight) {
                                                            val sr = state.suggestedRight
                                                            if (sr?.videoUrl != null) {
                                                                onNavigateToVideo?.invoke(
                                                                    CompareRepository.uidToServiceId(sr.uid),
                                                                    sr.videoUrl,
                                                                    sr.title
                                                                )
                                                            }
                                                        } else {
                                                            selectedHistoryEntryRight?.streamEntity?.let {
                                                                onNavigateToVideo?.invoke(it.serviceId, it.url, it.title)
                                                            }
                                                        }
                                                    }
                                                ),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(2.dp, Color(0xFFE57373)),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                if (hasSuggestedRight) {
                                                    SuggestedVideoThumbnail(
                                                        thumbnailUrl = state.suggestedRight?.thumbnailUrl,
                                                        thumbnailHeight = 52.dp
                                                    )
                                                } else {
                                                    CompareVideoThumbnailCard(
                                                        entry = selectedHistoryEntryRight!!,
                                                        thumbnailHeight = 52.dp,
                                                        showMeta = false,
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                                if (isRightCurrent) {
                                                    CurrentBadge(
                                                        modifier = Modifier.align(Alignment.TopEnd)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(colorResource(R.color.tournesol_chip_bg_selected), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "VS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF1B1B1B)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = {
                                    if (!hasSuggestedRight) {
                                        openHistoryOverlay(OverlayTarget.RIGHT)
                                    }
                                },
                                onLongClick = {
                                    if (hasSuggestedRight) {
                                        val sr = state.suggestedRight
                                        if (sr?.videoUrl != null) {
                                            onNavigateToVideo?.invoke(
                                                CompareRepository.uidToServiceId(sr.uid),
                                                sr.videoUrl,
                                                sr.title
                                            )
                                        }
                                    } else {
                                        selectedHistoryEntryRight?.streamEntity?.let {
                                            onNavigateToVideo?.invoke(it.serviceId, it.url, it.title)
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        CompareSideLabel(
                            title = effectiveRightTitle,
                            uploader = effectiveRightUploader,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colorResource(R.color.tournesol_chip_bg_selected),
                        containerColor = colorResource(R.color.tournesol_chip_bg_unselected),
                        selectedLabelColor = colorResource(R.color.tournesol_chip_text_selected),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                    val chipShape = RoundedCornerShape(8.dp)
                    val chipTextStyle = TextStyle(
                        fontFamily = FontFamily(Typeface.create("sans-serif-medium", Typeface.NORMAL)),
                        fontSize = 14.sp,
                        letterSpacing = TextUnit(0.04f, TextUnitType.Em)
                    )
                    FilterChip(
                        selected = true,
                        onClick = submitOrUpdateAction,
                        enabled = canSubmit,
                        label = { Text(text = submitButtonLabel, style = chipTextStyle) },
                        colors = chipColors,
                        shape = chipShape,
                        border = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val diceEnabled = !state.suggestionsLoading
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = diceEnabled) { onRandomizeLeft() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF42A5F5).copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.ic_casino),
                                contentDescription = stringResource(R.string.compare_randomize_left),
                                colorFilter = ColorFilter.tint(Color(0xFF42A5F5)),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = diceEnabled) { onRandomizeRight() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE57373).copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(R.drawable.ic_casino),
                                contentDescription = stringResource(R.string.compare_randomize_right),
                                colorFilter = ColorFilter.tint(Color(0xFFE57373)),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (state.suggestionsLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }
            }
        }

        CompactHeader(
            description = activeDescription,
            iconRes = activeDimension.iconRes,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = bottomContentPadding)
                .zIndex(2f)
        )

        if (hasOpenedHistoryOverlay) {
            val accentColor = if (activeOverlayTarget == OverlayTarget.RIGHT) {
                Color(0xFFE57373)
            } else {
                Color(0xFF42A5F5)
            }
            val overlayBackground = Color(0xFF0B0B0B)
            val overlayRowSpacing = 6.dp
            val overlayColumnSpacing = 6.dp
            val overlayCardHeight = 54.dp
            val overlayRowSpacingPx = with(density) { overlayRowSpacing.roundToPx() }
            val overlayColumnSpacingPx = with(density) { overlayColumnSpacing.roundToPx() }
            val overlayCardHeightPx = with(density) { overlayCardHeight.roundToPx() }
            val overlayBody: @Composable () -> Unit = {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val visibleRowsPerPage = if (overlayGridBounds == null) {
                        overlayRowsPerPage.coerceAtLeast(1).coerceAtMost(16)
                    } else {
                        val gridViewportHeightPx = overlayGridBounds!!.height
                            .roundToInt()
                            .coerceAtLeast(0)
                        (
                            (gridViewportHeightPx + overlayRowSpacingPx) /
                                (overlayCardHeightPx + overlayRowSpacingPx)
                            )
                            .coerceAtLeast(1)
                            .coerceAtMost(16)
                    }
                    LaunchedEffect(visibleRowsPerPage) {
                        if (overlayRowsPerPage != visibleRowsPerPage) {
                            overlayRowsPerPage = visibleRowsPerPage
                        }
                    }
                    val visiblePageSize = overlayGridColumns * visibleRowsPerPage
                    val pageCount = ((overlayEntries.size + visiblePageSize - 1) / visiblePageSize)
                        .coerceAtLeast(1)
                    val currentPage = (historyOverlayIndex / visiblePageSize)
                        .coerceIn(0, pageCount - 1)
                    LaunchedEffect(currentPage) {
                        if (historyOverlayPage != currentPage) {
                            historyOverlayPage = currentPage
                        }
                    }
                    val pageStart = currentPage * visiblePageSize
                    val pageEnd = kotlin.math.min(pageStart + visiblePageSize, overlayEntries.size)
                    val pageEntries = overlayEntries.subList(pageStart, pageEnd)
                    val selectedAbsoluteIndex = (overlayDragPreviewIndex ?: historyOverlayIndex)
                        .coerceIn(pageStart, (pageEnd - 1).coerceAtLeast(pageStart))
                    val selectedOffsetInPage = (selectedAbsoluteIndex - pageStart)
                        .coerceIn(0, (pageEntries.size - 1).coerceAtLeast(0))
                    val pageRows = ((pageEntries.size + overlayGridColumns - 1) / overlayGridColumns)
                        .coerceAtLeast(1)
                    LaunchedEffect(
                        showHistoryOverlay,
                        activeOverlayTarget,
                        currentPage,
                        visiblePageSize,
                        overlayEntries.size
                    ) {
                        if (!showHistoryOverlay || overlayEntries.isEmpty()) {
                            return@LaunchedEffect
                        }

                        val pagesToPrefetch = LinkedHashSet<Int>().apply {
                            add(currentPage)
                            if (currentPage < pageCount - 1) {
                                add(currentPage + 1)
                            }
                        }
                        val urlsToPrefetch = LinkedHashSet<String>()
                        pagesToPrefetch.forEach { page ->
                            val start = page * visiblePageSize
                            val end = kotlin.math.min(start + visiblePageSize, overlayEntries.size)
                            for (index in start until end) {
                                val url = overlayEntries[index].streamEntity.thumbnailUrl
                                if (!url.isNullOrBlank()) {
                                    urlsToPrefetch.add(url)
                                }
                            }
                        }

                        val imageLoader = context.imageLoader
                        urlsToPrefetch.forEach { url ->
                            imageLoader.enqueue(
                                ImageRequest.Builder(context)
                                    .data(url)
                                    .memoryCacheKey(url)
                                    .diskCacheKey(url)
                                    .build()
                            )
                        }
                    }
                    LaunchedEffect(pageStart, pageEnd) {
                        overlayDragPreviewIndex = null
                    }
                    val hitTestOverlayIndex: (Offset) -> Int? = { localPosition ->
                        val gridWidthPx = overlayGridSize.width.toFloat()
                        val gridHeightPx = overlayGridSize.height.toFloat()
                        if (gridWidthPx <= 0f ||
                            gridHeightPx <= 0f ||
                            localPosition.x < 0f ||
                            localPosition.y < 0f ||
                            localPosition.x > gridWidthPx ||
                            localPosition.y > gridHeightPx
                        ) {
                            null
                        } else {
                            val rowStridePx = overlayCardHeightPx + overlayRowSpacingPx
                            val row = (localPosition.y / rowStridePx).toInt()
                            val yInRow = localPosition.y - row * rowStridePx
                            if (yInRow > overlayCardHeightPx) {
                                null
                            } else {
                                val cardWidthPx = (
                                    gridWidthPx - overlayColumnSpacingPx * (overlayGridColumns - 1)
                                    ) / overlayGridColumns
                                if (cardWidthPx <= 0f) {
                                    null
                                } else {
                                    val colStridePx = cardWidthPx + overlayColumnSpacingPx
                                    val col = (localPosition.x / colStridePx).toInt()
                                    if (col < 0 || col >= overlayGridColumns) {
                                        null
                                    } else {
                                        val xInCol = localPosition.x - col * colStridePx
                                        if (xInCol > cardWidthPx) {
                                            null
                                        } else {
                                            val offsetInPage = row * overlayGridColumns + col
                                            if (offsetInPage < 0 || offsetInPage >= pageEntries.size) {
                                                null
                                            } else {
                                                pageStart + offsetInPage
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = accentColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(
                                    text = "${currentPage + 1}/$pageCount",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFFEDEDED),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                            Image(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.close),
                                colorFilter = ColorFilter.tint(Color(0xFFEDEDED)),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(22.dp)
                                    .clickable { showHistoryOverlay = false }
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .onGloballyPositioned { coordinates ->
                                    if (showHistoryOverlay) {
                                        val bounds = coordinates.boundsInRoot()
                                        overlayGridBounds = bounds
                                        overlayGridSize = coordinates.size
                                    }
                                }
                                .pointerInput(
                                    showHistoryOverlay,
                                    pageStart,
                                    pageEnd,
                                    pageEntries.size,
                                    overlayGridSize,
                                    overlayGridColumns,
                                    overlayCardHeightPx,
                                    overlayRowSpacingPx,
                                    overlayColumnSpacingPx
                                ) {
                                    if (!showHistoryOverlay) {
                                        return@pointerInput
                                    }
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var pointerId = down.id
                                        var releasedOnIndex: Int? = null
                                        var previewIndex = hitTestOverlayIndex(down.position)
                                        overlayDragPreviewIndex = previewIndex
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes
                                                .firstOrNull { it.id == pointerId }
                                                ?: break
                                            pointerId = change.id
                                            val hoveredIndex = hitTestOverlayIndex(change.position)
                                            val insideGrid = change.position.x >= 0f &&
                                                change.position.y >= 0f &&
                                                change.position.x <= overlayGridSize.width.toFloat() &&
                                                change.position.y <= overlayGridSize.height.toFloat()
                                            previewIndex = when {
                                                hoveredIndex != null -> hoveredIndex
                                                insideGrid -> previewIndex
                                                else -> null
                                            }
                                            overlayDragPreviewIndex = previewIndex
                                            if (!change.pressed) {
                                                releasedOnIndex = hoveredIndex
                                                break
                                            }
                                        }
                                        overlayDragPreviewIndex = null
                                        if (releasedOnIndex != null) {
                                            val absoluteIndex = releasedOnIndex!!
                                            when (activeOverlayTarget) {
                                                OverlayTarget.LEFT -> leftHistoryIndex = absoluteIndex
                                                OverlayTarget.RIGHT -> rightHistoryIndex = absoluteIndex
                                            }
                                            historyOverlayIndex = absoluteIndex
                                            showHistoryOverlay = false
                                        }
                                    }
                                },
                            verticalArrangement = Arrangement.spacedBy(overlayRowSpacing)
                        ) {
                            for (row in 0 until pageRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(overlayColumnSpacing)
                                ) {
                                    for (column in 0 until overlayGridColumns) {
                                        val offsetInPage = row * overlayGridColumns + column
                                        if (offsetInPage < pageEntries.size) {
                                            val absoluteIndex = pageStart + offsetInPage
                                            CompareHistoryOverlayGridCard(
                                                entry = pageEntries[offsetInPage],
                                                selected = offsetInPage == selectedOffsetInPage,
                                                accentColor = accentColor,
                                                onClick = {
                                                    when (activeOverlayTarget) {
                                                        OverlayTarget.LEFT -> leftHistoryIndex = absoluteIndex
                                                        OverlayTarget.RIGHT -> rightHistoryIndex = absoluteIndex
                                                    }
                                                    historyOverlayIndex = absoluteIndex
                                                    showHistoryOverlay = false
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(overlayCardHeight)
                                            )
                                        } else {
                                            Spacer(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(overlayCardHeight)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (pageCount > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val targetPage = (currentPage - 1).coerceAtLeast(0)
                                        val targetStart = targetPage * visiblePageSize
                                        val targetPageCount = kotlin.math.min(
                                            visiblePageSize,
                                            overlayEntries.size - targetStart
                                        ).coerceAtLeast(1)
                                        val targetOffset = selectedOffsetInPage.coerceIn(
                                            0,
                                            targetPageCount - 1
                                        )
                                        historyOverlayPage = targetPage
                                        historyOverlayIndex = targetStart + targetOffset
                                    },
                                    enabled = currentPage > 0,
                                    contentPadding = PaddingValues(
                                        horizontal = 14.dp,
                                        vertical = 4.dp
                                    )
                                ) {
                                    Text("<<")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        val targetPage = (currentPage + 1).coerceAtMost(pageCount - 1)
                                        val targetStart = targetPage * visiblePageSize
                                        val targetPageCount = kotlin.math.min(
                                            visiblePageSize,
                                            overlayEntries.size - targetStart
                                        ).coerceAtLeast(1)
                                        val targetOffset = selectedOffsetInPage.coerceIn(
                                            0,
                                            targetPageCount - 1
                                        )
                                        historyOverlayPage = targetPage
                                        historyOverlayIndex = targetStart + targetOffset
                                    },
                                    enabled = currentPage < pageCount - 1,
                                    contentPadding = PaddingValues(
                                        horizontal = 14.dp,
                                        vertical = 4.dp
                                    )
                                ) {
                                    Text(">>")
                                }
                            }
                        }
                    }
                }
            }
            if (state.compactPopupVisible) {
                val overlayModifier = if (showHistoryOverlay) {
                    Modifier
                        .fillMaxSize()
                        .background(overlayBackground)
                        .zIndex(4f)
                } else {
                    Modifier
                        .size(1.dp)
                        .graphicsLayer { alpha = 0f }
                        .zIndex(-1f)
                }
                Box(
                    modifier = overlayModifier,
                    contentAlignment = Alignment.TopCenter
                ) {
                    overlayBody()
                }
            } else if (showHistoryOverlay) {
                Dialog(
                    onDismissRequest = { showHistoryOverlay = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayBackground)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        overlayBody()
                    }
                }
            }
        }
    }

    if (state.showRecommendationsDialog) {
        CompareComparisonsFullScreen(
            recommendations = state.recommendations,
            totalCount = state.recommendationsTotalCount,
            isLoading = state.recommendationsLoading,
            errorMessage = state.recommendationsError,
            onDismiss = onDismissRecommendations
        )
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
    description: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            textAlign = TextAlign.Start,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun CompareComparisonsFullScreen(
    recommendations: List<CompareRecommendationItem>,
    totalCount: Int? = null,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .zIndex(6f)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val title = if (totalCount != null) {
                    stringResource(R.string.compare_you_submitted, totalCount)
                } else {
                    stringResource(R.string.compare_title)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onDismiss)
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                recommendations.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.compare_no_comparisons_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 10.dp)
                    ) {
                        itemsIndexed(
                            recommendations,
                            key = { index, item -> "${item.uid}-$index" }
                        ) { index, item ->
                            CompareComparisonRow(item, index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CompareComparisonRow(item: CompareRecommendationItem, index: Int = 0) {
    val score = item.largelyRecommendedScore
    val leftWins = score != null && score > 0
    val rightWins = score != null && score < 0
    val leftBorderColor = Color(0xFF42A5F5)
    val rightBorderColor = Color(0xFFE57373)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (index % 2 == 0) {
            MaterialTheme.colorScheme.surface
        } else {
            colorResource(R.color.tournesol_chip_bg_unselected)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left label (video A)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                CompareSideLabel(
                    title = item.videoA.title,
                    uploader = item.videoA.uploader,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Center: thumbnails + VS badge
            Box(
                modifier = Modifier.weight(2f),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Left thumbnail (video A)
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(2.dp, leftBorderColor),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Box(modifier = Modifier.padding(2.dp)) {
                                    AsyncImage(
                                        model = item.videoA.thumbnailUrl,
                                        contentDescription = null,
                                        placeholder = painterResource(R.drawable.placeholder_thumbnail_video),
                                        error = painterResource(R.drawable.placeholder_thumbnail_video),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                            if (leftWins) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth(0.4f)
                                        .height(2.dp)
                                        .background(Color(0xFFFFD54F), RoundedCornerShape(1.dp))
                                )
                            }
                        }

                        // Right thumbnail (video B)
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(2.dp, rightBorderColor),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Box(modifier = Modifier.padding(2.dp)) {
                                    AsyncImage(
                                        model = item.videoB.thumbnailUrl,
                                        contentDescription = null,
                                        placeholder = painterResource(R.drawable.placeholder_thumbnail_video),
                                        error = painterResource(R.drawable.placeholder_thumbnail_video),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                            if (rightWins) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth(0.4f)
                                        .height(2.dp)
                                        .background(Color(0xFFFFD54F), RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }

                    // VS badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                colorResource(R.color.tournesol_chip_bg_selected),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = Color(0xFF1B1B1B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Right label (video B)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                CompareSideLabel(
                    title = item.videoB.title,
                    uploader = item.videoB.uploader,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun CompareSideLabel(
    title: String,
    uploader: String,
    textAlign: TextAlign
) {
    val titleColor = compareMetaTitleColor()
    val uploaderColor = compareMetaUploaderColor()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = titleColor,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (uploader.isNotBlank()) {
            Text(
                text = uploader,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                ),
                color = uploaderColor,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun compareMetaTitleColor(): Color {
    return MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
}

@Composable
private fun compareMetaUploaderColor(): Color {
    return lerp(
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.primary,
        0.26f
    ).copy(alpha = 0.82f)
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

private fun requestDisallowParentIntercept(view: android.view.View, disallow: Boolean) {
    var currentParent = view.parent
    while (currentParent != null) {
        currentParent.requestDisallowInterceptTouchEvent(disallow)
        currentParent = currentParent.parent
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
    val dimensionLabels = dimensions.map { stringResource(it.labelRes) }
    var firstColumnWidthPx by remember { mutableIntStateOf(0) }
    val firstColumnWidth = with(LocalDensity.current) { firstColumnWidthPx.toDp() }
    val mainCriterionSet =
        selectedIds.contains(COMPACT_MAIN_CRITERION_ID) || scores.score != 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        dimensions.forEachIndexed { index, criterion ->
            val score = dimensionScore(scores, criterion)
            val isMainCriterion = criterion.id == COMPACT_MAIN_CRITERION_ID
            val rowAlpha = if (!mainCriterionSet && !isMainCriterion) 0.5f else 1f
            CompactDimensionRow(
                criterion = criterion,
                criterionLabel = dimensionLabels[index],
                score = score,
                isActive = index == activeIndex,
                isSelected = selectedIds.contains(criterion.id),
                isMainCriterion = isMainCriterion,
                showMainAttention = isMainCriterion && !mainCriterionSet,
                rowAlpha = rowAlpha,
                firstColumnWidth = firstColumnWidth,
                onFirstColumnMeasured = { measuredWidth ->
                    if (measuredWidth > firstColumnWidthPx) {
                        firstColumnWidthPx = measuredWidth
                    }
                },
                onToggleSelected = { selected -> onToggleSelected(criterion.id, selected) },
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun CompactDimensionRow(
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
                horizontal = 10.dp,
                vertical = if (isMainCriterion) 4.dp else 1.dp
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
                text = criterionLabel,
                style = MaterialTheme.typography.labelLarge.copy(
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
                style = MaterialTheme.typography.labelSmall,
                color = scoreColor,
                textAlign = TextAlign.End,
                modifier = Modifier.width(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            MiniScoreBar(
                value = score,
                isActive = isActive,
                barHeight = if (isMainCriterion) 5.dp else 3.dp,
                showAttentionRing = showMainAttention,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun MiniScoreBar(
    value: Int,
    isActive: Boolean,
    barHeight: androidx.compose.ui.unit.Dp,
    showAttentionRing: Boolean = false,
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
    val attentionRotation = if (showAttentionRing) {
        val transition = rememberInfiniteTransition(label = "mainCriterionAttentionRing")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "mainCriterionAttentionRotation"
        ).value
    } else {
        0f
    }
    if (showAttentionRing) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SwipeHintWaveText()
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
            ) {
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
                val strokeWidth = 1.dp.toPx()
                val inset = strokeWidth / 2f
                val ringWidth = size.width - strokeWidth
                val ringHeight = size.height - strokeWidth
                if (ringWidth > 0f && ringHeight > 0f) {
                    rotate(degrees = attentionRotation, pivot = center) {
                        drawRoundRect(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFEF5350),
                                    Color(0xFFE57373),
                                    Color(0xFF4FC3F7),
                                    Color(0xFF64B5F6),
                                    Color(0xFFEF5350)
                                )
                            ),
                            topLeft = Offset(inset, inset),
                            size = Size(ringWidth, ringHeight),
                            cornerRadius = CornerRadius(ringHeight / 2f, ringHeight / 2f),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }
        }
    } else {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(barHeight)
        ) {
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
}

@Composable
private fun SwipeHintWaveText(
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
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.7.sp
                    ),
                    color = animatedColor
                )
            }
        }
    }
}

@Composable
private fun CompareHistoryOverlayGridCard(
    entry: StreamHistoryEntry,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stream = remember(entry) { entry.toStreamInfoItem() }
    val titleColor = compareMetaTitleColor()
    val uploaderColor = compareMetaUploaderColor()
    val cardBackground = Color(0xFF131313)
    val borderColor = if (selected) {
        accentColor
    } else {
        Color(0xFF2A2A2A)
    }
    Surface(
        modifier = Modifier
            .then(modifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = cardBackground,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreamThumbnail(
                stream = stream,
                showProgress = false,
                showDuration = true,
                durationTextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 84.dp, height = 48.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entry.streamEntity.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 12.sp
                    ),
                    color = titleColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.streamEntity.uploader.isNotBlank()) {
                    Text(
                        text = entry.streamEntity.uploader,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = uploaderColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
                modifier = Modifier.clickable(
                    enabled = submitEnabled,
                    onClick = { onSubmit() }
                ),
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
                if (state.submitInProgress) {
                    Spacer(modifier = Modifier.width(8.dp))
                    SubmitSpinner(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (showChange) {
                Spacer(modifier = Modifier.width(14.dp))
                Row(
                    modifier = Modifier.clickable(
                        enabled = changeEnabled,
                        onClick = { onChangeMainScore() }
                    ),
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
                    modifier = Modifier.clickable(
                        enabled = submitMoreEnabled,
                        onClick = { onSubmitMore() }
                    ),
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
                    if (state.submitMoreInProgress) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SubmitSpinner(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (showUpdate) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(
                        modifier = Modifier.clickable(
                            enabled = updateEnabled,
                            onClick = { onSubmitMore() }
                        ),
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

private fun formatScoreMagnitude(score: Int): String {
    return abs(score).toString()
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
    onSelectIndex: (Int) -> Unit,
    onNavigateToVideo: ((Int, String, String) -> Unit)? = null
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
    val totalPages = entries.size

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
                var lastDragTimestamp = 0L
                var smoothedSpeedPxPerSec = 0f
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
                        lastDragTimestamp = 0L
                        smoothedSpeedPxPerSec = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val currentTimestamp = change.uptimeMillis
                        val deltaMs = if (lastDragTimestamp == 0L) {
                            16L
                        } else {
                            (currentTimestamp - lastDragTimestamp).coerceAtLeast(1L)
                        }
                        val instantSpeedPxPerSec = abs(dragAmount.y) * 1000f / deltaMs
                        smoothedSpeedPxPerSec = if (smoothedSpeedPxPerSec == 0f) {
                            instantSpeedPxPerSec
                        } else {
                            smoothedSpeedPxPerSec * 0.75f + instantSpeedPxPerSec * 0.25f
                        }
                        lastDragTimestamp = currentTimestamp

                        // Slow drag => finer control; fast swipe => more travel.
                        val normalizedSpeed =
                            (smoothedSpeedPxPerSec / (pageStepPx * 10f)).coerceIn(0f, 1f)
                        val accelerationMultiplier = 0.72f + normalizedSpeed * 0.83f
                        dragDistance += dragAmount.y * accelerationMultiplier
                        val rawDelta = -dragDistance / pageStepPx
                        val absRawDelta = abs(rawDelta)
                        val visualDelta = if (absRawDelta <= 0.5f) {
                            rawDelta
                        } else {
                            val excess = absRawDelta - 0.5f
                            val dampingSlope = 0.28f + normalizedSpeed * 0.52f
                            val damped = 0.5f + excess * dampingSlope
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
                onLongClick = {
                    val entity = entries[page].streamEntity
                    onNavigateToVideo?.invoke(
                        entity.serviceId,
                        entity.url,
                        entity.title
                    )
                },
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
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/$totalPages",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    entry: StreamHistoryEntry,
    focus: Float,
    contentAlpha: Float,
    backgroundAlpha: Float,
    elevation: androidx.compose.ui.unit.Dp,
    onLongClick: (() -> Unit)? = null,
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
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
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
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                maxLines = 3,
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
private fun CompareVideoThumbnailCard(
    entry: StreamHistoryEntry,
    thumbnailHeight: androidx.compose.ui.unit.Dp = 88.dp,
    showMeta: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop
) {
    val stream = remember(entry) { entry.toStreamInfoItem() }
    val thumbnailDescription = stringResource(R.string.compare_thumbnail_description)
    Column(modifier = if (showMeta) Modifier.fillMaxWidth() else Modifier) {
        StreamThumbnail(
            stream = stream,
            showProgress = false,
            durationAlignment = Alignment.BottomEnd,
            durationTextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            contentScale = contentScale,
            modifier = Modifier
                .then(
                    if (showMeta) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.aspectRatio(16f / 9f)
                    }
                )
                .height(thumbnailHeight)
                .semantics {
                    contentDescription = thumbnailDescription
                }
        )
        if (showMeta) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stream.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
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
}

@Composable
private fun SuggestedVideoThumbnail(
    thumbnailUrl: String?,
    thumbnailHeight: androidx.compose.ui.unit.Dp = 52.dp
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .build(),
        imageLoader = context.imageLoader,
        contentDescription = stringResource(R.string.compare_thumbnail_description),
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .height(thumbnailHeight)
    )
}

@Composable
private fun CurrentBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 2.dp, end = 2.dp)
            .background(Color(0xCC1B1B1B), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = stringResource(R.string.compare_current_label),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp
            ),
            color = Color.White
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
