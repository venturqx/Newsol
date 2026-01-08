package org.schabi.newpipe.fragments.detail

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
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
import org.schabi.newpipe.util.KEY_INFO
import org.schabi.newpipe.util.TournesolAuthManager
import org.schabi.newpipe.util.TournesolLoginDialog
import java.io.IOException

class CompareFragment : Fragment() {
    private var currentInfo: StreamInfo? = null
    private val disposables = CompositeDisposable()
    private var lastViewedEntry: StreamHistoryEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInfo = arguments?.serializable<StreamInfo>(KEY_INFO)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scrollView = NestedScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            val padding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16f,
                resources.displayMetrics
            ).toInt()
            setPadding(padding, padding, padding, padding)
        }
        scrollView.addView(
            layout,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val textView = TextView(requireContext()).apply {
            text = getString(R.string.compare_title)
        }
        layout.addView(textView)

        val currentHeader = TextView(requireContext()).apply {
            text = getString(R.string.compare_current_video_header)
        }
        layout.addView(currentHeader)

        val currentContent = TextView(requireContext())
        updateCurrent(currentContent)
        layout.addView(currentContent)

        val lastViewedHeader = TextView(requireContext()).apply {
            text = getString(R.string.compare_last_viewed_header)
        }
        layout.addView(lastViewedHeader)

        val lastViewedContent = TextView(requireContext()).apply {
            text = getString(R.string.compare_loading_history)
        }
        layout.addView(lastViewedContent)

        val historyRecordManager = HistoryRecordManager(requireContext())
        val historySource = historyRecordManager.getLatestStreamHistoryEntry()
        disposables.add(
            historySource
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { entry -> updateLastViewed(lastViewedContent, entry) },
                    { lastViewedContent.text = getString(R.string.compare_history_unavailable) },
                    { lastViewedContent.text = getString(R.string.compare_history_empty) }
                )
        )

        val scoreLabel = TextView(requireContext()).apply {
            text = getString(R.string.compare_score_label, SCORE_OFFSET + SCORE_MIN)
        }
        layout.addView(scoreLabel)

        val scoreSeekBar = SeekBar(requireContext()).apply {
            max = SCORE_RANGE
            progress = SCORE_OFFSET
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    scoreLabel.text = getString(
                        R.string.compare_score_label,
                        progress + SCORE_MIN
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // No-op.
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    // No-op.
                }
            })
        }
        layout.addView(
            scoreSeekBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val compareButton = Button(requireContext()).apply {
            setText(R.string.compare_action_button)
            setOnClickListener {
                val score = scoreSeekBar.progress + SCORE_MIN
                sendComparison(score)
            }
        }
        layout.addView(compareButton)

        val loginButton = Button(requireContext()).apply {
            setText(R.string.tournesol_login_action)
            setOnClickListener { TournesolLoginDialog(requireContext()).show() }
        }
        layout.addView(loginButton)

        return scrollView
    }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    private fun updateLastViewed(target: TextView, entry: StreamHistoryEntry) {
        lastViewedEntry = entry
        val title = entry.streamEntity.title
        val url = entry.streamEntity.url
        target.text = getString(R.string.compare_item_info, title, url)
    }

    private fun updateCurrent(target: TextView) {
        val info = currentInfo
        if (info == null) {
            target.text = getString(R.string.compare_current_unavailable)
            return
        }

        target.text = getString(R.string.compare_item_info, info.name, info.url)
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
        val lastEntry = lastViewedEntry
        if (lastEntry == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.compare_history_unavailable),
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
            return
        }

        val lastUid = buildTournesolUid(
            lastEntry.streamEntity.url,
            lastEntry.streamEntity.serviceId
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

    companion object {
        private const val COMPARE_URL = "https://api.tournesol.app/users/me/comparisons/videos"
        private const val COMPARE_POLL = "videos"
        private const val COMPARE_CRITERIA = "largely_recommended"
        private const val SCORE_MIN = -100
        private const val SCORE_MAX = 100
        private const val SCORE_RANGE = SCORE_MAX - SCORE_MIN
        private const val SCORE_OFFSET = -SCORE_MIN

        @JvmStatic
        fun getInstance(info: StreamInfo): CompareFragment {
            return CompareFragment().apply {
                arguments = bundleOf(KEY_INFO to info)
            }
        }
    }
}
