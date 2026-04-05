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
            includeLowScoreVideos: Boolean,
            durationMinSeconds: Int,
            durationMaxSeconds: Int,
            weightLargelyRecommended: Int,
            weightReliability: Int,
            weightImportance: Int,
            weightPedagogy: Int,
            weightLaymanFriendly: Int,
            weightEntertainingRelaxing: Int,
            weightEngaging: Int,
            weightDiversityInclusion: Int,
            weightBetterHabits: Int,
            weightBackfireRisk: Int
        )
    }

    private var currentLanguages: MutableList<String> = ArrayList()
    private var currentDateKey: String = TournesolHelper.DEFAULT_TOURNESOL_FILTER_DATE_KEY
    private var currentIncludeLowScoreVideos: Boolean =
        TournesolHelper.DEFAULT_TOURNESOL_FILTER_INCLUDE_LOW_SCORE
    private var currentDurationMinSeconds: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_DURATION_MIN
    private var currentDurationMaxSeconds: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_DURATION_MAX
    private var currentWeightLargelyRecommended: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightReliability: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightImportance: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightPedagogy: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightLaymanFriendly: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightEntertainingRelaxing: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightEngaging: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightDiversityInclusion: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightBetterHabits: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
    private var currentWeightBackfireRisk: Int = TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
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
        val previousDurationMin = currentDurationMinSeconds
        val previousDurationMax = currentDurationMaxSeconds
        val previousWeightLR = currentWeightLargelyRecommended
        val previousWeightRel = currentWeightReliability
        val previousWeightImp = currentWeightImportance
        val previousWeightPed = currentWeightPedagogy
        val previousWeightLay = currentWeightLaymanFriendly
        val previousWeightEnt = currentWeightEntertainingRelaxing
        val previousWeightEng = currentWeightEngaging
        val previousWeightDiv = currentWeightDiversityInclusion
        val previousWeightBet = currentWeightBetterHabits
        val previousWeightBack = currentWeightBackfireRisk
        loadFilters()
        if (previousLanguages != currentLanguages ||
            previousDateKey != currentDateKey ||
            previousIncludeLowScoreVideos != currentIncludeLowScoreVideos ||
            previousDurationMin != currentDurationMinSeconds ||
            previousDurationMax != currentDurationMaxSeconds ||
            previousWeightLR != currentWeightLargelyRecommended ||
            previousWeightRel != currentWeightReliability ||
            previousWeightImp != currentWeightImportance ||
            previousWeightPed != currentWeightPedagogy ||
            previousWeightLay != currentWeightLaymanFriendly ||
            previousWeightEnt != currentWeightEntertainingRelaxing ||
            previousWeightEng != currentWeightEngaging ||
            previousWeightDiv != currentWeightDiversityInclusion ||
            previousWeightBet != currentWeightBetterHabits ||
            previousWeightBack != currentWeightBackfireRisk
        ) {
            updateQuickChipSelection()
            notifyFiltersChanged()
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

    fun getCurrentDurationMinSeconds(): Int = currentDurationMinSeconds
    fun getCurrentDurationMaxSeconds(): Int = currentDurationMaxSeconds
    fun getCurrentWeightLargelyRecommended(): Int = currentWeightLargelyRecommended
    fun getCurrentWeightReliability(): Int = currentWeightReliability
    fun getCurrentWeightImportance(): Int = currentWeightImportance
    fun getCurrentWeightPedagogy(): Int = currentWeightPedagogy
    fun getCurrentWeightLaymanFriendly(): Int = currentWeightLaymanFriendly
    fun getCurrentWeightEntertainingRelaxing(): Int = currentWeightEntertainingRelaxing
    fun getCurrentWeightEngaging(): Int = currentWeightEngaging
    fun getCurrentWeightDiversityInclusion(): Int = currentWeightDiversityInclusion
    fun getCurrentWeightBetterHabits(): Int = currentWeightBetterHabits
    fun getCurrentWeightBackfireRisk(): Int = currentWeightBackfireRisk

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

    private fun notifyFiltersChanged() {
        listener.onFiltersChanged(
            ArrayList(currentLanguages),
            currentDateKey,
            currentIncludeLowScoreVideos,
            currentDurationMinSeconds,
            currentDurationMaxSeconds,
            currentWeightLargelyRecommended,
            currentWeightReliability,
            currentWeightImportance,
            currentWeightPedagogy,
            currentWeightLaymanFriendly,
            currentWeightEntertainingRelaxing,
            currentWeightEngaging,
            currentWeightDiversityInclusion,
            currentWeightBetterHabits,
            currentWeightBackfireRisk
        )
    }

    private fun onUnsafeToggled() {
        currentIncludeLowScoreVideos = !currentIncludeLowScoreVideos
        saveFilters()
        updateQuickChipSelection()
        notifyFiltersChanged()
    }

    private fun onQuickDateSelected(dateKey: String) {
        if (currentDateKey == dateKey) return
        currentDateKey = dateKey
        saveFilters()
        updateQuickChipSelection()
        notifyFiltersChanged()
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

        // Highlight Advanced chip if a non-quick filter is active
        val hasAdvancedFilters = !isQuickFilter ||
            currentDurationMinSeconds >= 0 || currentDurationMaxSeconds >= 0 ||
            currentWeightLargelyRecommended >= 0 || currentWeightReliability >= 0 ||
            currentWeightImportance >= 0 || currentWeightPedagogy >= 0 ||
            currentWeightLaymanFriendly >= 0 || currentWeightEntertainingRelaxing >= 0 ||
            currentWeightEngaging >= 0 || currentWeightDiversityInclusion >= 0 ||
            currentWeightBetterHabits >= 0 || currentWeightBackfireRisk >= 0
        val advancedChip = headerView.findViewById<Chip>(R.id.chip_advanced) ?: return
        if (hasAdvancedFilters) {
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
            currentIncludeLowScoreVideos,
            currentDurationMinSeconds,
            currentDurationMaxSeconds,
            currentWeightLargelyRecommended,
            currentWeightReliability,
            currentWeightImportance,
            currentWeightPedagogy,
            currentWeightLaymanFriendly,
            currentWeightEntertainingRelaxing,
            currentWeightEngaging,
            currentWeightDiversityInclusion,
            currentWeightBetterHabits,
            currentWeightBackfireRisk
        )
        filterFragment.setListener(object : TournesolFilterFragment.FilterListener {
            override fun onApply(
                languages: List<String>,
                dateKey: String,
                includeLowScoreVideos: Boolean,
                durationMinSeconds: Int,
                durationMaxSeconds: Int,
                weightLargelyRecommended: Int,
                weightReliability: Int,
                weightImportance: Int,
                weightPedagogy: Int,
                weightLaymanFriendly: Int,
                weightEntertainingRelaxing: Int,
                weightEngaging: Int,
                weightDiversityInclusion: Int,
                weightBetterHabits: Int,
                weightBackfireRisk: Int
            ) {
                currentLanguages = ArrayList(languages)
                currentDateKey = dateKey
                currentIncludeLowScoreVideos = includeLowScoreVideos
                currentDurationMinSeconds = durationMinSeconds
                currentDurationMaxSeconds = durationMaxSeconds
                currentWeightLargelyRecommended = weightLargelyRecommended
                currentWeightReliability = weightReliability
                currentWeightImportance = weightImportance
                currentWeightPedagogy = weightPedagogy
                currentWeightLaymanFriendly = weightLaymanFriendly
                currentWeightEntertainingRelaxing = weightEntertainingRelaxing
                currentWeightEngaging = weightEngaging
                currentWeightDiversityInclusion = weightDiversityInclusion
                currentWeightBetterHabits = weightBetterHabits
                currentWeightBackfireRisk = weightBackfireRisk

                saveFilters()
                updateQuickChipSelection()
                notifyFiltersChanged()
            }
        })
        filterFragment.show(fragment.parentFragmentManager, "TournesolFilters")
    }

    private fun loadFilters() {
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(fragment.requireContext())
        val langs = prefs.getString(
            TournesolHelper.PREF_TOURNESOL_FILTER_LANGUAGES,
            TournesolHelper.getDefaultFilterLanguages()
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
        currentDurationMinSeconds = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_DURATION_MIN,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_DURATION_MIN
        )
        currentDurationMaxSeconds = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_DURATION_MAX,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_DURATION_MAX
        )
        currentWeightLargelyRecommended = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_LARGELY_RECOMMENDED,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightReliability = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_RELIABILITY,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightImportance = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_IMPORTANCE,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightPedagogy = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_PEDAGOGY,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightLaymanFriendly = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_LAYMAN_FRIENDLY,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightEntertainingRelaxing = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_ENTERTAINING_RELAXING,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightEngaging = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_ENGAGING,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightDiversityInclusion = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_DIVERSITY_INCLUSION,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightBetterHabits = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_BETTER_HABITS,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
        )
        currentWeightBackfireRisk = prefs.getInt(
            TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_BACKFIRE_RISK,
            TournesolHelper.DEFAULT_TOURNESOL_FILTER_WEIGHT
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
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_DURATION_MIN,
                currentDurationMinSeconds
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_DURATION_MAX,
                currentDurationMaxSeconds
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_LARGELY_RECOMMENDED,
                currentWeightLargelyRecommended
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_RELIABILITY,
                currentWeightReliability
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_IMPORTANCE,
                currentWeightImportance
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_PEDAGOGY,
                currentWeightPedagogy
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_LAYMAN_FRIENDLY,
                currentWeightLaymanFriendly
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_ENTERTAINING_RELAXING,
                currentWeightEntertainingRelaxing
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_ENGAGING,
                currentWeightEngaging
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_DIVERSITY_INCLUSION,
                currentWeightDiversityInclusion
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_BETTER_HABITS,
                currentWeightBetterHabits
            )
            .putInt(
                TournesolHelper.PREF_TOURNESOL_FILTER_WEIGHT_BACKFIRE_RISK,
                currentWeightBackfireRisk
            )
            .apply()
    }
}
