package org.schabi.newpipe.fragments.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.ktx.serializable
import org.schabi.newpipe.util.KEY_INFO
import org.schabi.newpipe.views.TournesolScoreCard

class TournesolFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tournesol, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val info = arguments?.serializable<StreamInfo>(KEY_INFO) ?: return
        if (childFragmentManager.findFragmentByTag(TAG_COMPARE) == null) {
            val compareFragment = CompareFragment.getInstance(info, useCompactUi = true, embedInDetail = true, externalScroll = true)
            childFragmentManager.beginTransaction()
                .replace(R.id.compare_container, compareFragment, TAG_COMPARE)
                .commitAllowingStateLoss()
        }
        val scoreCard = view.findViewById<TournesolScoreCard>(R.id.tournesol_score_card)
        scoreCard.bind(info, parentFragment as? VideoDetailFragment)
    }

    companion object {
        private const val TAG_COMPARE = "tournesol_compare"

        fun getInstance(info: StreamInfo): TournesolFragment {
            return TournesolFragment().apply {
                arguments = bundleOf(KEY_INFO to info)
            }
        }
    }
}
