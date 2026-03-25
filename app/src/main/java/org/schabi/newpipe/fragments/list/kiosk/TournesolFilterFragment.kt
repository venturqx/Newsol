package org.schabi.newpipe.fragments.list.kiosk

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
            includeLowScoreVideos: Boolean
        )
    }

    private var listener: FilterListener? = null
    private var initialLanguages: List<String>? = null
    private var initialDateKey: String? = null
    private var initialIncludeLowScoreVideos: Boolean =
        TournesolHelper.DEFAULT_TOURNESOL_FILTER_INCLUDE_LOW_SCORE

    fun setListener(listener: FilterListener) {
        this.listener = listener
    }

    fun setInitialData(
        languages: List<String>,
        dateKey: String,
        includeLowScoreVideos: Boolean
    ) {
        this.initialLanguages = languages
        this.initialDateKey = dateKey
        this.initialIncludeLowScoreVideos = includeLowScoreVideos
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
            ?: TournesolHelper.DEFAULT_TOURNESOL_FILTER_LANGUAGES.split(",")
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
                        onApply = { languages, dateKey, includeLowScoreVideos ->
                            listener?.onApply(languages, dateKey, includeLowScoreVideos)
                        },
                        onClose = { dismiss() }
                    )
                }
            }
        }
    }
}

private data class FilterOption(val key: String, @StringRes val labelResId: Int)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TournesolFilterSheet(
    initialLanguages: List<String>,
    initialDateKey: String,
    initialIncludeLowScoreVideos: Boolean,
    onApply: (List<String>, String, Boolean) -> Unit,
    onClose: () -> Unit
) {
    val chipSelectedColor = colorResource(R.color.tournesol_filter_accent)
    val chipStrokeColor = colorResource(R.color.tournesol_chip_stroke)
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = chipSelectedColor,
        containerColor = Color.Transparent,
        selectedLabelColor = Color.White,
        labelColor = MaterialTheme.colorScheme.onSurface
    )
    val selectedLanguages = remember {
        mutableStateListOf<String>().apply { addAll(initialLanguages) }
    }
    var selectedDateKey by remember { mutableStateOf(initialDateKey) }
    var includeLowScoreVideos by remember { mutableStateOf(initialIncludeLowScoreVideos) }

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

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.filter_tournesol),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            FilterSection(stringResource(R.string.filter_content)) {
                FilterChip(
                    selected = includeLowScoreVideos,
                    onClick = {
                        includeLowScoreVideos = !includeLowScoreVideos
                        onApply(selectedLanguages.toList(), selectedDateKey, includeLowScoreVideos)
                    },
                    label = { Text(text = stringResource(R.string.include_low_score_videos)) },
                    colors = chipColors,
                    border = null
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            FilterSection(stringResource(R.string.filter_languages)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                onApply(
                                    selectedLanguages.toList(),
                                    selectedDateKey,
                                    includeLowScoreVideos
                                )
                            },
                            label = { Text(text = stringResource(option.labelResId)) },
                            colors = chipColors,
                            border = null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            FilterSection(stringResource(R.string.filter_date)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateOptions.forEach { option ->
                        val isSelected = selectedDateKey == option.key
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    selectedDateKey = option.key
                                    onApply(
                                        selectedLanguages.toList(),
                                        selectedDateKey,
                                        includeLowScoreVideos
                                    )
                                }
                            },
                            label = { Text(text = stringResource(option.labelResId)) },
                            colors = chipColors,
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}
