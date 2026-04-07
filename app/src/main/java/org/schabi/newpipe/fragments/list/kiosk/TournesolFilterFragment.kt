package org.schabi.newpipe.fragments.list.kiosk

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.schabi.newpipe.R
import org.schabi.newpipe.ui.theme.AppTheme
import org.schabi.newpipe.util.TournesolHelper

class TournesolFilterFragment : BottomSheetDialogFragment() {
    interface FilterListener {
        fun onApply(
            languages: List<String>,
            dateKey: String,
            includeLowScoreVideos: Boolean,
            durationMinSeconds: Int,
            durationMaxSeconds: Int,
            weightLargelyRecommended: Int,
            weightReliability: Int,
            weightImportance: Int,
            weightPedagogy: Int,
            weightLaymanFriendly: Int,
            weightEntertainingRelaxing: Int,
            weightEngaging: Int,
            weightDiversityInclusion: Int,
            weightBetterHabits: Int,
            weightBackfireRisk: Int
        )
    }

    private var listener: FilterListener? = null
    private var initialLanguages: List<String>? = null
    private var initialDateKey: String? = null
    private var initialIncludeLowScoreVideos: Boolean =
        TournesolHelper.DEFAULT_TOURNESOL_FILTER_INCLUDE_LOW_SCORE
    private var initialDurationMinSeconds: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_DURATION_MIN
    private var initialDurationMaxSeconds: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_DURATION_MAX
    private var initialWeightLargelyRecommended: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightReliability: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightImportance: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightPedagogy: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightLaymanFriendly: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightEntertainingRelaxing: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightEngaging: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightDiversityInclusion: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightBetterHabits: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var initialWeightBackfireRisk: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT

    fun setListener(listener: FilterListener) {
        this.listener = listener
    }

    fun setInitialData(
        languages: List<String>,
        dateKey: String,
        includeLowScoreVideos: Boolean,
        durationMinSeconds: Int,
        durationMaxSeconds: Int,
        weightLargelyRecommended: Int,
        weightReliability: Int,
        weightImportance: Int,
        weightPedagogy: Int,
        weightLaymanFriendly: Int,
        weightEntertainingRelaxing: Int,
        weightEngaging: Int,
        weightDiversityInclusion: Int,
        weightBetterHabits: Int,
        weightBackfireRisk: Int
    ) {
        this.initialLanguages = languages
        this.initialDateKey = dateKey
        this.initialIncludeLowScoreVideos = includeLowScoreVideos
        this.initialDurationMinSeconds = durationMinSeconds
        this.initialDurationMaxSeconds = durationMaxSeconds
        this.initialWeightLargelyRecommended = weightLargelyRecommended
        this.initialWeightReliability = weightReliability
        this.initialWeightImportance = weightImportance
        this.initialWeightPedagogy = weightPedagogy
        this.initialWeightLaymanFriendly = weightLaymanFriendly
        this.initialWeightEntertainingRelaxing = weightEntertainingRelaxing
        this.initialWeightEngaging = weightEngaging
        this.initialWeightDiversityInclusion = weightDiversityInclusion
        this.initialWeightBetterHabits = weightBetterHabits
        this.initialWeightBackfireRisk = weightBackfireRisk
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            if (bottomSheet != null) {
                // Ensure the parent bottom sheet is transparent so our background shows.
                bottomSheet.setBackgroundResource(android.R.color.transparent)
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val seedLanguages = initialLanguages
            ?: TournesolHelper.getDefaultFilterLanguages().split(",")
        val seedDateKey = initialDateKey ?: TournesolHelper.DEFAULT_TOURNESOL_FILTER_DATE_KEY
        val seedIncludeLowScoreVideos = initialIncludeLowScoreVideos

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    TournesolFilterSheet(
                        initialLanguages = seedLanguages,
                        initialDateKey = seedDateKey,
                        initialIncludeLowScoreVideos = seedIncludeLowScoreVideos,
                        initialDurationMinSeconds = initialDurationMinSeconds,
                        initialDurationMaxSeconds = initialDurationMaxSeconds,
                        initialWeightLargelyRecommended = initialWeightLargelyRecommended,
                        initialWeightReliability = initialWeightReliability,
                        initialWeightImportance = initialWeightImportance,
                        initialWeightPedagogy = initialWeightPedagogy,
                        initialWeightLaymanFriendly = initialWeightLaymanFriendly,
                        initialWeightEntertainingRelaxing = initialWeightEntertainingRelaxing,
                        initialWeightEngaging = initialWeightEngaging,
                        initialWeightDiversityInclusion = initialWeightDiversityInclusion,
                        initialWeightBetterHabits = initialWeightBetterHabits,
                        initialWeightBackfireRisk = initialWeightBackfireRisk,
                        onApply = {
                                languages,
                                dateKey,
                                includeLowScoreVideos,
                                durationMin,
                                durationMax,
                                wLR,
                                wRel,
                                wImp,
                                wPed,
                                wLay,
                                wEnt,
                                wEng,
                                wDiv,
                                wBet,
                                wBack
                            ->
                            listener?.onApply(
                                languages,
                                dateKey,
                                includeLowScoreVideos,
                                durationMin,
                                durationMax,
                                wLR, wRel, wImp,
                                wPed, wLay, wEnt, wEng, wDiv, wBet, wBack
                            )
                        },
                        onClose = { dismiss() }
                    )
                }
            }
        }
    }
}

