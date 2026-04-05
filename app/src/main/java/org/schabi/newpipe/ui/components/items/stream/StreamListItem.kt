package org.schabi.newpipe.ui.components.items.stream

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.ui.theme.AppTheme
import org.schabi.newpipe.util.TournesolHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StreamListItem(
    stream: StreamInfoItem,
    showProgress: Boolean,
    isSelected: Boolean,
    onClick: (StreamInfoItem) -> Unit = {},
    onLongClick: (StreamInfoItem) -> Unit = {},
    onDismissPopup: () -> Unit = {}
) {
    // Box serves as an anchor for the dropdown menu
    Box(
        modifier = Modifier
            .combinedClickable(onLongClick = { onLongClick(stream) }, onClick = { onClick(stream) })
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreamThumbnail(
                stream = stream,
                showProgress = showProgress,
                modifier = Modifier.size(width = 140.dp, height = 78.dp)
            )

            Column {
                Text(
                    text = stream.name,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2
                )

                Text(text = stream.uploaderName.orEmpty(), style = MaterialTheme.typography.bodySmall)

                val tournesolScore = stream.tournesolScore
                if (tournesolScore != null) {
                    val isUnsafe = stream.tournesolUnsafeReasons.isNotEmpty()
                    val hasInsufficientReason = TournesolHelper.hasInsufficientReason(
                        stream.tournesolUnsafeReasons
                    )
                    val scoreText = stringResource(
                        R.string.tournesol_score_label,
                        tournesolScore.toString()
                    )
                    Row(
                        modifier = Modifier.alpha(if (isUnsafe) 0.5f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasInsufficientReason) {
                            Text(text = "\uD83C\uDF31", fontSize = 18.sp)
                        } else {
                            Image(
                                painter = painterResource(R.drawable.logo_small),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = scoreText,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD1B65C)
                        )
                    }
                }

                Text(
                    text = getStreamInfoDetail(stream),
                    style = MaterialTheme.typography.bodySmall
                )

                val hasContributors = stream.tournesolNContributors >= 0
                val hasComparisons = stream.tournesolNComparisons >= 0
                val bestIcon = criteriaIcon(stream.tournesolBestCriteria)
                val worstIcon = criteriaIcon(stream.tournesolWorstCriteria)
                if (hasContributors || hasComparisons || bestIcon != null || worstIcon != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.alpha(0.6f)
                    ) {
                        if (hasComparisons || hasContributors) {
                            val parts = buildString {
                                if (hasContributors) {
                                    append(stream.tournesolNContributors)
                                    append(" voters")
                                }
                                if (hasComparisons) {
                                    if (hasContributors) append(" · ")
                                    append(stream.tournesolNComparisons)
                                    append(" votes")
                                }
                            }
                            Text(
                                text = parts,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (bestIcon != null) {
                            if (hasContributors || hasComparisons) {
                                Text(text = "·", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "▲",
                                fontSize = 10.sp,
                                color = Color(0xFF6B9E6F)
                            )
                            Image(
                                painter = painterResource(bestIcon),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (worstIcon != null) {
                            Text(
                                text = "▼",
                                fontSize = 10.sp,
                                color = Color(0xFFC07070)
                            )
                            Image(
                                painter = painterResource(worstIcon),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        StreamMenu(stream, isSelected, onDismissPopup)
    }
}

@DrawableRes
private fun criteriaIcon(criteria: String?): Int? = when (criteria) {
    "reliability" -> R.drawable.reliability
    "pedagogy" -> R.drawable.pedagogy
    "importance" -> R.drawable.importance
    "layman_friendly" -> R.drawable.layman_friendly
    "entertaining_relaxing" -> R.drawable.entertaining_relaxing
    "engaging" -> R.drawable.engaging
    "diversity_inclusion" -> R.drawable.diversity_inclusion
    "better_habits" -> R.drawable.better_habits
    "backfire_risk" -> R.drawable.backfire_risk
    else -> null
}

@Preview(name = "Light mode", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StreamListItemPreview(
    @PreviewParameter(StreamItemPreviewProvider::class) stream: StreamInfoItem
) {
    AppTheme {
        Surface {
            StreamListItem(stream, showProgress = false, isSelected = false)
        }
    }
}
