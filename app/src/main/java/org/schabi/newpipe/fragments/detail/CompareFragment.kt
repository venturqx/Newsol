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
import org.schabi.newpipe.R
import org.schabi.newpipe.database.history.model.StreamHistoryEntry
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.ktx.serializable
import org.schabi.newpipe.local.history.HistoryRecordManager
import org.schabi.newpipe.ui.theme.AppTheme
import org.schabi.newpipe.util.KEY_INFO
import org.schabi.newpipe.util.TournesolAuthManager

class CompareFragment : Fragment() {
    private var currentInfo: StreamInfo? = null
    private val disposables = CompositeDisposable()

    private var historyEntries by mutableStateOf<List<StreamHistoryEntry>>(emptyList())
    private var historyMessageRes by mutableStateOf<Int?>(R.string.compare_loading_history)
    private var selectedIndex by mutableIntStateOf(0)
    private var selectedStreamId: Long? = null
    private var score by mutableIntStateOf(0)
    private var submitted by mutableStateOf(false)
    private var submitInProgress by mutableStateOf(false)
    private var showLoginDialog by mutableStateOf(false)
    private var loginInProgress by mutableStateOf(false)
    private var loginError by mutableStateOf<String?>(null)
    private var loginDisposable: Disposable? = null
    private val submittedComparisons = LinkedHashSet<CompareKey>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInfo = arguments?.serializable<StreamInfo>(KEY_INFO)
        loadSubmittedComparisons()
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
                    CompareScreen(
                        state = CompareUiState(
                            historyEntries = historyEntries,
                            selectedIndex = selectedIndex,
                            historyMessageRes = historyMessageRes,
                            score = score,
                            submitted = submitted,
                            submitInProgress = submitInProgress,
                            showLoginDialog = showLoginDialog,
                            loginInProgress = loginInProgress,
                            loginError = loginError
                        ),
                        onSelectIndex = { selectIndex(it) },
                        onScoreChange = { score = it },
                        onSubmit = { sendComparison(score) },
                        onDismissLogin = { dismissLoginDialog() },
                        onRegister = { openRegisterPage() },
                        onLogin = { username, password -> performLogin(username, password) }
                    )
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
            submitInProgress = false
            selectedStreamId = newSelectedId
            updateSubmittedState()
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
        submitInProgress = false
        updateSubmittedState()
    }

    private fun isHistoryEnabled(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val key = getString(R.string.enable_watch_history_key)
        return prefs.getBoolean(key, false)
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

    private fun updateSubmittedState() {
        submitted = compareKeyForSelection()?.let(submittedComparisons::contains) == true
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

    private class MissingTokenException : RuntimeException()
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

        @JvmStatic
        fun getInstance(info: StreamInfo): CompareFragment {
            return CompareFragment().apply {
                arguments = bundleOf(KEY_INFO to info)
            }
        }
    }
}
