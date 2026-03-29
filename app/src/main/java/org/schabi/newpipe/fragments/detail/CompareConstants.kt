package org.schabi.newpipe.fragments.detail

import org.schabi.newpipe.R

internal const val SCORE_MIN = -100
internal const val SCORE_MAX = 100
internal const val REGISTER_URL = "https://tournesol.app/signup"

internal data class CompareCriterion(
    val id: String,
    val labelRes: Int,
    val iconRes: Int
)

internal data class CriteriaScore(
    val criteria: String,
    val score: Int
)

internal data class ComparePairSelection(
    val leftServiceId: Int,
    val leftUrl: String,
    val rightServiceId: Int,
    val rightUrl: String
)

internal val EXTRA_CRITERIA = listOf(
    CompareCriterion(
        id = "reliability",
        labelRes = R.string.compare_criteria_reliability,
        iconRes = R.drawable.reliability
    ),
    CompareCriterion(
        id = "pedagogy",
        labelRes = R.string.compare_criteria_pedagogy,
        iconRes = R.drawable.pedagogy
    ),
    CompareCriterion(
        id = "importance",
        labelRes = R.string.compare_criteria_importance,
        iconRes = R.drawable.importance
    ),
    CompareCriterion(
        id = "layman_friendly",
        labelRes = R.string.compare_criteria_layman_friendly,
        iconRes = R.drawable.layman_friendly
    ),
    CompareCriterion(
        id = "entertaining_relaxing",
        labelRes = R.string.compare_criteria_entertaining_relaxing,
        iconRes = R.drawable.entertaining_relaxing
    ),
    CompareCriterion(
        id = "engaging",
        labelRes = R.string.compare_criteria_engaging,
        iconRes = R.drawable.engaging
    ),
    CompareCriterion(
        id = "diversity_inclusion",
        labelRes = R.string.compare_criteria_diversity_inclusion,
        iconRes = R.drawable.diversity_inclusion
    ),
    CompareCriterion(
        id = "better_habits",
        labelRes = R.string.compare_criteria_better_habits,
        iconRes = R.drawable.better_habits
    ),
    CompareCriterion(
        id = "backfire_risk",
        labelRes = R.string.compare_criteria_backfire_risk,
        iconRes = R.drawable.backfire_risk
    )
)

internal fun defaultExtraScores(): Map<String, Int> {
    return EXTRA_CRITERIA.associate { it.id to 0 }
}
