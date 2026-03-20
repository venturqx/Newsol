package org.schabi.newpipe.fragments.detail

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException
import java.net.URLEncoder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.DownloaderImpl
import org.schabi.newpipe.R
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory

object CompareRepository {
    fun buildTournesolUid(url: String, serviceId: Int): String? {
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

    fun submitComparison(
        token: String,
        lastUid: String,
        currentUid: String,
        score: Int
    ): Single<Int> = Single.fromCallable {
        val payload = buildComparisonPayload(
            lastUid,
            currentUid,
            listOf(CriteriaScore(COMPARE_CRITERIA, score))
        )
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

    internal fun submitComparisonWithCriteria(
        token: String,
        lastUid: String,
        currentUid: String,
        criteriaScores: List<CriteriaScore>
    ): Single<Int> = Single.fromCallable {
        val payload = buildComparisonPayload(lastUid, currentUid, criteriaScores)
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

    fun checkComparison(
        token: String,
        uidA: String,
        uidB: String
    ): Single<Boolean> = Single.fromCallable {
        val url = "$COMPARE_URL/$uidA/$uidB/"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        val client = getHttpClient()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            return@fromCallable when (response.code) {
                200 -> true
                404 -> false
                else -> throw IOException("HTTP ${response.code} $responseBody")
            }
        }
    }.subscribeOn(Schedulers.io())

    internal fun patchComparison(
        token: String,
        lastUid: String,
        currentUid: String,
        criteriaScores: List<CriteriaScore>
    ): Single<Int> = Single.fromCallable {
        val payload = buildPatchPayload(criteriaScores)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = payload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$COMPARE_URL/$lastUid/$currentUid/")
            .patch(body)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        val client = getHttpClient()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                return@fromCallable R.string.compare_more_submitted
            }
            throw IOException("HTTP ${response.code} $responseBody")
        }
    }.subscribeOn(Schedulers.io())

