package org.schabi.newpipe.util

import android.content.Context
import android.util.Base64
import androidx.preference.PreferenceManager
import io.reactivex.rxjava3.core.Maybe
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
    private const val AUTH_URL = "https://api.tournesol.app/o/authorize/"
    private const val TOKEN_URL = "https://api.tournesol.app/o/token/"
    private const val OAUTH_SCOPE = "read write groups"
    private const val DEFAULT_MIN_TTL_MS = 2 * 60 * 1000L

    fun saveAuthState(context: Context, authState: AuthState) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(SHARED_PREF_AUTH_STATE, authState.jsonSerializeString()).apply()
    }

    fun saveAuthState(context: Context, tokenResponse: TokenResponse) {
        val config = AuthorizationServiceConfiguration(
            android.net.Uri.parse(AUTH_URL),
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

    fun getValidAccessToken(
        context: Context,
        minTtlMs: Long = DEFAULT_MIN_TTL_MS
    ): Maybe<String> {
        val authState = getAuthState(context) ?: return Maybe.empty()
        val token = authState.accessToken
        val expirationTime = authState.accessTokenExpirationTime
        val now = System.currentTimeMillis()
        val needsRefresh = token.isNullOrBlank() ||
            (expirationTime != null && expirationTime <= now + minTtlMs)
        if (!needsRefresh) {
            return Maybe.just(token)
        }

        val refreshToken = authState.refreshToken
        if (refreshToken.isNullOrBlank()) {
            return Maybe.empty()
        }

        return performTokenRefresh(refreshToken)
            .flatMapMaybe { tokenResponse ->
                authState.update(tokenResponse, null)
                saveAuthState(context, authState)
                val refreshedToken = authState.accessToken
                if (refreshedToken.isNullOrBlank()) {
                    Maybe.empty()
                } else {
                    Maybe.just(refreshedToken)
                }
            }
    }

    fun performPasswordLogin(username: String, password: String): Single<TokenResponse> {
        return TournesolSecretExtractor().extractSecrets()
            .flatMap { secrets ->
                val clientId = secrets.first
                val clientSecret = secrets.second

                val formBody = FormBody.Builder()
                    .add("grant_type", "password")
                    .add("username", username)
                    .add("password", password)
                    .add("scope", OAUTH_SCOPE)
                    .build()

                performTokenRequest(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    formBody = formBody,
                    grantType = "password",
                    refreshTokenFallback = null
                )
            }
    }

    fun performTokenRefresh(refreshToken: String): Single<TokenResponse> {
        return TournesolSecretExtractor().extractSecrets()
            .flatMap { secrets ->
                val clientId = secrets.first
                val clientSecret = secrets.second

                val formBody = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .add("scope", OAUTH_SCOPE)
                    .build()

                performTokenRequest(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    formBody = formBody,
                    grantType = "refresh_token",
                    refreshTokenFallback = refreshToken
                )
            }
    }

    private fun performTokenRequest(
        clientId: String,
        clientSecret: String,
        formBody: FormBody,
        grantType: String,
        refreshTokenFallback: String?
    ): Single<TokenResponse> {
        return Single.create { emitter ->
            val client = DownloaderImpl.getInstance()?.getClient() ?: OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val authString = "$clientId:$clientSecret"
            val authHeader = "Basic " + Base64.encodeToString(
                authString.toByteArray(),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val errorLabel = if (grantType == "password") "Login failed" else "Token refresh failed"

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful && response.body != null) {
                        val responseBody = response.body!!.string()
                        val json = JSONObject(responseBody)
                        emitter.onSuccess(
                            buildTokenResponse(
                                clientId = clientId,
                                grantType = grantType,
                                json = json,
                                refreshTokenFallback = refreshTokenFallback
                            )
                        )
                    } else {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        emitter.onError(IOException("$errorLabel: ${response.code} $errorBody"))
                    }
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    private fun buildTokenResponse(
        clientId: String,
        grantType: String,
        json: JSONObject,
        refreshTokenFallback: String?
    ): TokenResponse {
        val accessToken = json.optString("access_token")
        if (accessToken.isBlank()) {
            throw IOException("Token response missing access_token")
        }

        val config = AuthorizationServiceConfiguration(
            android.net.Uri.parse(AUTH_URL),
            android.net.Uri.parse(TOKEN_URL)
        )
        val requestBuilder = TokenRequest.Builder(config, clientId)
            .setGrantType(grantType)
        if (grantType == "refresh_token" && !refreshTokenFallback.isNullOrBlank()) {
            requestBuilder.setRefreshToken(refreshTokenFallback)
        }
        val builder = TokenResponse.Builder(requestBuilder.build())

        builder.setAccessToken(accessToken)
        builder.setTokenType(json.optString("token_type"))
        if (json.has("expires_in")) {
            builder.setAccessTokenExpirationTime(
                System.currentTimeMillis() + (json.getLong("expires_in") * 1000)
            )
        }

        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
            ?: refreshTokenFallback
        if (!refreshToken.isNullOrBlank()) {
            builder.setRefreshToken(refreshToken)
        }

        val scope = json.optString("scope")
        if (scope.isNotBlank()) {
            builder.setScope(scope)
        }

        return builder.build()
    }
}
