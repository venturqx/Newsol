package org.schabi.newpipe.util

import android.util.Log
import io.reactivex.rxjava3.core.Single
import java.io.IOException
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.DownloaderImpl

class TournesolSecretExtractor {
    private val client = DownloaderImpl.getInstance()?.getClient() ?: OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun extractSecrets(): Single<Pair<String, String>> {
        return Single.create { emitter ->
            try {
                Log.d(TAG, "Starting dynamic secret extraction...")

                val mainHtml = fetch(BASE_URL)
                val jsUrls = HashSet<String>()

                val srcMatcher = SCRIPT_SRC_PATTERN.matcher(mainHtml)
                while (srcMatcher.find()) {
                    jsUrls.add(BASE_URL + srcMatcher.group(1).substring(1))
                }

                val hrefMatcher = HREF_PATTERN.matcher(mainHtml)
                while (hrefMatcher.find()) {
                    jsUrls.add(BASE_URL + hrefMatcher.group(1).substring(1))
                }

                Log.d(TAG, "Found ${jsUrls.size} initial JS bundles.")

                for (jsUrl in ArrayList(jsUrls)) {
                    if (emitter.isDisposed) {
                        return@create
                    }

                    Log.d(TAG, "Checking $jsUrl")
                    val jsContent = fetch(jsUrl)

                    var secrets = findCredentialsInText(jsContent)
                    if (secrets != null) {
                        emitter.onSuccess(secrets)
                        return@create
                    }

                    val chunkMatcher = CHUNK_PATTERN.matcher(jsContent)
                    while (chunkMatcher.find()) {
                        val chunkPath = chunkMatcher.group(1)
                        val chunkUrl = BASE_URL + chunkPath

                        if (!jsUrls.contains(chunkUrl)) {
                            Log.d(TAG, "Checking referenced chunk: $chunkUrl")
                            val chunkContent = fetch(chunkUrl)
                            secrets = findCredentialsInText(chunkContent)
                            if (secrets != null) {
                                emitter.onSuccess(secrets)
                                return@create
                            }
                            jsUrls.add(chunkUrl)
                        }
                    }
                }

                emitter.onError(IOException("Could not find Tournesol API credentials"))
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }
    }

    private fun fetch(url: String): String {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.body == null) {
                throw IOException("Failed to fetch $url: ${response.code}")
            }
            return response.body!!.string()
        }
    }

    private fun findCredentialsInText(text: String): Pair<String, String>? {
        val idMatcher = ID_PATTERN.matcher(text)
        while (idMatcher.find()) {
            val potentialId = idMatcher.group(1)
            val secretMatcher = SECRET_PATTERN.matcher(text)
            if (secretMatcher.find()) {
                val potentialSecret = secretMatcher.group(1)
                Log.d(TAG, "Found credentials! ID: ${potentialId.substring(0, 5)}...")
                return Pair(potentialId, potentialSecret)
            }
        }
        return null
    }

    companion object {
        private const val TAG = "TournesolSecrets"
        private const val BASE_URL = "https://tournesol.app/"

        private val SCRIPT_SRC_PATTERN =
            Pattern.compile("src=\\\"(/assets/.*?-([\\w-]+)\\.js)\\\"")
        private val HREF_PATTERN =
            Pattern.compile("href=\\\"(/assets/.*?-([\\w-]+)\\.js)\\\"")
        private val CHUNK_PATTERN =
            Pattern.compile("[\\\"'](assets/[a-zA-Z0-9_-]+\\.js)[\\\"']")

        private val ID_PATTERN = Pattern.compile("[\\\"']([a-zA-Z0-9]{40})[\\\"']")
        private val SECRET_PATTERN = Pattern.compile("[\\\"']([a-zA-Z0-9]{128})[\\\"']")
    }
}
