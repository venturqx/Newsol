package org.schabi.newpipe.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedHashSet
import java.util.Locale
import java.util.TimeZone

data class TournesolCacheEntry(val score: Long, val unsafeReasons: List<String>)

object TournesolScoreCache {
    private val cache = LinkedHashMap<String, TournesolCacheEntry>(32, 0.75f, true)

    fun put(url: String, score: Long, unsafeReasons: List<String> = emptyList()) {
        synchronized(cache) {
            cache[url] = TournesolCacheEntry(score, unsafeReasons)
            // Keep cache bounded
            if (cache.size > 200) {
                cache.remove(cache.keys.first())
            }
        }
    }

    fun get(url: String): Long? {
        synchronized(cache) {
            return cache[url]?.score
        }
    }

    fun getEntry(url: String): TournesolCacheEntry? {
        synchronized(cache) {
            return cache[url]
        }
    }
}

object TournesolHelper {
    const val KIOSK_ID = "Tournesol"
    const val PREF_TOURNESOL_FILTER_LANGUAGES = "tournesol_filter_languages"
    const val PREF_TOURNESOL_FILTER_DATE_KEY = "tournesol_filter_date_key"
    const val PREF_TOURNESOL_FILTER_INCLUDE_LOW_SCORE = "tournesol_filter_include_low_score"
    const val PREF_TOURNESOL_FILTER_DURATION_MIN = "tournesol_filter_duration_min"
    const val PREF_TOURNESOL_FILTER_DURATION_MAX = "tournesol_filter_duration_max"
    const val PREF_TOURNESOL_FILTER_WEIGHT_LARGELY_RECOMMENDED = "tournesol_filter_weight_largely_recommended"
    const val PREF_TOURNESOL_FILTER_WEIGHT_RELIABILITY = "tournesol_filter_weight_reliability"
    const val PREF_TOURNESOL_FILTER_WEIGHT_IMPORTANCE = "tournesol_filter_weight_importance"
    const val PREF_TOURNESOL_FILTER_WEIGHT_PEDAGOGY = "tournesol_filter_weight_pedagogy"
    const val PREF_TOURNESOL_FILTER_WEIGHT_LAYMAN_FRIENDLY = "tournesol_filter_weight_layman_friendly"
    const val PREF_TOURNESOL_FILTER_WEIGHT_ENTERTAINING_RELAXING = "tournesol_filter_weight_entertaining_relaxing"
    const val PREF_TOURNESOL_FILTER_WEIGHT_ENGAGING = "tournesol_filter_weight_engaging"
    const val PREF_TOURNESOL_FILTER_WEIGHT_DIVERSITY_INCLUSION = "tournesol_filter_weight_diversity_inclusion"
    const val PREF_TOURNESOL_FILTER_WEIGHT_BETTER_HABITS = "tournesol_filter_weight_better_habits"
    const val PREF_TOURNESOL_FILTER_WEIGHT_BACKFIRE_RISK = "tournesol_filter_weight_backfire_risk"

    /**
     * Returns the default filter languages based on system locale:
     * - French or English system → that language only
     * - Any other language → English + that language
     */
    fun getDefaultFilterLanguages(): String {
        val systemLang = Locale.getDefault().language
        return when (systemLang) {
            "fr", "en" -> systemLang
            else -> "en,$systemLang"
        }
    }
    const val DEFAULT_TOURNESOL_FILTER_DATE_KEY = "3_months"
    const val DEFAULT_TOURNESOL_FILTER_INCLUDE_LOW_SCORE = false
    const val DEFAULT_TOURNESOL_FILTER_DURATION_MIN = -1
    const val DEFAULT_TOURNESOL_FILTER_DURATION_MAX = -1
    const val DEFAULT_TOURNESOL_FILTER_WEIGHT = -1
    private const val REASON_INSUFFICIENT_TOURNESOL_SCORE = "insufficient_tournesol_score"
    private const val REASON_INSUFFICIENT_TRUST = "insufficient_trust"
    private const val REASON_MODERATION_BY_ASSOCIATION = "moderation_by_association"
    private const val REASON_MODERATION_BY_CONTRIBUTORS = "moderation_by_contributors"

