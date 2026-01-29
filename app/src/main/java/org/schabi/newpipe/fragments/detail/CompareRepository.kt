package org.schabi.newpipe.fragments.detail

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
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
import java.io.IOException

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
        score: Int
    ): JSONObject {
        val payload = JSONObject()
        payload.put("pollName", COMPARE_POLL)
        payload.put("entity_a", JSONObject().put("uid", lastUid))
        payload.put("entity_b", JSONObject().put("uid", currentUid))

        val criteriaScores = buildCriteriaScores(
            listOf(CriteriaScore(COMPARE_CRITERIA, score))
        )
        payload.put("criteria_scores", criteriaScores)
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

    private fun getHttpClient(): OkHttpClient {
        return DownloaderImpl.getInstance()?.getClient() ?: OkHttpClient.Builder().build()
    }

    private const val COMPARE_URL = "https://api.tournesol.app/users/me/comparisons/videos"
    private const val COMPARE_POLL = "videos"
    private const val COMPARE_CRITERIA = "largely_recommended"
}
