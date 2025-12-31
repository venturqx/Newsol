package org.schabi.newpipe.fragments.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.schabi.newpipe.database.history.model.StreamHistoryEntry
import org.schabi.newpipe.local.history.HistoryRecordManager
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.ktx.serializable
import org.schabi.newpipe.util.KEY_INFO

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
        }
        scrollView.addView(
            layout,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val textView = TextView(requireContext()).apply {
            text = "Compare"
        }
        layout.addView(textView)

        val currentHeader = TextView(requireContext()).apply {
            text = "Current video"
        }
        layout.addView(currentHeader)

        val currentContent = TextView(requireContext())
        updateCurrent(currentContent)
        layout.addView(currentContent)

        val lastViewedHeader = TextView(requireContext()).apply {
            text = "Last viewed video"
        }
        layout.addView(lastViewedHeader)

        val lastViewedContent = TextView(requireContext()).apply {
            text = "Loading history..."
        }
        layout.addView(lastViewedContent)

        val historyRecordManager = HistoryRecordManager(requireContext())
        disposables.add(
            historyRecordManager.getLatestStreamHistoryEntry()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { entry -> updateLastViewed(lastViewedContent, entry) },
                    { lastViewedContent.text = "History unavailable" },
                    { lastViewedContent.text = "History empty" }
                )
        )

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
        target.text = "Title: $title\nURL: $url"
    }

    private fun updateCurrent(target: TextView) {
        val info = currentInfo
        if (info == null) {
            target.text = "Current video unavailable"
            return
        }

        target.text = "Title: ${info.name}\nURL: ${info.url}"
    }

    companion object {
        @JvmStatic
        fun getInstance(info: StreamInfo): CompareFragment {
            return CompareFragment().apply {
                arguments = bundleOf(KEY_INFO to info)
            }
        }
    }
}
