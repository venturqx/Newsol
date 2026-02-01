package org.schabi.newpipe.fragments.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.json.JSONObject
import org.schabi.newpipe.R
import org.schabi.newpipe.database.history.model.StreamHistoryEntry
import org.schabi.newpipe.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.ktx.serializable
import org.schabi.newpipe.local.history.HistoryRecordManager
import org.schabi.newpipe.ui.theme.AppTheme
import org.schabi.newpipe.util.KEY_INFO
import org.schabi.newpipe.util.TournesolAuthManager
import java.time.OffsetDateTime
import kotlin.math.abs

class CompareFragment : Fragment() {
    private var currentInfo: StreamInfo? = null
    private var useCompactUi = false
    private val disposables = CompositeDisposable()

    private var currentHistoryEntry: StreamHistoryEntry? = null
    private var historyEntries by mutableStateOf<List<StreamHistoryEntry>>(emptyList())
    private var historyMessageRes by mutableStateOf<Int?>(R.string.compare_loading_history)
    private var selectedIndex by mutableIntStateOf(0)
    private var selectedStreamId: Long? = null
    private var score by mutableIntStateOf(0)
    private var extraScores by mutableStateOf(defaultExtraScores())
    private var storedMainScore by mutableStateOf<Int?>(null)
    private var storedExtraScores by mutableStateOf<Map<String, Int>>(emptyMap())
    private var submitted by mutableStateOf(false)
    private var submittedConfirmed by mutableStateOf(false)
    private var extraSubmitted by mutableStateOf(false)
    private var submitInProgress by mutableStateOf(false)
    private var changeInProgress by mutableStateOf(false)
    private var submitMoreInProgress by mutableStateOf(false)
    private var compactPopupVisible by mutableStateOf(false)
    private var showLoginDialog by mutableStateOf(false)
    private var loginInProgress by mutableStateOf(false)
    private var loginError by mutableStateOf<String?>(null)
    private var loginDisposable: Disposable? = null
    private var checkDisposable: Disposable? = null
    private val submittedComparisons = LinkedHashSet<CompareKey>()
    private val storedScores = LinkedHashMap<String, ComparisonScores>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInfo = arguments?.serializable<StreamInfo>(KEY_INFO)
        useCompactUi = arguments?.getBoolean(KEY_COMPACT_UI) == true
        currentHistoryEntry = currentInfo?.let { buildCurrentEntry(it) }
        loadSubmittedComparisons()
        loadStoredScores()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val historyRecordManager = HistoryRecordManager(requireContext())
        if (isHistoryEnabled()) {
            val historySource = historyRecordManager.getStreamHistorySortedById()
            disposables.add(
                historySource
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { entries -> updateHistory(entries) },
                        {
                            historyEntries = emptyList()
                            historyMessageRes = R.string.compare_history_unavailable
                        }
                    )
            )
        } else {
            historyEntries = emptyList()
            historyMessageRes = R.string.compare_history_unavailable
        }
        return ComposeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            ViewCompat.setNestedScrollingEnabled(this, true)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    val uiState = CompareUiState(
                        historyEntries = historyEntries,
                        currentEntry = currentHistoryEntry,
                        selectedIndex = selectedIndex,
                        historyMessageRes = historyMessageRes,
                        score = score,
                        extraScores = extraScores,
                        storedMainScore = storedMainScore,
                        storedExtraScores = storedExtraScores,
                        submitted = submitted,
                        submittedConfirmed = submittedConfirmed,
                        extraSubmitted = extraSubmitted,
                        submitInProgress = submitInProgress,
                        changeInProgress = changeInProgress,
                        submitMoreInProgress = submitMoreInProgress,
                        showLoginDialog = showLoginDialog,
                        loginInProgress = loginInProgress,
                        loginError = loginError
                    )
                    val onExtraScoreChange = { criteria: String, value: Int ->
                        updateExtraScore(criteria, value)
                    }
                    if (useCompactUi) {
                        CompareCompactScreen(
                            state = uiState,
                            onScoreChange = { score = it },
                            onExtraScoreChange = onExtraScoreChange,
                            onSubmitSelected = { selected -> sendCompactSubmit(selected) },
                            onUpdateSelected = { selected -> sendCompactUpdate(selected) },
                            showSubmitButton = compactPopupVisible,
                            onDismissLogin = { dismissLoginDialog() },
                            onRegister = { openRegisterPage() },
                            onLogin = { username, password -> performLogin(username, password) }
                        )
                    } else {
                        CompareScreen(
                            state = uiState,
                            onSelectIndex = { selectIndex(it) },
                            onScoreChange = { score = it },
                            onSubmit = { sendComparison(score) },
                            onChangeMainScore = { sendMainScoreChange() },
                            onExtraScoreChange = onExtraScoreChange,
                            onSubmitMore = { sendAdditionalCriteria() },
                            onDismissLogin = { dismissLoginDialog() },
                            onRegister = { openRegisterPage() },
                            onLogin = { username, password -> performLogin(username, password) }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        loginDisposable?.dispose()
        loginDisposable = null
        super.onDestroyView()
    }

    private fun updateHistory(entries: List<StreamHistoryEntry>) {
        val sorted = entries.sortedByDescending { it.accessDate }
        val current = currentInfo
        val filtered = if (current == null) {
            sorted
        } else {
            sorted.filterNot { entry ->
                entry.streamEntity.serviceId == current.serviceId &&
                    entry.streamEntity.url == current.url
            }
        }
        historyEntries = filtered
        historyMessageRes = if (filtered.isEmpty()) {
            R.string.compare_history_empty
        } else {
            null
        }

        val existingIndex = selectedStreamId?.let { id ->
            filtered.indexOfFirst { it.streamId == id }
        } ?: -1

        val newIndex = when {
            filtered.isEmpty() -> 0
            existingIndex >= 0 -> existingIndex
            else -> 0
        }

        val newSelectedId = filtered.getOrNull(newIndex)?.streamId
        selectedIndex = newIndex
        if (newSelectedId != selectedStreamId) {
            resetSelectionState()
            selectedStreamId = newSelectedId
            refreshSubmittedState()
        }
    }

    private fun selectIndex(newIndex: Int) {
        if (historyEntries.isEmpty()) {
            return
        }
        val clampedIndex = newIndex.coerceIn(0, historyEntries.lastIndex)
        if (clampedIndex == selectedIndex) {
            return
        }
        selectedIndex = clampedIndex
        selectedStreamId = historyEntries[clampedIndex].streamId
        resetSelectionState()
        refreshSubmittedState()
    }

    private fun isHistoryEnabled(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val key = getString(R.string.enable_watch_history_key)
        return prefs.getBoolean(key, false)
    }

    private fun buildCurrentEntry(info: StreamInfo): StreamHistoryEntry {
        val entity = StreamEntity(info)
        val hash = info.url.hashCode().toLong()
        val syntheticId = if (hash == 0L) -1L else -abs(hash)
        return StreamHistoryEntry(
            streamEntity = entity,
            streamId = syntheticId,
            accessDate = OffsetDateTime.now(),
            repeatCount = 0
        )
    }

    private fun showLoginDialog() {
        loginError = null
        loginInProgress = false
        showLoginDialog = true
    }

    private fun dismissLoginDialog() {
        showLoginDialog = false
        loginInProgress = false
        loginError = null
        loginDisposable?.dispose()
        loginDisposable = null
    }

    private fun openRegisterPage() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REGISTER_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        requireContext().startActivity(intent)
    }

    private fun performLogin(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            loginError = getString(R.string.tournesol_login_missing_fields)
            return
        }
        loginError = null
        loginInProgress = true
        loginDisposable?.dispose()
        loginDisposable = TournesolAuthManager.performPasswordLogin(username, password)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { tokenResponse ->
                    TournesolAuthManager.saveAuthState(requireContext(), tokenResponse)
                    loginInProgress = false
                    showLoginDialog = false
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.tournesol_login_success),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                { error ->
                    loginInProgress = false
                    val message = error.message
                    loginError = if (message.isNullOrBlank()) {
                        getString(R.string.tournesol_login_failed)
                    } else {
                        getString(R.string.tournesol_login_failed_with_message, message)
                    }
                }
            )
        loginDisposable?.let { disposables.add(it) }
    }

    private fun sendComparison(score: Int) {
        val info = currentInfo
        if (info == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_current_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val selectedEntry = historyEntries.getOrNull(selectedIndex)
        if (selectedEntry == null) {
            val messageRes = historyMessageRes ?: R.string.compare_history_unavailable
            Toast.makeText(
                requireContext(),
                getString(messageRes),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val lastUid = CompareRepository.buildTournesolUid(
            selectedEntry.streamEntity.url,
            selectedEntry.streamEntity.serviceId
        )
        val currentUid = CompareRepository.buildTournesolUid(
            info.url,
            info.serviceId
        )

        if (lastUid == null || currentUid == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_service_not_supported),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        submitInProgress = true
        disposables.add(
            TournesolAuthManager.getValidAccessToken(requireContext())
                .subscribeOn(Schedulers.io())
                .switchIfEmpty(
                    io.reactivex.rxjava3.core.Maybe.error(MissingTokenException())
                )
                .flatMapSingle { token ->
                    CompareRepository.submitComparison(token, lastUid, currentUid, score)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { messageRes ->
                        submitInProgress = false
                        markSubmitted(lastUid, currentUid)
                        storeSubmittedScores(
                            CompareKey(lastUid, currentUid),
                            mainScore = score
                        )
                        Toast.makeText(
                            requireContext(),
                            getString(messageRes),
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    { throwable ->
                        submitInProgress = false
                        if (throwable is MissingTokenException) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.compare_login_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            showLoginDialog()
                            return@subscribe
                        }
                        val message = throwable.message
                        val errorText = if (message.isNullOrBlank()) {
                            getString(R.string.compare_failed)
                        } else {
                            getString(R.string.compare_failed_with_message, message)
                        }
                        Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
                    }
                )
        )
    }

    private fun sendCompactSubmit(selectedIds: Set<String>) {
        if (submitInProgress) {
            return
        }
        val payload = buildCompactCriteriaPayload(selectedIds) ?: run {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_select_criteria),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val info = currentInfo
        if (info == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_current_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val selectedEntry = historyEntries.getOrNull(selectedIndex)
        if (selectedEntry == null) {
            val messageRes = historyMessageRes ?: R.string.compare_history_unavailable
            Toast.makeText(
                requireContext(),
                getString(messageRes),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val lastUid = CompareRepository.buildTournesolUid(
            selectedEntry.streamEntity.url,
            selectedEntry.streamEntity.serviceId
        )
        val currentUid = CompareRepository.buildTournesolUid(
            info.url,
            info.serviceId
        )

        if (lastUid == null || currentUid == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_service_not_supported),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        submitInProgress = true
        disposables.add(
            TournesolAuthManager.getValidAccessToken(requireContext())
                .subscribeOn(Schedulers.io())
                .switchIfEmpty(
                    io.reactivex.rxjava3.core.Maybe.error(MissingTokenException())
                )
                .flatMapSingle { token ->
                    CompareRepository.submitComparisonWithCriteria(
                        token,
                        lastUid,
                        currentUid,
                        payload.criteriaScores
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { messageRes ->
                        submitInProgress = false
                        markSubmitted(lastUid, currentUid)
                        storeSubmittedScores(
                            CompareKey(lastUid, currentUid),
                            mainScore = payload.mainScore,
                            extraScores = payload.extraScores
                        )
                        Toast.makeText(
                            requireContext(),
                            getString(messageRes),
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    { throwable ->
                        submitInProgress = false
                        if (throwable is MissingTokenException) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.compare_login_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            showLoginDialog()
                            return@subscribe
                        }
                        val message = throwable.message
                        val errorText = if (message.isNullOrBlank()) {
                            getString(R.string.compare_failed)
                        } else {
                            getString(R.string.compare_failed_with_message, message)
                        }
                        Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
                    }
                )
        )
    }

    private fun sendCompactUpdate(selectedIds: Set<String>) {
        if (submitMoreInProgress) {
            return
        }
        val payload = buildCompactCriteriaPayload(selectedIds) ?: run {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_select_criteria),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val info = currentInfo
        if (info == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_current_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val selectedEntry = historyEntries.getOrNull(selectedIndex)
        if (selectedEntry == null) {
            val messageRes = historyMessageRes ?: R.string.compare_history_unavailable
            Toast.makeText(
                requireContext(),
                getString(messageRes),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val lastUid = CompareRepository.buildTournesolUid(
            selectedEntry.streamEntity.url,
            selectedEntry.streamEntity.serviceId
        )
        val currentUid = CompareRepository.buildTournesolUid(
            info.url,
            info.serviceId
        )

        if (lastUid == null || currentUid == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_service_not_supported),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        submitMoreInProgress = true
        disposables.add(
            TournesolAuthManager.getValidAccessToken(requireContext())
                .subscribeOn(Schedulers.io())
                .switchIfEmpty(
                    io.reactivex.rxjava3.core.Maybe.error(MissingTokenException())
                )
                .flatMapSingle { token ->
                    CompareRepository.patchComparison(
                        token,
                        lastUid,
                        currentUid,
                        payload.criteriaScores
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { messageRes ->
                        submitMoreInProgress = false
                        storeSubmittedScores(
                            CompareKey(lastUid, currentUid),
                            mainScore = payload.mainScore,
                            extraScores = payload.extraScores
                        )
                        Toast.makeText(
                            requireContext(),
                            getString(messageRes),
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    { throwable ->
                        submitMoreInProgress = false
                        if (throwable is MissingTokenException) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.compare_login_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            showLoginDialog()
                            return@subscribe
                        }
                        val message = throwable.message
                        val errorText = if (message.isNullOrBlank()) {
                            getString(R.string.compare_failed)
                        } else {
                            getString(R.string.compare_failed_with_message, message)
                        }
                        Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
                    }
                )
        )
    }

    private fun sendMainScoreChange() {
        if (changeInProgress || !submittedConfirmed) {
            return
        }
        val key = compareKeyForSelection() ?: return
        val storedScore = storedScores[key.toStorage()]?.mainScore
        if (storedScore != null && storedScore == score) {
            return
        }

        changeInProgress = true
        disposables.add(
            TournesolAuthManager.getValidAccessToken(requireContext())
                .subscribeOn(Schedulers.io())
                .switchIfEmpty(
                    io.reactivex.rxjava3.core.Maybe.error(MissingTokenException())
                )
                .flatMapSingle { token ->
                    CompareRepository.patchComparison(
                        token,
                        key.lastUid,
                        key.currentUid,
                        listOf(CriteriaScore("largely_recommended", score))
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        changeInProgress = false
                        storeSubmittedScores(key, mainScore = score)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.compare_score_updated),
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    { throwable ->
                        changeInProgress = false
                        if (throwable is MissingTokenException) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.compare_login_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            showLoginDialog()
                            return@subscribe
                        }
                        val message = throwable.message
                        val errorText = if (message.isNullOrBlank()) {
                            getString(R.string.compare_failed)
                        } else {
                            getString(R.string.compare_failed_with_message, message)
                        }
                        Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
                    }
                )
        )
    }

    private fun sendAdditionalCriteria() {
        if (submitMoreInProgress || !submitted) {
            return
        }
        val info = currentInfo
        if (info == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_current_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val selectedEntry = historyEntries.getOrNull(selectedIndex)
        if (selectedEntry == null) {
            val messageRes = historyMessageRes ?: R.string.compare_history_unavailable
            Toast.makeText(
                requireContext(),
                getString(messageRes),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val lastUid = CompareRepository.buildTournesolUid(
            selectedEntry.streamEntity.url,
            selectedEntry.streamEntity.serviceId
        )
        val currentUid = CompareRepository.buildTournesolUid(
            info.url,
            info.serviceId
        )

        if (lastUid == null || currentUid == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_service_not_supported),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val criteriaScores = EXTRA_CRITERIA.map { criterion ->
            CriteriaScore(criterion.id, extraScores[criterion.id] ?: 0)
        }

        submitMoreInProgress = true
        disposables.add(
            TournesolAuthManager.getValidAccessToken(requireContext())
                .subscribeOn(Schedulers.io())
                .switchIfEmpty(
                    io.reactivex.rxjava3.core.Maybe.error(MissingTokenException())
                )
                .flatMapSingle { token ->
                    CompareRepository.patchComparison(
                        token,
                        lastUid,
                        currentUid,
                        criteriaScores
                    )
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { messageRes ->
                        submitMoreInProgress = false
                        storeSubmittedScores(
                            CompareKey(lastUid, currentUid),
                            extraScores = extraScores
                        )
                        Toast.makeText(
                            requireContext(),
                            getString(messageRes),
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    { throwable ->
                        submitMoreInProgress = false
                        if (throwable is MissingTokenException) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.compare_login_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            showLoginDialog()
                            return@subscribe
                        }
                        val message = throwable.message
                        val errorText = if (message.isNullOrBlank()) {
                            getString(R.string.compare_failed)
                        } else {
                            getString(R.string.compare_failed_with_message, message)
                        }
                        Toast.makeText(requireContext(), errorText, Toast.LENGTH_LONG).show()
                    }
                )
        )
    }

    private fun updateExtraScore(criteria: String, value: Int) {
        val updated = extraScores.toMutableMap()
        updated[criteria] = value
        extraScores = updated
    }

    private fun resetSelectionState() {
        submitInProgress = false
        changeInProgress = false
        submitMoreInProgress = false
        score = 0
        extraScores = defaultExtraScores()
        storedMainScore = null
        storedExtraScores = emptyMap()
        extraSubmitted = false
        submittedConfirmed = false
        applyStoredScoresForSelection()
    }

    private fun applyStoredScoresForSelection() {
        val key = compareKeyForSelection() ?: return
        val stored = storedScores[key.toStorage()] ?: return
        storedMainScore = stored.mainScore
        storedExtraScores = stored.extraScores
        extraSubmitted = stored.extraScores.isNotEmpty()
        stored.mainScore?.let { score = it }
        if (stored.extraScores.isNotEmpty()) {
            val updated = defaultExtraScores().toMutableMap()
            stored.extraScores.forEach { (criteria, value) ->
                updated[criteria] = value
            }
            extraScores = updated
        }
    }

    private fun updateSubmittedState() {
        submitted = compareKeyForSelection()?.let(submittedComparisons::contains) == true
        submittedConfirmed = false
    }

    private fun refreshSubmittedState() {
        updateSubmittedState()
        requestRemoteSubmittedCheck()
    }

    private fun requestRemoteSubmittedCheck() {
        val key = compareKeyForSelection() ?: return
        checkDisposable?.dispose()
        checkDisposable = TournesolAuthManager.getValidAccessToken(requireContext())
            .subscribeOn(Schedulers.io())
            .flatMapSingle { token ->
                CompareRepository.checkComparison(token, key.currentUid, key.lastUid)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { exists -> applyRemoteSubmittedResult(key, exists) },
                { /* ignore auto-check errors */ },
                { /* no token, keep local state */ }
            )
        checkDisposable?.let { disposables.add(it) }
    }

    private fun applyRemoteSubmittedResult(key: CompareKey, exists: Boolean) {
        if (compareKeyForSelection() != key) {
            return
        }
        if (exists) {
            markSubmitted(key.lastUid, key.currentUid)
        } else {
            if (submittedComparisons.remove(key)) {
                saveSubmittedComparisons()
            }
            if (storedScores.remove(key.toStorage()) != null) {
                saveStoredScores()
            }
            if (compareKeyForSelection() == key) {
                storedMainScore = null
                storedExtraScores = emptyMap()
                extraSubmitted = false
            }
            submitted = false
            submittedConfirmed = false
        }
    }

    private fun compareKeyForSelection(): CompareKey? {
        val info = currentInfo ?: return null
        val selectedEntry = historyEntries.getOrNull(selectedIndex) ?: return null
        val lastUid = CompareRepository.buildTournesolUid(
            selectedEntry.streamEntity.url,
            selectedEntry.streamEntity.serviceId
        ) ?: return null
        val currentUid = CompareRepository.buildTournesolUid(
            info.url,
            info.serviceId
        ) ?: return null
        return CompareKey(lastUid, currentUid)
    }

    private fun markSubmitted(lastUid: String, currentUid: String) {
        val key = CompareKey(lastUid, currentUid)
        if (submittedComparisons.add(key)) {
            saveSubmittedComparisons()
        }
        submitted = true
        submittedConfirmed = true
    }

    private fun loadSubmittedComparisons() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val stored = prefs.getStringSet(PREF_SUBMITTED_COMPARISONS, emptySet()).orEmpty()
        submittedComparisons.clear()
        stored.mapNotNull(CompareKey.Companion::fromStorage)
            .forEach(submittedComparisons::add)
    }

    private fun saveSubmittedComparisons() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val serialized = submittedComparisons.map(CompareKey::toStorage).toSet()
        prefs.edit().putStringSet(PREF_SUBMITTED_COMPARISONS, serialized).apply()
    }

    private fun loadStoredScores() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val raw = prefs.getString(PREF_COMPARISON_SCORES, null)
        storedScores.clear()
        if (raw.isNullOrBlank()) {
            return
        }
        try {
            val root = JSONObject(raw)
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val entry = root.optJSONObject(key) ?: continue
                val mainScore = if (entry.has("mainScore")) {
                    entry.optInt("mainScore")
                } else {
                    null
                }
                val extraScores = mutableMapOf<String, Int>()
                val extras = entry.optJSONObject("extraScores")
                if (extras != null) {
                    val extraKeys = extras.keys()
                    while (extraKeys.hasNext()) {
                        val criteria = extraKeys.next()
                        extraScores[criteria] = extras.optInt(criteria)
                    }
                }
                storedScores[key] = ComparisonScores(
                    mainScore = mainScore,
                    extraScores = extraScores
                )
            }
        } catch (_: Exception) {
            storedScores.clear()
        }
    }

    private fun saveStoredScores() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val root = JSONObject()
        storedScores.forEach { (key, scores) ->
            val entry = JSONObject()
            scores.mainScore?.let { entry.put("mainScore", it) }
            if (scores.extraScores.isNotEmpty()) {
                val extras = JSONObject()
                scores.extraScores.forEach { (criteria, value) ->
                    extras.put(criteria, value)
                }
                entry.put("extraScores", extras)
            }
            root.put(key, entry)
        }
        prefs.edit().putString(PREF_COMPARISON_SCORES, root.toString()).apply()
    }

    private fun storeSubmittedScores(
        key: CompareKey,
        mainScore: Int? = null,
        extraScores: Map<String, Int>? = null
    ) {
        val storageKey = key.toStorage()
        val existing = storedScores[storageKey]
        val mergedMain = mainScore ?: existing?.mainScore
        val mergedExtra = extraScores ?: existing?.extraScores.orEmpty()
        if (mergedMain == null && mergedExtra.isEmpty()) {
            return
        }
        storedScores[storageKey] = ComparisonScores(
            mainScore = mergedMain,
            extraScores = mergedExtra
        )
        if (compareKeyForSelection() == key) {
            storedMainScore = mergedMain
            storedExtraScores = mergedExtra
            extraSubmitted = mergedExtra.isNotEmpty()
        }
        saveStoredScores()
    }

    private class MissingTokenException : RuntimeException()
    private data class ComparisonScores(
        val mainScore: Int? = null,
        val extraScores: Map<String, Int> = emptyMap()
    )
    fun setCompactPopupVisibleState(visible: Boolean) {
        compactPopupVisible = visible
    }
    private data class CompactCriteriaPayload(
        val criteriaScores: List<CriteriaScore>,
        val mainScore: Int?,
        val extraScores: Map<String, Int>
    )

    private fun buildCompactCriteriaPayload(
        selectedIds: Set<String>
    ): CompactCriteriaPayload? {
        if (selectedIds.isEmpty()) {
            return null
        }
        val criteriaScores = ArrayList<CriteriaScore>(selectedIds.size)
        val extras = LinkedHashMap<String, Int>()
        var mainScore: Int? = null
        selectedIds.forEach { criteria ->
            val value = if (criteria == "largely_recommended") {
                mainScore = score
                score
            } else {
                val extraValue = extraScores[criteria] ?: 0
                extras[criteria] = extraValue
                extraValue
            }
            criteriaScores.add(CriteriaScore(criteria, value))
        }
        if (criteriaScores.isEmpty()) {
            return null
        }
        return CompactCriteriaPayload(
            criteriaScores = criteriaScores,
            mainScore = mainScore,
            extraScores = extras
        )
    }
    private data class CompareKey(val lastUid: String, val currentUid: String) {
        fun toStorage(): String {
            return "${encode(lastUid)}|${encode(currentUid)}"
        }

        companion object {
            fun fromStorage(value: String): CompareKey? {
                val parts = value.split('|', limit = 2)
                if (parts.size != 2) {
                    return null
                }
                val lastUid = decode(parts[0]) ?: return null
                val currentUid = decode(parts[1]) ?: return null
                return CompareKey(lastUid, currentUid)
            }

            private fun encode(value: String): String {
                return Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            }

            private fun decode(value: String): String? {
                return try {
                    String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    companion object {
        private const val PREF_SUBMITTED_COMPARISONS = "compare_submitted_pairs_v1"
        private const val PREF_COMPARISON_SCORES = "compare_submitted_scores_v1"
        private const val KEY_COMPACT_UI = "compare_compact_ui"

        @JvmStatic
        fun getInstance(info: StreamInfo, useCompactUi: Boolean = false): CompareFragment {
            return CompareFragment().apply {
                arguments = bundleOf(
                    KEY_INFO to info,
                    KEY_COMPACT_UI to useCompactUi
                )
            }
        }
    }
}
