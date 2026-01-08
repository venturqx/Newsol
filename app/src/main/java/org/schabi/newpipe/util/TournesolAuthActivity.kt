package org.schabi.newpipe.util

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import org.schabi.newpipe.BuildConfig
import org.schabi.newpipe.R

class TournesolAuthActivity : AppCompatActivity() {
    private lateinit var authService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthorizationService(this)

        if (savedInstanceState == null) {
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            "START_AUTH" -> startAuth()
            "AUTH_COMPLETE" -> handleAuthResponse(intent)
            "AUTH_CANCELLED" -> {
                Toast.makeText(
                    this,
                    getString(R.string.tournesol_auth_cancelled),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun startAuth() {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://api.tournesol.app/o/authorize/"),
            Uri.parse("https://api.tournesol.app/o/token/")
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            "uUhctKD6C5LC4rPkklnshylcr3PHHQ8DTXBon8Mx",
            ResponseTypeValues.CODE,
            Uri.parse("${BuildConfig.APPLICATION_ID}:/oauth2redirect")
        ).setScope("read write groups").build()

        val completionIntent = Intent(this, TournesolAuthActivity::class.java).apply {
            action = "AUTH_COMPLETE"
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        var flags = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE
        }

        val successIntent = PendingIntent.getActivity(this, 0, completionIntent, flags)

        val cancelIntent = Intent(this, TournesolAuthActivity::class.java).apply {
            action = "AUTH_CANCELLED"
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val failureIntent = PendingIntent.getActivity(this, 0, cancelIntent, flags)

        authService.performAuthorizationRequest(authRequest, successIntent, failureIntent)
    }

    private fun handleAuthResponse(intent: Intent) {
        val resp = AuthorizationResponse.fromIntent(intent)
        val ex = AuthorizationException.fromIntent(intent)

        if (resp != null) {
            val authState = AuthState(resp, ex)
            authService.performTokenRequest(
                resp.createTokenExchangeRequest()
            ) { response: TokenResponse?, tokenEx: AuthorizationException? ->
                if (response != null) {
                    authState.update(response, tokenEx)
                    TournesolAuthManager.saveAuthState(this@TournesolAuthActivity, authState)
                    Toast.makeText(
                        this@TournesolAuthActivity,
                        getString(R.string.tournesol_login_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@TournesolAuthActivity,
                        getString(R.string.tournesol_login_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }
        } else {
            Toast.makeText(this, getString(R.string.tournesol_login_failed), Toast.LENGTH_SHORT)
                .show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authService.dispose()
    }
}
