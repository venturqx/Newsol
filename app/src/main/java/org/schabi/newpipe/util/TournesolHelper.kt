package org.schabi.newpipe.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedHashSet
import java.util.Locale
import java.util.TimeZone

object TournesolHelper {
    const val KIOSK_ID = "Tournesol"
    const val PREF_TOURNESOL_FILTER_LANGUAGES = "tournesol_filter_languages"
    const val PREF_TOURNESOL_FILTER_DATE_KEY = "tournesol_filter_date_key"
    const val PREF_TOURNESOL_FILTER_INCLUDE_LOW_SCORE = "tournesol_filter_include_low_score"

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
    private const val REASON_INSUFFICIENT_TOURNESOL_SCORE = "insufficient_tournesol_score"
    private const val REASON_INSUFFICIENT_TRUST = "insufficient_trust"
    private const val REASON_MODERATION_BY_ASSOCIATION = "moderation_by_association"
    private const val REASON_MODERATION_BY_CONTRIBUTORS = "moderation_by_contributors"

    fun buildTournesolUrl(
        languages: List<String>?,
        dateKey: String,
        includeLowScoreVideos: Boolean
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
