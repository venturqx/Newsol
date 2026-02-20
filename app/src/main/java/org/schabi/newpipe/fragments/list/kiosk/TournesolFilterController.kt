package org.schabi.newpipe.fragments.list.kiosk

import android.content.SharedPreferences
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import org.schabi.newpipe.R
import org.schabi.newpipe.util.TournesolHelper

class TournesolFilterController(
    private val fragment: Fragment,
    private val listener: Listener
) {
    interface Listener {
        fun onFiltersChanged(
            languages: List<String>,
            dateKey: String,
            includeLowScoreVideos: Boolean
        )
    }

    private var currentLanguages: MutableList<String> = ArrayList()
    private var currentDateKey: String = TournesolHelper.DEFAULT_TOURNESOL_FILTER_DATE_KEY
    private var currentIncludeLowScoreVideos: Boolean =
        TournesolHelper.DEFAULT_TOURNESOL_FILTER_INCLUDE_LOW_SCORE
    private var tournesolHeaderView: View? = null

    fun init(rootView: View) {
        loadFilters()
        setupTournesolHeader(rootView)
    }

    fun onResume() {
        val previousLanguages = ArrayList(currentLanguages)
        val previousDateKey = currentDateKey
        val previousIncludeLowScoreVideos = currentIncludeLowScoreVideos
        loadFilters()
        if (previousLanguages != currentLanguages ||
            previousDateKey != currentDateKey ||
            previousIncludeLowScoreVideos != currentIncludeLowScoreVideos
        ) {
            updateTournesolHeaderSummary()
            listener.onFiltersChanged(
                ArrayList(currentLanguages),
                currentDateKey,
                currentIncludeLowScoreVideos
            )
        }
    }

    fun onDestroyView() {
        tournesolHeaderView = null
    }

    fun getCurrentLanguages(): List<String> {
        return ArrayList(currentLanguages)
    }

    fun getCurrentDateKey(): String {
        return currentDateKey
    }

    fun getCurrentIncludeLowScoreVideos(): Boolean {
        return currentIncludeLowScoreVideos
    }

    private fun setupTournesolHeader(rootView: View) {
        val headerContainer = rootView.findViewById<ViewGroup>(R.id.kiosk_header_container)
        if (headerContainer == null) {
            return
        }

        if (tournesolHeaderView == null) {
            val contextThemeWrapper = ContextThemeWrapper(
                fragment.requireContext(),
                com.google.android.material.R.style.Theme_MaterialComponents_DayNight
            )

            tournesolHeaderView = LayoutInflater.from(contextThemeWrapper)
                .inflate(R.layout.tournesol_header, headerContainer, false)

            val btnOpenFilters = tournesolHeaderView?.findViewById<View>(R.id.btn_open_filters)
            btnOpenFilters?.setOnClickListener { openFilterSheet() }

            updateTournesolHeaderSummary()
        }

        if (tournesolHeaderView?.parent == null) {
            headerContainer.addView(tournesolHeaderView)
        }
        headerContainer.visibility = View.VISIBLE
    }

    private fun openFilterSheet() {
        val filterFragment = TournesolFilterFragment()
        filterFragment.setInitialData(
            currentLanguages,
            currentDateKey,
            currentIncludeLowScoreVideos
        )
        filterFragment.setListener(object : TournesolFilterFragment.FilterListener {
            override fun onApply(
                languages: List<String>,
                dateKey: String,
                includeLowScoreVideos: Boolean
            ) {
                currentLanguages = ArrayList(languages)
                currentDateKey = dateKey
                currentIncludeLowScoreVideos = includeLowScoreVideos

                saveFilters()
                updateTournesolHeaderSummary()
                listener.onFiltersChanged(
                    ArrayList(currentLanguages),
                    currentDateKey,
                    currentIncludeLowScoreVideos
                )
            }
        })
        filterFragment.show(fragment.parentFragmentManager, "TournesolFilters")
    }

    private fun updateTournesolHeaderSummary() {
        val headerView = tournesolHeaderView ?: return
        val textActiveFilters = headerView.findViewById<TextView>(R.id.text_active_filters)
            ?: return

        val sb = StringBuilder()

        if (currentLanguages.isNotEmpty()) {
            val langNames = ArrayList<String>()
            for (code in currentLanguages) {
                when (code) {
                    "en" -> langNames.add(fragment.getString(R.string.language_english))
                    "fr" -> langNames.add(fragment.getString(R.string.language_french))
                    "es" -> langNames.add(fragment.getString(R.string.language_spanish))
                    "de" -> langNames.add(fragment.getString(R.string.language_german))
                    "it" -> langNames.add(fragment.getString(R.string.language_italian))
                    "pt" -> langNames.add(fragment.getString(R.string.language_portuguese))
                }
            }
            for (i in langNames.indices) {
                sb.append(langNames[i])
                if (i < langNames.size - 1) {
                    sb.append(", ")
                }
            }
        } else {
            sb.append(fragment.getString(R.string.all))
        }

        sb.append(FILTER_SEPARATOR)

        val dateString = when (currentDateKey) {
            "forever" -> fragment.getString(R.string.date_since_forever)
            "year" -> fragment.getString(R.string.date_last_year)
            "3_months" -> fragment.getString(R.string.date_last_3_months)
            "month" -> fragment.getString(R.string.date_last_month)
            "week" -> fragment.getString(R.string.date_last_week)
            else -> ""
        }
        sb.append(dateString)

        if (currentIncludeLowScoreVideos) {
            sb.append(FILTER_SEPARATOR)
            sb.append(fragment.getString(R.string.include_low_score_videos_short))
        }

        textActiveFilters.text = sb.toString()
    }

    private fun loadFilters() {
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(fragment.requireContext())
        val langs = prefs.getString(
            TournesolHelper.PREF_TOURNESOL_FILTER_LANGUAGES,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_LANGUAGES
        )
        currentLanguages = if (!langs.isNullOrEmpty()) {
            ArrayList(langs.split(","))
        } else {
            ArrayList()
        }
        currentDateKey = prefs.getString(
            TournesolHelper.PREF_TOURNESOL_FILTER_DATE_KEY,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_DATE_KEY
        ) ?: TournesolHelper.DEFAULT_TOURNESOL_FILTER_DATE_KEY
        currentIncludeLowScoreVideos = prefs.getBoolean(
            TournesolHelper.PREF_TOURNESOL_FILTER_INCLUDE_LOW_SCORE,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_INCLUDE_LOW_SCORE
        )
    }

    private fun saveFilters() {
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(fragment.requireContext())
        val languages = currentLanguages.joinToString(",")
        prefs.edit()
            .putString(TournesolHelper.PREF_TOURNESOL_FILTER_LANGUAGES, languages)
            .putString(TournesolHelper.PREF_TOURNESOL_FILTER_DATE_KEY, currentDateKey)
            .putBoolean(
                TournesolHelper.PREF_TOURNESOL_FILTER_INCLUDE_LOW_SCORE,
                currentIncludeLowScoreVideos
            )
            .apply()
    }

    companion object {
        private const val FILTER_SEPARATOR = " \u0007 "
    }
}
