package org.schabi.newpipe.util

import android.content.Context
import android.util.Base64
import androidx.preference.PreferenceManager
import io.reactivex.rxjava3.core.Single
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.schabi.newpipe.DownloaderImpl
import java.io.IOException
import java.util.concurrent.TimeUnit

object TournesolAuthManager {
    private const val SHARED_PREF_AUTH_STATE = "tournesol_auth_state"
    private const val TOKEN_URL = "https://api.tournesol.app/o/token/"

    fun saveAuthState(context: Context, authState: AuthState) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(SHARED_PREF_AUTH_STATE, authState.jsonSerializeString()).apply()
    }

    fun saveAuthState(context: Context, tokenResponse: TokenResponse) {
        val config = AuthorizationServiceConfiguration(
            android.net.Uri.parse("https://api.tournesol.app/o/authorize/"),
            android.net.Uri.parse(TOKEN_URL)
        )
        val authState = AuthState(config)
        authState.update(tokenResponse, null)
        saveAuthState(context, authState)
    }

    fun getAuthState(context: Context): AuthState? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val json = prefs.getString(SHARED_PREF_AUTH_STATE, null) ?: return null
        return try {
            AuthState.jsonDeserialize(json)
        } catch (_: Exception) {
            null
        }
    }

    fun performPasswordLogin(username: String, password: String): Single<TokenResponse> {
        return TournesolSecretExtractor().extractSecrets()
            .flatMap { secrets ->
                val clientId = secrets.first
                val clientSecret = secrets.second

                Single.create { emitter ->
                    val client = DownloaderImpl.getInstance()?.getClient() ?: OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val authString = "$clientId:$clientSecret"
                    val authHeader = "Basic " + Base64.encodeToString(
                        authString.toByteArray(),
                        Base64.NO_WRAP
                    )

                    val formBody = FormBody.Builder()
                        .add("grant_type", "password")
                        .add("username", username)
                        .add("password", password)
                        .add("scope", "read write groups")
                        .build()

                    val request = Request.Builder()
                        .url(TOKEN_URL)
                        .post(formBody)
                        .addHeader("Authorization", authHeader)
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()

                    try {
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful && response.body != null) {
                                val responseBody = response.body!!.string()
                                val json = JSONObject(responseBody)

                                val builder = TokenResponse.Builder(
                                    TokenRequest.Builder(
                                        AuthorizationServiceConfiguration(
                                            android.net.Uri.parse(
                                                "https://api.tournesol.app/o/authorize/"
                                            ),
                                            android.net.Uri.parse(TOKEN_URL)
                                        ),
                                        clientId
                                    ).setGrantType("password").build()
                                )

                                builder.setAccessToken(json.optString("access_token"))
                                builder.setTokenType(json.optString("token_type"))
                                if (json.has("expires_in")) {
                                    builder.setAccessTokenExpirationTime(
                                        System.currentTimeMillis() + (json.getLong("expires_in") * 1000)
                                    )
                                }
                                builder.setRefreshToken(json.optString("refresh_token"))
                                builder.setScope(json.optString("scope"))

                                emitter.onSuccess(builder.build())
                            } else {
                                val errorBody = response.body?.string() ?: "Unknown error"
                                emitter.onError(IOException("Login failed: ${response.code} $errorBody"))
                            }
                        }
                    } catch (e: Exception) {
                        emitter.onError(e)
                    }
                }
            }
    }
}
