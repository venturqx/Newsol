package org.schabi.newpipe.fragments.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.DownloaderImpl
import org.schabi.newpipe.R
import org.schabi.newpipe.database.history.model.StreamHistoryEntry
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.ktx.serializable
import org.schabi.newpipe.local.history.HistoryRecordManager
import org.schabi.newpipe.ui.components.items.stream.StreamThumbnail
import org.schabi.newpipe.ui.theme.AppTheme
import org.schabi.newpipe.util.KEY_INFO
import org.schabi.newpipe.util.TournesolAuthManager
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt
import android.graphics.Paint as AndroidPaint

class CompareFragment : Fragment() {
    private var currentInfo: StreamInfo? = null
    private val disposables = CompositeDisposable()

    private var historyEntries by mutableStateOf<List<StreamHistoryEntry>>(emptyList())
    private var historyMessageRes by mutableStateOf<Int?>(R.string.compare_loading_history)
    private var selectedIndex by mutableIntStateOf(0)
    private var selectedStreamId: Long? = null
    private var score by mutableIntStateOf(0)
    private var submitted by mutableStateOf(false)
    private var showLoginDialog by mutableStateOf(false)
    private var loginInProgress by mutableStateOf(false)
    private var loginError by mutableStateOf<String?>(null)
    private var loginDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInfo = arguments?.serializable<StreamInfo>(KEY_INFO)
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
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    CompareScreen(
                        historyEntries = historyEntries,
                        selectedIndex = selectedIndex,
                        historyMessageRes = historyMessageRes,
                        score = score,
                        submitted = submitted,
                        showLoginDialog = showLoginDialog,
                        loginInProgress = loginInProgress,
                        loginError = loginError,
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
        if (newSelectedId != selectedStreamId) {
            submitted = false
            selectedStreamId = newSelectedId
        }
        selectedIndex = newIndex
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
        submitted = false
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

        val token = getAccessToken()
        if (token == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_login_required),
                Toast.LENGTH_SHORT
            ).show()
            showLoginDialog()
            return
        }

        val lastUid = buildTournesolUid(
            selectedEntry.streamEntity.url,
            selectedEntry.streamEntity.serviceId
        )
        val currentUid = buildTournesolUid(
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

        disposables.add(
            submitComparison(token, lastUid, currentUid, score)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { messageRes ->
                        submitted = true
                        Toast.makeText(
                            requireContext(),
                            getString(messageRes),
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    { throwable ->
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

    private fun getAccessToken(): String? {
        val authState = TournesolAuthManager.getAuthState(requireContext()) ?: return null
        val token = authState.accessToken
        val expirationTime = authState.accessTokenExpirationTime
        if (token == null || (expirationTime != null && expirationTime <= System.currentTimeMillis())) {
            return null
        }
        return token
    }

    private fun buildTournesolUid(url: String, serviceId: Int): String? {
        val prefix = getTournesolServicePrefix(serviceId) ?: return null
        return try {
            val service = NewPipe.getService(serviceId)
            val factory: LinkHandlerFactory = service.getStreamLHFactory()
            val id = factory.getId(url)
            "$prefix:$id"
        } catch (_: ExtractionException) {
            null
        }
    }

    private fun getTournesolServicePrefix(serviceId: Int): String? {
        return when (serviceId) {
            ServiceList.YouTube.serviceId -> "yt"
            ServiceList.SoundCloud.serviceId -> "sc"
            ServiceList.PeerTube.serviceId -> "peertube"
            else -> null
        }
    }

    private fun submitComparison(
        token: String,
        lastUid: String,
        currentUid: String,
        score: Int
    ) = io.reactivex.rxjava3.core.Single.fromCallable {
        val payload = buildComparisonPayload(lastUid, currentUid, score)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = payload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(COMPARE_URL)
            .post(body)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        val client = getHttpClient()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                return@fromCallable R.string.compare_submitted
            }
            if (response.code == 400 && responseBody.contains("already compared")) {
                return@fromCallable R.string.compare_already_submitted
            }
            throw IOException("HTTP ${response.code} $responseBody")
        }
    }.subscribeOn(Schedulers.io())

    private fun getHttpClient(): OkHttpClient {
        return DownloaderImpl.getInstance()?.getClient() ?: OkHttpClient.Builder().build()
    }

    private fun buildComparisonPayload(
        lastUid: String,
        currentUid: String,
        score: Int
    ): JSONObject {
        val payload = JSONObject()
        payload.put("pollName", COMPARE_POLL)
        payload.put("entity_a", JSONObject().put("uid", lastUid))
        payload.put("entity_b", JSONObject().put("uid", currentUid))

        val scoreItem = JSONObject()
        scoreItem.put("criteria", COMPARE_CRITERIA)
        scoreItem.put("score", score)
        scoreItem.put("score_max", SCORE_MAX)

        val criteriaScores = JSONArray()
        criteriaScores.put(scoreItem)
        payload.put("criteria_scores", criteriaScores)
        return payload
    }

    @Composable
    private fun CompareScreen(
        historyEntries: List<StreamHistoryEntry>,
        selectedIndex: Int,
        historyMessageRes: Int?,
        score: Int,
        submitted: Boolean,
        showLoginDialog: Boolean,
        loginInProgress: Boolean,
        loginError: String?,
        onSelectIndex: (Int) -> Unit,
        onScoreChange: (Int) -> Unit,
        onSubmit: () -> Unit,
        onDismissLogin: () -> Unit,
        onRegister: () -> Unit,
        onLogin: (String, String) -> Unit
    ) {
        val currentEntry = historyEntries.getOrNull(selectedIndex)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFE57373))) {
                        append("\u2191 Video A")
                    }
                    append(" should be more recommended than ")
                    withStyle(SpanStyle(color = Color(0xFF64B5F6))) {
                        append("Video B \u2193")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp)
            )

            HistoryWheel(
                entries = historyEntries,
                selectedIndex = selectedIndex,
                historyMessageRes = historyMessageRes,
                onSelectIndex = onSelectIndex
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Video A",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE57373)
                )
                Text(
                    text = "Video B",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF64B5F6)
                )
            }

            ThickScoreSlider(
                value = score,
                onValueChange = onScoreChange,
                modifier = Modifier.fillMaxWidth()
            )

            val submitEnabled = currentEntry != null && !submitted
            val submitLabel = stringResource(
                if (submitted) {
                    R.string.compare_submitted_label
                } else {
                    R.string.compare_submit_label
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable(enabled = submitEnabled) { onSubmit() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = submitLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (submitEnabled) {
                        lerp(MaterialTheme.colorScheme.onSurface, Color(0xFFFFD54F), 0.35f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(R.drawable.logo_small),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showLoginDialog) {
            TournesolLoginDialog(
                inProgress = loginInProgress,
                errorMessage = loginError,
                onDismiss = onDismissLogin,
                onRegister = onRegister,
                onLogin = onLogin
            )
        }
    }

    @Composable
    private fun TournesolLoginDialog(
        inProgress: Boolean,
        errorMessage: String?,
        onDismiss: () -> Unit,
        onRegister: () -> Unit,
        onLogin: (String, String) -> Unit
    ) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var localError by remember { mutableStateOf<String?>(null) }
        val statusText = when {
            localError != null -> localError
            inProgress -> stringResource(R.string.tournesol_login_in_progress)
            errorMessage != null -> errorMessage
            else -> null
        }
        val statusColor = if (localError != null || errorMessage != null) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        val missingFieldsText = stringResource(R.string.tournesol_login_missing_fields)

        Dialog(onDismissRequest = { if (!inProgress) onDismiss() }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tournesol_login_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.compare_login_required),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            if (localError != null) {
                                localError = null
                            }
                        },
                        label = { Text(stringResource(R.string.tournesol_username_hint)) },
                        singleLine = true,
                        enabled = !inProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (localError != null) {
                                localError = null
                            }
                        },
                        label = { Text(stringResource(R.string.tournesol_password_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !inProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (statusText != null) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onRegister,
                            enabled = !inProgress
                        ) {
                            Text(stringResource(R.string.tournesol_register_button))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !inProgress
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(
                                onClick = {
                                    if (username.isBlank() || password.isBlank()) {
                                        localError = missingFieldsText
                                    } else {
                                        localError = null
                                        onLogin(username.trim(), password.trim())
                                    }
                                },
                                enabled = !inProgress
                            ) {
                                if (inProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(R.string.tournesol_login_button))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun HistoryWheel(
        entries: List<StreamHistoryEntry>,
        selectedIndex: Int,
        historyMessageRes: Int?,
        onSelectIndex: (Int) -> Unit
    ) {
        val itemHeight = 64.dp
        if (entries.isEmpty()) {
            val message = historyMessageRes?.let { stringResource(it) }.orEmpty()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = itemHeight),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
            return
        }

        val density = LocalDensity.current
        val pagerState = rememberPagerState(
            initialPage = selectedIndex.coerceIn(0, entries.lastIndex)
        ) { entries.size }
        val coroutineScope = rememberCoroutineScope()
        val cameraDistance = with(density) { 22.dp.toPx() }
        val depthShift = with(density) { 18.dp.toPx() }
        val pageSizePx = with(density) { itemHeight.toPx() }
        val overlap = itemHeight * 0.75f
        val pageStepPx = with(density) { (itemHeight - overlap).toPx() }.coerceAtLeast(1f)

        LaunchedEffect(entries, selectedIndex) {
            val targetPage = selectedIndex.coerceIn(0, entries.lastIndex)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }

        LaunchedEffect(pagerState, entries) {
            snapshotFlow { pagerState.isScrollInProgress }
                .filter { !it }
                .collect {
                    val newIndex = pagerState.currentPage.coerceIn(0, entries.lastIndex)
                    if (newIndex != selectedIndex) {
                        onSelectIndex(newIndex)
                    }
                }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 2f)
                .pointerInput(pagerState, pageStepPx, entries.size) {
                    val velocityTracker = VelocityTracker()
                    var startPage = 0
                    var startPosition = 0f
                    var position = 0f
                    var minPage = 0
                    var maxPage = 0
                    var dragDistance = 0f
                    fun positionToPageOffset(currentPosition: Float): Pair<Int, Float> {
                        var clampedPosition = currentPosition.coerceIn(
                            minPage.toFloat(),
                            maxPage.toFloat()
                        )
                        var page = clampedPosition.toInt()
                        var offset = clampedPosition - page
                        if (offset > 0.5f && page < entries.lastIndex) {
                            page += 1
                            offset -= 1f
                        }
                        offset = offset.coerceIn(-0.5f, 0.5f)
                        return page to offset
                    }
                    detectDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            startPage = pagerState.currentPage
                            startPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                            position = startPosition
                            minPage = (startPage - 1).coerceAtLeast(0)
                            maxPage = (startPage + 1).coerceAtMost(entries.lastIndex)
                            dragDistance = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            dragDistance += dragAmount.y
                            val rawDelta = -dragDistance / pageStepPx
                            val absRawDelta = abs(rawDelta)
                            val visualDelta = if (absRawDelta <= 0.5f) {
                                rawDelta
                            } else {
                                val excess = absRawDelta - 0.5f
                                val damped = 0.5f + excess * 0.4f
                                if (rawDelta < 0f) -damped else damped
                            }
                            position = (startPosition + visualDelta).coerceIn(
                                minPage.toFloat(),
                                maxPage.toFloat()
                            )
                            val (targetPage, offset) = positionToPageOffset(position)
                            pagerState.requestScrollToPage(
                                targetPage,
                                offset
                            )
                        },
                        onDragCancel = {
                            val target = position.roundToInt()
                                .coerceIn(minPage, maxPage)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    target,
                                    animationSpec = tween(
                                        durationMillis = 320,
                                        easing = LinearOutSlowInEasing
                                    )
                                )
                            }
                        },
                        onDragEnd = {
                            val deltaFromStart = -dragDistance / pageStepPx
                            val velocityY = velocityTracker.calculateVelocity().y
                            val velocityThreshold = pageStepPx * 2.5f
                            val minFlingOffset = 0.08f
                            val swipeThreshold = 0.28f
                            val minTarget = minPage.coerceIn(0, entries.lastIndex)
                            val maxTarget = maxPage.coerceIn(0, entries.lastIndex)
                            val target = when {
                                deltaFromStart > swipeThreshold -> startPage + 1
                                deltaFromStart < -swipeThreshold -> startPage - 1
                                kotlin.math.abs(velocityY) > velocityThreshold &&
                                    kotlin.math.abs(deltaFromStart) > minFlingOffset ->
                                    if (velocityY < 0f) startPage + 1 else startPage - 1
                                else -> startPage
                            }.coerceIn(minTarget, maxTarget)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(
                                    target,
                                    animationSpec = tween(
                                        durationMillis = 320,
                                        easing = LinearOutSlowInEasing
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            val contentPadding = PaddingValues(vertical = itemHeight / 2)
            VerticalPager(
                state = pagerState,
                contentPadding = contentPadding,
                pageSize = PageSize.Fixed(itemHeight),
                pageSpacing = -overlap,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val layoutInfo = pagerState.layoutInfo
                val pageInfo = layoutInfo.visiblePagesInfo.firstOrNull { it.index == page }
                val rawOffset = if (pageInfo != null && pageStepPx > 0f) {
                    val pageCenter = pageInfo.offset + pageSizePx / 2f
                    val viewportCenter =
                        (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                    (pageCenter - viewportCenter) / pageStepPx
                } else {
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                }
                val rawOffsetAbs = abs(rawOffset)
                val pageOffset = rawOffsetAbs.coerceIn(0f, 1f)
                val focusSpan = 1.35f
                val visibilitySpan = 1.6f
                val focusOffset = (rawOffsetAbs / focusSpan).coerceIn(0f, 1f)
                val focus = 1f - focusOffset
                val visibility = ((visibilitySpan - rawOffsetAbs) / visibilitySpan)
                    .coerceIn(0f, 1f)
                val direction = when {
                    rawOffset > 0f -> 1f
                    rawOffset < 0f -> -1f
                    else -> 0f
                }
                val scale = lerp(1f, 0.88f, pageOffset)
                val alpha = lerp(1f, 0.35f, pageOffset)
                val rotationX = lerp(0f, 10f, pageOffset) * -direction
                val translation = depthShift * pageOffset * direction
                val elevation = (10f * (1f - pageOffset)).dp
                val contentAlpha = alpha * visibility
                val rawBackgroundAlpha = (1f - rawOffsetAbs).coerceIn(0f, 1f)
                val computedBackgroundAlpha = rawBackgroundAlpha * rawBackgroundAlpha
                val backgroundAlpha = if (pagerState.currentPage == page &&
                    !pagerState.isScrollInProgress
                ) {
                    1f
                } else {
                    computedBackgroundAlpha
                }
                HistoryCard(
                    entry = entries[page],
                    focus = focus,
                    contentAlpha = contentAlpha,
                    backgroundAlpha = backgroundAlpha,
                    elevation = elevation,
                    modifier = Modifier
                        .height(itemHeight)
                        .zIndex(focus)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            rotationX = rotationX,
                            translationY = translation,
                            cameraDistance = cameraDistance
                        )
                )
            }
        }
    }

    @Composable
    private fun HistoryCard(
        entry: StreamHistoryEntry,
        focus: Float,
        contentAlpha: Float,
        backgroundAlpha: Float,
        elevation: androidx.compose.ui.unit.Dp,
        modifier: Modifier = Modifier
    ) {
        val clampedFocus = focus.coerceIn(0f, 1f)
        val clampedBackgroundAlpha = backgroundAlpha.coerceIn(0f, 1f)
        val background = lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            clampedFocus
        ).copy(alpha = clampedBackgroundAlpha)
        val borderAlpha = (lerp(0.25f, 0.6f, clampedFocus) * clampedBackgroundAlpha)
            .coerceIn(0f, 1f)
        val borderColor = Color(0xFF64B5F6).copy(alpha = borderAlpha)
        Surface(
            modifier = modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = background,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = elevation,
            shadowElevation = elevation,
            border = BorderStroke(
                1.5.dp,
                borderColor
            )
        ) {
            Column(
                modifier = Modifier
                    .graphicsLayer(alpha = contentAlpha.coerceIn(0f, 1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompareVideoRow(entry)
            }
        }
    }

    @Composable
    private fun ThickScoreSlider(
        value: Int,
        onValueChange: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val displayValue = abs(value)
        val scoreDescription = stringResource(
            R.string.compare_score_accessibility,
            displayValue,
            0,
            SCORE_MAX
        )
        var sliderSize by remember { mutableStateOf(IntSize.Zero) }
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val leftAccent = Color(0xFFE57373)
        val rightAccent = Color(0xFF64B5F6)
        val accentColor = when {
            value < 0 -> leftAccent
            value > 0 -> rightAccent
            else -> Color(0xFF9E9E9E)
        }
        val fillColor = accentColor
        val indicatorColor = accentColor
        val labelFillColor = MaterialTheme.colorScheme.surface
        val labelStrokeColor = accentColor
        val labelShadowColor = accentColor.copy(alpha = 0.18f)
        val labelTextColor = accentColor
        val valueText = displayValue.toString()
        val deadZonePx = with(LocalDensity.current) { 10.dp.toPx() }

        fun updateFromPosition(x: Float, snapToCenter: Boolean) {
            val width = sliderSize.width.toFloat()
            if (width <= 0f) {
                return
            }
            val centerX = width / 2f
            val clamped = x.coerceIn(0f, width)
            val normalized = ((clamped - centerX) / centerX).coerceIn(-1f, 1f)
            var newValue = (normalized * SCORE_MAX)
                .roundToInt()
                .coerceIn(SCORE_MIN, SCORE_MAX)
            if (snapToCenter && abs(clamped - centerX) <= deadZonePx) {
                newValue = 0
            }
            if (newValue != value) {
                onValueChange(newValue)
            }
        }

        Canvas(
            modifier = modifier
                .height(72.dp)
                .padding(vertical = 6.dp)
                .onSizeChanged { sliderSize = it }
                .semantics { contentDescription = scoreDescription }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        updateFromPosition(offset.x, snapToCenter = true)
                    }
                }
                .pointerInput(Unit) {
                    var lastDragX = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastDragX = offset.x
                            updateFromPosition(offset.x, snapToCenter = false)
                        },
                        onDrag = { change, _ ->
                            lastDragX = change.position.x
                            updateFromPosition(change.position.x, snapToCenter = false)
                        },
                        onDragEnd = {
                            updateFromPosition(lastDragX, snapToCenter = true)
                        },
                        onDragCancel = {
                            updateFromPosition(lastDragX, snapToCenter = true)
                        }
                    )
                }
        ) {
            val trackHeight = size.height * 0.84f
            val trackTop = (size.height - trackHeight) / 2f
            val trackRadius = trackHeight * 0.08f
            drawRoundRect(
                color = trackColor,
                topLeft = androidx.compose.ui.geometry.Offset(0f, trackTop),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackRadius, trackRadius)
            )

            val centerX = size.width / 2f
            val signedProgress = when {
                value > 0 -> value.toFloat() / SCORE_MAX.toFloat()
                value < 0 -> value.toFloat() / -SCORE_MIN.toFloat()
                else -> 0f
            }.coerceIn(-1f, 1f)
            val fillWidth = kotlin.math.abs(signedProgress) * centerX
            if (fillWidth > 0f) {
                val fillStartX = if (signedProgress >= 0f) centerX else centerX - fillWidth
                drawRoundRect(
                    color = fillColor,
                    topLeft = androidx.compose.ui.geometry.Offset(fillStartX, trackTop),
                    size = Size(fillWidth, trackHeight),
                    cornerRadius = CornerRadius(trackRadius, trackRadius)
                )
            }

            val indicatorCenterX = centerX + signedProgress * centerX
            val indicatorHeight = trackHeight * 1.28f
            val indicatorWidth = trackHeight * 0.14f
            val indicatorTop = (size.height - indicatorHeight) / 2f
            drawRect(
                color = indicatorColor,
                topLeft = androidx.compose.ui.geometry.Offset(
                    indicatorCenterX - indicatorWidth / 2f,
                    indicatorTop
                ),
                size = Size(indicatorWidth, indicatorHeight)
            )

            val labelHeight = trackHeight * 0.46f
            val labelRadius = labelHeight / 2f
            val labelCenterY = size.height / 2f
            val labelTop = labelCenterY - labelHeight / 2f
            val labelPaddingX = labelHeight * 0.5f
            val labelTextSize = labelHeight * 0.6f
            val labelPaint = AndroidPaint().apply {
                isAntiAlias = true
                color = labelTextColor.toArgb()
                textAlign = AndroidPaint.Align.CENTER
                textSize = labelTextSize
                isFakeBoldText = true
            }
            val textWidth = labelPaint.measureText(valueText)
            val labelWidth = kotlin.math.max(labelHeight, textWidth + labelPaddingX * 2f)
            val labelLeft = indicatorCenterX - labelWidth / 2f
            drawRoundRect(
                color = labelShadowColor,
                topLeft = androidx.compose.ui.geometry.Offset(
                    labelLeft,
                    labelTop + labelHeight * 0.12f
                ),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelRadius, labelRadius)
            )
            drawRoundRect(
                color = labelFillColor,
                topLeft = androidx.compose.ui.geometry.Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelRadius, labelRadius)
            )
            drawRoundRect(
                color = labelStrokeColor,
                topLeft = androidx.compose.ui.geometry.Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelRadius, labelRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = labelHeight * 0.08f)
            )
            drawContext.canvas.nativeCanvas.apply {
                val metrics = labelPaint.fontMetrics
                val textY = labelCenterY - (metrics.ascent + metrics.descent) / 2f
                drawText(valueText, indicatorCenterX, textY, labelPaint)
            }
        }
    }

    @Composable
    private fun CompareVideoRow(entry: StreamHistoryEntry) {
        val stream = remember(entry) { entry.toStreamInfoItem() }
        val thumbnailDescription = stringResource(R.string.compare_thumbnail_description)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StreamThumbnail(
                stream = stream,
                showProgress = false,
                modifier = Modifier
                    .size(width = 88.dp, height = 48.dp)
                    .semantics {
                        contentDescription = thumbnailDescription
                    }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = stream.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stream.uploaderName.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    companion object {
        private const val COMPARE_URL = "https://api.tournesol.app/users/me/comparisons/videos"
        private const val COMPARE_POLL = "videos"
        private const val COMPARE_CRITERIA = "largely_recommended"
        private const val SCORE_MIN = -100
        private const val SCORE_MAX = 100
        private const val SCORE_RANGE = SCORE_MAX - SCORE_MIN
        private const val REGISTER_URL = "https://tournesol.app/signup"

        @JvmStatic
        fun getInstance(info: StreamInfo): CompareFragment {
            return CompareFragment().apply {
                arguments = bundleOf(KEY_INFO to info)
            }
        }
    }
}
