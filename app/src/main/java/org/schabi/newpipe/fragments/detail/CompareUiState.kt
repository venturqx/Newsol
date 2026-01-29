package org.schabi.newpipe.fragments.detail

import org.schabi.newpipe.database.history.model.StreamHistoryEntry

data class CompareUiState(
    val historyEntries: List<StreamHistoryEntry> = emptyList(),
    val selectedIndex: Int = 0,
    val historyMessageRes: Int? = null,
    val score: Int = 0,
    val extraScores: Map<String, Int> = defaultExtraScores(),
    val storedMainScore: Int? = null,
    val submitted: Boolean = false,
    val submittedConfirmed: Boolean = false,
    val submitInProgress: Boolean = false,
    val changeInProgress: Boolean = false,
    val submitMoreInProgress: Boolean = false,
    val showLoginDialog: Boolean = false,
    val loginInProgress: Boolean = false,
    val loginError: String? = null
) {
    val selectedEntry: StreamHistoryEntry?
        get() = historyEntries.getOrNull(selectedIndex)
}
