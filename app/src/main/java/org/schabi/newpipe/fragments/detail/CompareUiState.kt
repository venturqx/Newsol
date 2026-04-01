package org.schabi.newpipe.fragments.detail

import org.schabi.newpipe.database.history.model.StreamHistoryEntry

data class CompareUiState(
    val historyEntries: List<StreamHistoryEntry> = emptyList(),
    val currentEntry: StreamHistoryEntry? = null,
    val selectedIndex: Int = 0,
    val historyMessageRes: Int? = null,
    val score: Int = 0,
    val extraScores: Map<String, Int> = defaultExtraScores(),
    val storedMainScore: Int? = null,
    val storedExtraScores: Map<String, Int> = emptyMap(),
    val submitted: Boolean = false,
    val submittedConfirmed: Boolean = false,
    val extraSubmitted: Boolean = false,
    val submitInProgress: Boolean = false,
    val changeInProgress: Boolean = false,
    val submitMoreInProgress: Boolean = false,
    val recommendations: List<CompareRecommendationItem> = emptyList(),
    val recommendationsTotalCount: Int? = null,
    val recommendationsLoading: Boolean = false,
    val recommendationsError: String? = null,
    val showRecommendationsDialog: Boolean = false,
    val showLoginDialog: Boolean = false,
    val loginInProgress: Boolean = false,
    val loginError: String? = null,
    val compactPopupVisible: Boolean = false
) {
    val selectedEntry: StreamHistoryEntry?
        get() = historyEntries.getOrNull(selectedIndex)
}
