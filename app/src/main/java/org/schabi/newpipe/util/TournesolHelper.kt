package org.schabi.newpipe.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object TournesolHelper {
    const val KIOSK_ID = "Tournesol"
    const val PREF_TOURNESOL_FILTER_LANGUAGES = "tournesol_filter_languages"
    const val PREF_TOURNESOL_FILTER_DATE_KEY = "tournesol_filter_date_key"
    const val DEFAULT_TOURNESOL_FILTER_LANGUAGES = "en"
    const val DEFAULT_TOURNESOL_FILTER_DATE_KEY = "3_months"

    fun buildTournesolUrl(languages: List<String>?, dateKey: String): String {
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

        return sb.toString()
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
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(cal.time)
    }
}
