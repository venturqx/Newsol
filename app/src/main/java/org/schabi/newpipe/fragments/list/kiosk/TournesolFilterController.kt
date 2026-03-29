package org.schabi.newpipe.fragments.list.kiosk

import android.content.SharedPreferences
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.chip.Chip
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

    private val quickDateChips = linkedMapOf(
        "day" to R.id.chip_day,
        "week" to R.id.chip_week,
        "month" to R.id.chip_month,
        "3_months" to R.id.chip_3_months,
        "year" to R.id.chip_year
    )

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
            updateQuickChipSelection()
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
            ?: return

        if (tournesolHeaderView == null) {
            val contextThemeWrapper = ContextThemeWrapper(
                fragment.requireContext(),
                com.google.android.material.R.style.Theme_MaterialComponents_DayNight
            )

            tournesolHeaderView = LayoutInflater.from(contextThemeWrapper)
                .inflate(R.layout.tournesol_header, headerContainer, false)

            // Wire up quick date chips
            for ((dateKey, chipId) in quickDateChips) {
                tournesolHeaderView?.findViewById<Chip>(chipId)?.setOnClickListener {
                    onQuickDateSelected(dateKey)
                }
            }

            // Wire up Unsafe toggle
            tournesolHeaderView?.findViewById<Chip>(R.id.chip_unsafe)?.setOnClickListener {
                onUnsafeToggled()
            }

            // Wire up Advanced button
            tournesolHeaderView?.findViewById<View>(R.id.chip_advanced)?.setOnClickListener {
                openFilterSheet()
            }

            updateQuickChipSelection()
        }

        if (tournesolHeaderView?.parent == null) {
            headerContainer.addView(tournesolHeaderView)
        }
        headerContainer.visibility = View.VISIBLE
    }

    private fun onUnsafeToggled() {
        currentIncludeLowScoreVideos = !currentIncludeLowScoreVideos
        saveFilters()
        updateQuickChipSelection()
        listener.onFiltersChanged(
            ArrayList(currentLanguages),
            currentDateKey,
            currentIncludeLowScoreVideos
        )
    }

    private fun onQuickDateSelected(dateKey: String) {
        if (currentDateKey == dateKey) return
        currentDateKey = dateKey
        saveFilters()
        updateQuickChipSelection()
        listener.onFiltersChanged(
            ArrayList(currentLanguages),
            currentDateKey,
            currentIncludeLowScoreVideos
        )
    }

    private fun updateQuickChipSelection() {
        val headerView = tournesolHeaderView ?: return
        val isQuickFilter = quickDateChips.containsKey(currentDateKey)

        for ((dateKey, chipId) in quickDateChips) {
            val chip = headerView.findViewById<Chip>(chipId) ?: continue
            chip.isChecked = dateKey == currentDateKey
        }

        // Sync Unsafe chip
        headerView.findViewById<Chip>(R.id.chip_unsafe)?.isChecked = currentIncludeLowScoreVideos

        // Highlight Advanced chip if a non-quick filter is active (e.g. "forever")
        val advancedChip = headerView.findViewById<Chip>(R.id.chip_advanced) ?: return
        if (!isQuickFilter) {
            advancedChip.isCheckable = true
            advancedChip.isChecked = true
        } else {
            advancedChip.isChecked = false
            advancedChip.isCheckable = false
        }
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
                updateQuickChipSelection()
                listener.onFiltersChanged(
                    ArrayList(currentLanguages),
                    currentDateKey,
                    currentIncludeLowScoreVideos
                )
            }
        })
        filterFragment.show(fragment.parentFragmentManager, "TournesolFilters")
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
}