private data class FilterOption(val key: String, @StringRes val labelResId: Int)

private data class DurationPreset(val key: String, @StringRes val labelResId: Int, val minSec: Int, val maxSec: Int)

private val DURATION_PRESETS = listOf(
    DurationPreset("any", R.string.filter_duration_no_limit, -1, -1),
    DurationPreset("short", R.string.filter_duration_short, -1, 240),
    DurationPreset("medium", R.string.filter_duration_medium, 240, 1200),
    DurationPreset("long", R.string.filter_duration_long, 1200, -1)
)

private fun matchDurationPreset(min: Int, max: Int): String = DURATION_PRESETS.firstOrNull { it.minSec == min && it.maxSec == max }?.key ?: "any"

private const val WEIGHT_BOOSTED = 100
private const val WEIGHT_DEFAULT = -1

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TournesolFilterSheet(
    initialLanguages: List<String>,
    initialDateKey: String,
    initialIncludeLowScoreVideos: Boolean,
    initialDurationMinSeconds: Int,
    initialDurationMaxSeconds: Int,
    initialWeightLargelyRecommended: Int,
    initialWeightReliability: Int,
    initialWeightImportance: Int,
    initialWeightPedagogy: Int,
    initialWeightLaymanFriendly: Int,
    initialWeightEntertainingRelaxing: Int,
    initialWeightEngaging: Int,
    initialWeightDiversityInclusion: Int,
    initialWeightBetterHabits: Int,
    initialWeightBackfireRisk: Int,
    onApply: (List<String>, String, Boolean, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int) -> Unit,
    onClose: () -> Unit
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
    val selectedLanguages = remember {
        mutableStateListOf<String>().apply { addAll(initialLanguages) }
    }
    var selectedDateKey by remember { mutableStateOf(initialDateKey) }
    var includeLowScoreVideos by remember { mutableStateOf(initialIncludeLowScoreVideos) }

    var durationKey by remember {
        mutableStateOf(matchDurationPreset(initialDurationMinSeconds, initialDurationMaxSeconds))
    }

    // Weights: -1 means default (not set), 0-100 are explicit values
    var weightLR by remember { mutableStateOf(initialWeightLargelyRecommended) }
    var weightRel by remember { mutableStateOf(initialWeightReliability) }
    var weightImp by remember { mutableStateOf(initialWeightImportance) }
    var weightPed by remember { mutableStateOf(initialWeightPedagogy) }
    var weightLay by remember { mutableStateOf(initialWeightLaymanFriendly) }
    var weightEnt by remember { mutableStateOf(initialWeightEntertainingRelaxing) }
    var weightEng by remember { mutableStateOf(initialWeightEngaging) }
    var weightDiv by remember { mutableStateOf(initialWeightDiversityInclusion) }
    var weightBet by remember { mutableStateOf(initialWeightBetterHabits) }
    var weightBack by remember { mutableStateOf(initialWeightBackfireRisk) }

    fun currentDuration(): Pair<Int, Int> {
        val preset = DURATION_PRESETS.firstOrNull { it.key == durationKey } ?: DURATION_PRESETS[0]
        return preset.minSec to preset.maxSec
    }

    fun applyAll() {
        val (dMin, dMax) = currentDuration()
        onApply(
            selectedLanguages.toList(),
            selectedDateKey,
            includeLowScoreVideos,
            dMin,
            dMax,
            weightLR,
            weightRel,
            weightImp,
            weightPed,
            weightLay,
            weightEnt,
            weightEng,
            weightDiv,
            weightBet,
            weightBack
        )
    }

    val languageOptions = remember {
        listOf(
            FilterOption("en", R.string.language_english),
            FilterOption("fr", R.string.language_french),
            FilterOption("es", R.string.language_spanish),
            FilterOption("de", R.string.language_german),
            FilterOption("it", R.string.language_italian),
            FilterOption("pt", R.string.language_portuguese)
        )
    }
    val dateOptions = remember {
        listOf(
            FilterOption("forever", R.string.date_since_forever),
            FilterOption("year", R.string.date_last_year),
            FilterOption("3_months", R.string.date_last_3_months),
            FilterOption("month", R.string.date_last_month),
            FilterOption("week", R.string.date_last_week),
            FilterOption("day", R.string.date_last_day)
        )
    }

    val criteria: List<Triple<Int, Int, (Int) -> Unit>> = listOf(
        Triple(R.string.filter_criteria_largely_recommended, weightLR, { v -> weightLR = v }),
        Triple(R.string.filter_criteria_reliability, weightRel, { v -> weightRel = v }),
        Triple(R.string.filter_criteria_importance, weightImp, { v -> weightImp = v }),
        Triple(R.string.filter_criteria_pedagogy, weightPed, { v -> weightPed = v }),
        Triple(R.string.filter_criteria_layman_friendly, weightLay, { v -> weightLay = v }),
        Triple(R.string.filter_criteria_entertaining_relaxing, weightEnt, { v -> weightEnt = v }),
        Triple(R.string.filter_criteria_engaging, weightEng, { v -> weightEng = v }),
        Triple(R.string.filter_criteria_diversity_inclusion, weightDiv, { v -> weightDiv = v }),
        Triple(R.string.filter_criteria_better_habits, weightBet, { v -> weightBet = v }),
        Triple(R.string.filter_criteria_backfire_risk, weightBack, { v -> weightBack = v })
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.filter_tournesol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.height(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Languages + low-score chip together under one section
            CompactSectionHeader(stringResource(R.string.filter_languages))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                languageOptions.forEach { option ->
                    val isSelected = selectedLanguages.contains(option.key)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedLanguages.remove(option.key)
                            } else {
                                selectedLanguages.add(option.key)
                            }
                            applyAll()
                        },
                        label = { Text(text = stringResource(option.labelResId), style = chipTextStyle) },
                        colors = chipColors,
                        shape = chipShape,
                        border = null
                    )
                }
                FilterChip(
                    selected = includeLowScoreVideos,
                    onClick = {
                        includeLowScoreVideos = !includeLowScoreVideos
                        applyAll()
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.include_low_score_videos_short),
                            style = chipTextStyle
                        )
                    },
                    colors = chipColors,
                    shape = chipShape,
                    border = null
                )
            }

            // Date
            CompactSectionHeader(stringResource(R.string.filter_date))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dateOptions.forEach { option ->
                    val isSelected = selectedDateKey == option.key
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                selectedDateKey = option.key
                                applyAll()
                            }
                        },
                        label = { Text(text = stringResource(option.labelResId), style = chipTextStyle) },
                        colors = chipColors,
                        shape = chipShape,
                        border = null
                    )
                }
            }

            // Duration: preset chips
            CompactSectionHeader(stringResource(R.string.filter_duration))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DURATION_PRESETS.forEach { preset ->
                    val isSelected = durationKey == preset.key
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                durationKey = preset.key
                                applyAll()
                            }
                        },
                        label = { Text(text = stringResource(preset.labelResId), style = chipTextStyle) },
                        colors = chipColors,
                        shape = chipShape,
                        border = null
                    )
                }
            }

            // Criteria: tap to boost. Each chip toggles weight between default (-1) and boosted (100).
            CompactSectionHeader(stringResource(R.string.filter_criteria))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                criteria.forEach { (labelRes, value, setter) ->
                    val isSelected = value >= 0
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            setter(if (isSelected) WEIGHT_DEFAULT else WEIGHT_BOOSTED)
                            applyAll()
                        },
                        label = { Text(text = stringResource(labelRes), style = chipTextStyle) },
                        colors = chipColors,
                        shape = chipShape,
                        border = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CompactSectionHeader(title: String) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