    fun buildTournesolUrl(
        languages: List<String>?,
        dateKey: String,
        includeLowScoreVideos: Boolean,
        durationMinSeconds: Int = DEFAULT_TOURNESOL_FILTER_DURATION_MIN,
        durationMaxSeconds: Int = DEFAULT_TOURNESOL_FILTER_DURATION_MAX,
        weightLargelyRecommended: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightReliability: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightImportance: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightPedagogy: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightLaymanFriendly: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightEntertainingRelaxing: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightEngaging: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightDiversityInclusion: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightBetterHabits: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT,
        weightBackfireRisk: Int = DEFAULT_TOURNESOL_FILTER_WEIGHT
    ): String {
        val sb = StringBuilder(KIOSK_ID)

        sb.append("?languages=")
        if (!languages.isNullOrEmpty()) {
            for (i in languages.indices) {
                sb.append(languages[i])
                if (i < languages.size - 1) {
                    sb.append(",")
                }
            }
        }

        val dateGte = calculateDateGte(dateKey)
        if (dateGte != null) {
            sb.append("&date_gte=").append(dateGte)
        }
        if (includeLowScoreVideos) {
            sb.append("&unsafe=true")
        }
        if (durationMinSeconds >= 0) {
            sb.append("&duration_gte=").append(durationMinSeconds)
        }
        if (durationMaxSeconds >= 0) {
            sb.append("&duration_lte=").append(durationMaxSeconds)
        }
        if (weightLargelyRecommended >= 0) {
            sb.append("&weight_largely_recommended=").append(weightLargelyRecommended)
        }
        if (weightReliability >= 0) {
            sb.append("&weight_reliability=").append(weightReliability)
        }
        if (weightImportance >= 0) {
            sb.append("&weight_importance=").append(weightImportance)
        }
        if (weightPedagogy >= 0) {
            sb.append("&weight_pedagogy=").append(weightPedagogy)
        }
        if (weightLaymanFriendly >= 0) {
            sb.append("&weight_layman_friendly=").append(weightLaymanFriendly)
        }
        if (weightEntertainingRelaxing >= 0) {
            sb.append("&weight_entertaining_relaxing=").append(weightEntertainingRelaxing)
        }
        if (weightEngaging >= 0) {
            sb.append("&weight_engaging=").append(weightEngaging)
        }
        if (weightDiversityInclusion >= 0) {
            sb.append("&weight_diversity_inclusion=").append(weightDiversityInclusion)
        }
        if (weightBetterHabits >= 0) {
            sb.append("&weight_better_habits=").append(weightBetterHabits)
        }
        if (weightBackfireRisk >= 0) {
            sb.append("&weight_backfire_risk=").append(weightBackfireRisk)
        }

        return sb.toString()
    }

    fun buildTournesolUrl(languages: List<String>?, dateKey: String): String = buildTournesolUrl(languages, dateKey, false)

    @JvmStatic
    fun formatUnsafeReasonCodes(reasons: List<String>?): String {
        if (reasons.isNullOrEmpty()) {
            return ""
        }

        val reasonLabels = LinkedHashSet<String>()
        for (reason in reasons) {
            val reasonLabel = when (reason) {
                REASON_INSUFFICIENT_TOURNESOL_SCORE,
                REASON_INSUFFICIENT_TRUST -> "Insufficient"

                REASON_MODERATION_BY_ASSOCIATION,
                REASON_MODERATION_BY_CONTRIBUTORS -> "Moderated"

                else -> reason
            }
            if (reasonLabel.isNotBlank()) {
                reasonLabels.add(reasonLabel)
            }
        }

        if (reasonLabels.isEmpty()) {
            return ""
        }

        return "(" + reasonLabels.joinToString("/") + ")"
    }

    @JvmStatic
    fun hasInsufficientReason(reasons: List<String>?): Boolean {
        return reasons?.any {
            it == REASON_INSUFFICIENT_TOURNESOL_SCORE || it == REASON_INSUFFICIENT_TRUST
        } == true
    }

    fun calculateDateGte(dateKey: String): String? {
        if (dateKey == "forever") {
            return null
        }

        val cal = Calendar.getInstance()
        when (dateKey) {
            "year" -> cal.add(Calendar.YEAR, -1)
            "3_months" -> cal.add(Calendar.MONTH, -3)
            "month" -> cal.add(Calendar.MONTH, -1)
            "week" -> cal.add(Calendar.WEEK_OF_YEAR, -1)
            "day" -> cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(cal.time)
    }
}
