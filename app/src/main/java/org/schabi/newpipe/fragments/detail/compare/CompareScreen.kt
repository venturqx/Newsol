package org.schabi.newpipe.fragments.detail.compare

import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import org.schabi.newpipe.R
import org.schabi.newpipe.fragments.detail.CompareUiState
import org.schabi.newpipe.fragments.detail.EXTRA_CRITERIA
import org.schabi.newpipe.fragments.detail.SubmitSpinner
import org.schabi.newpipe.fragments.detail.TournesolLoginDialog

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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scale = computeCompareScale(maxWidth.value)
        CompositionLocalProvider(LocalCompareScale provides scale) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollInterop)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp * scale, vertical = 6.dp * scale),
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

                LollipopPicker(
                    scores = state.extraScores,
                    onScoreChange = onExtraScoreChange,
                    mainScore = state.score,
                    onMainScoreChange = onScoreChange,
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