    fun fetchUserComparisons(
        token: String,
        username: String = "me",
        limit: Int = 20
    ): Single<List<CompareRecommendationItem>> = Single.fromCallable {
        val encodedUsername = if (username == "me") {
            "me"
        } else {
            URLEncoder.encode(username, Charsets.UTF_8.name())
        }
        val safeLimit = limit.coerceAtLeast(1)
        val url = "$BASE_URL/users/$encodedUsername/comparisons/videos?limit=$safeLimit"
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        val client = getHttpClient()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} $responseBody")
            }
            return@fromCallable parseComparisons(responseBody)
        }
    }.subscribeOn(Schedulers.io())

    private fun getTournesolServicePrefix(serviceId: Int): String? {
        return when (serviceId) {
            ServiceList.YouTube.serviceId -> "yt"
            ServiceList.SoundCloud.serviceId -> "sc"
            ServiceList.PeerTube.serviceId -> "peertube"
            else -> null
        }
    }

    private fun buildComparisonPayload(
        lastUid: String,
        currentUid: String,
        criteriaScores: List<CriteriaScore>
    ): JSONObject {
        val payload = JSONObject()
        payload.put("pollName", COMPARE_POLL)
        payload.put("entity_a", JSONObject().put("uid", lastUid))
        payload.put("entity_b", JSONObject().put("uid", currentUid))

        payload.put("criteria_scores", buildCriteriaScores(criteriaScores))
        return payload
    }

    private fun buildPatchPayload(
        criteriaScores: List<CriteriaScore>
    ): JSONObject {
        val payload = JSONObject()
        payload.put("criteria_scores", buildCriteriaScores(criteriaScores))
        return payload
    }

    private fun buildCriteriaScores(criteriaScores: List<CriteriaScore>): JSONArray {
        val scores = JSONArray()
        criteriaScores.forEach { scoreItem ->
            val item = JSONObject()
            item.put("criteria", scoreItem.criteria)
            item.put("score", scoreItem.score)
            item.put("score_max", SCORE_MAX)
            scores.put(item)
        }
        return scores
    }

    private fun parseComparisons(responseBody: String): List<CompareRecommendationItem> {
        val root = JSONObject(responseBody)
        val results = root.optJSONArray("results") ?: JSONArray()
        val parsed = ArrayList<CompareRecommendationItem>(results.length())
        for (index in 0 until results.length()) {
            val result = results.optJSONObject(index) ?: continue
            parsed.add(parseComparisonItem(result, index))
        }
        return parsed
    }

    private fun parseComparisonItem(
        result: JSONObject,
        index: Int
    ): CompareRecommendationItem {
        val entityA = result.optJSONObject("entity_a")
        val entityB = result.optJSONObject("entity_b")
        val videoA = parseComparisonVideo(
            entity = entityA,
            contexts = result.optJSONArray("entity_a_contexts"),
            fallbackUid = "entity_a_$index"
        )
        val videoB = parseComparisonVideo(
            entity = entityB,
            contexts = result.optJSONArray("entity_b_contexts"),
            fallbackUid = "entity_b_$index"
        )

        val scoreData = findCriteriaScore(
            result.optJSONArray("criteria_scores"),
            COMPARE_CRITERIA
        )
        val score = scoreData?.optNullableInt("score")
        val scoreMax = scoreData?.optNullableInt("score_max")
        val uid = "${videoA.uid}|${videoB.uid}|$index"

        return CompareRecommendationItem(
            uid = uid,
            videoA = videoA,
            videoB = videoB,
            largelyRecommendedScore = score,
            scoreMax = scoreMax
        )
    }

    private fun parseComparisonVideo(
        entity: JSONObject?,
        contexts: JSONArray?,
        fallbackUid: String
    ): CompareComparisonVideo {
        val metadataCandidates = ArrayList<JSONObject>()
        metadataCandidates.addAll(extractEntityMetadataCandidates(entity))
        metadataCandidates.addAll(extractContextMetadataCandidates(contexts))

        val uid = entity?.optString("uid").orEmpty().takeIf { it.isNotBlank() } ?: fallbackUid
        val title = findFirstText(metadataCandidates, TITLE_KEYS) ?: uid
        val uploader = findFirstText(metadataCandidates, UPLOADER_KEYS).orEmpty()
        val thumbnailUrl = findFirstThumbnailUrl(metadataCandidates) ?: buildFallbackThumbnailUrl(uid)
        val videoUrl = findFirstText(metadataCandidates, URL_KEYS)?.let(::normalizeUrl)
            ?: buildFallbackVideoUrl(uid)

        return CompareComparisonVideo(
            uid = uid,
            title = title,
            uploader = uploader,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl
        )
    }

    private fun extractEntityMetadataCandidates(entity: JSONObject?): List<JSONObject> {
        if (entity == null) {
            return emptyList()
        }
        val candidates = ArrayList<JSONObject>(8)
        collectObjectCandidates(entity, candidates)
        return candidates
    }

    private fun extractContextMetadataCandidates(contexts: JSONArray?): List<JSONObject> {
        if (contexts == null) {
            return emptyList()
        }
        val candidates = ArrayList<JSONObject>(contexts.length() * 8)
        for (index in 0 until contexts.length()) {
            collectObjectCandidates(contexts.opt(index), candidates)
        }
        return candidates
    }

    private fun collectObjectCandidates(
        value: Any?,
        candidates: MutableList<JSONObject>,
        depth: Int = 0
    ) {
        if (value == null || depth > 5) {
            return
        }
        when (value) {
            is JSONObject -> {
                value.optJSONObject("metadata")?.let { metadata ->
                    candidates.add(metadata)
                    collectObjectCandidates(metadata, candidates, depth + 1)
                }
                candidates.add(value)
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    collectObjectCandidates(value.opt(key), candidates, depth + 1)
                }
            }

            is JSONArray -> {
                for (index in 0 until value.length()) {
                    collectObjectCandidates(value.opt(index), candidates, depth + 1)
                }
            }
        }
    }

    private fun findFirstText(candidates: List<JSONObject>, keys: List<String>): String? {
        for (candidate in candidates) {
            extractText(candidate, keys)?.let { return it }
        }
        return null
    }

    private fun findFirstThumbnailUrl(candidates: List<JSONObject>): String? {
        for (candidate in candidates) {
            extractThumbnailUrl(candidate)?.let { return it }
        }
        return null
    }

    private fun findCriteriaScore(criteriaScores: JSONArray?, criteria: String): JSONObject? {
        if (criteriaScores == null) {
            return null
        }
        for (index in 0 until criteriaScores.length()) {
            val item = criteriaScores.optJSONObject(index) ?: continue
            if (item.optString("criteria") == criteria) {
                return item
            }
        }
        return null
    }

    private fun extractText(metadata: JSONObject?, keys: List<String>): String? {
        if (metadata == null) {
            return null
        }
        keys.forEach { key ->
            val value = metadata.optString(key).trim()
            if (value.isNotEmpty() && value != "null") {
                return value
            }
        }
        return null
    }

    private fun extractThumbnailUrl(metadata: JSONObject?): String? {
        if (metadata == null) {
            return null
        }
        for (key in THUMBNAIL_KEYS) {
            val raw = metadata.opt(key)
            when (raw) {
                is String -> {
                    val normalized = raw.trim()
                    if (normalized.isNotEmpty() && normalized != "null") {
                        return normalizeUrl(normalized)
                    }
                }

                is JSONObject -> {
                    extractText(raw, URL_KEYS)?.let { return normalizeUrl(it) }
                }

                is JSONArray -> {
                    for (index in 0 until raw.length()) {
                        when (val entry = raw.opt(index)) {
                            is String -> {
                                val normalized = entry.trim()
                                if (normalized.isNotEmpty() && normalized != "null") {
                                    return normalizeUrl(normalized)
                                }
                            }

                            is JSONObject -> {
                                extractText(entry, URL_KEYS)?.let { return normalizeUrl(it) }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun normalizeUrl(value: String): String {
        val normalized = value.trim()
        return if (normalized.startsWith("//")) {
            "https:$normalized"
        } else {
            normalized
        }
    }

    private fun buildFallbackThumbnailUrl(uid: String): String? {
        val videoId = extractYouTubeId(uid) ?: return null
        return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }

    private fun buildFallbackVideoUrl(uid: String): String? {
        val videoId = extractYouTubeId(uid) ?: return null
        return "https://www.youtube.com/watch?v=$videoId"
    }

    private fun extractYouTubeId(uid: String): String? {
        if (!uid.startsWith("yt:")) {
            return null
        }
        val videoId = uid.removePrefix("yt:").trim()
        return videoId.takeIf { it.isNotEmpty() }
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        return if (has(name) && !isNull(name)) {
            optInt(name)
        } else {
            null
        }
    }

    private fun getHttpClient(): OkHttpClient {
        return DownloaderImpl.getInstance()?.getClient() ?: OkHttpClient.Builder().build()
    }

    private const val BASE_URL = "https://api.tournesol.app"
    private const val COMPARE_URL = "$BASE_URL/users/me/comparisons/videos"
    private const val COMPARE_POLL = "videos"
    private const val COMPARE_CRITERIA = "largely_recommended"
    private val TITLE_KEYS = listOf("title", "name")
    private val UPLOADER_KEYS = listOf("uploader", "uploader_name", "channel_name", "author")
    private val URL_KEYS = listOf("url", "video_url", "link")
    private val THUMBNAIL_KEYS = listOf(
        "thumbnail_url",
        "thumbnail",
        "thumbnailUrl",
        "preview_url",
        "image",
        "images",
        "thumbnails"
    )
}
