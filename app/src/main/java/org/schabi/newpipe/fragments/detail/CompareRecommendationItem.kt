package org.schabi.newpipe.fragments.detail

data class CompareComparisonVideo(
    val uid: String,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val videoUrl: String?
)

data class CompareRecommendationItem(
    val uid: String,
    val videoA: CompareComparisonVideo,
    val videoB: CompareComparisonVideo,
    val largelyRecommendedScore: Int?,
    val scoreMax: Int?
)
