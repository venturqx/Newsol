package org.schabi.newpipe.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.schabi.newpipe.R

class TournesolLoginDialog(
    private val context: Context,
    private val onLoginSuccess: (() -> Unit)? = null
) {
    private var loginDisposable: Disposable? = null

    fun show() {
        val builder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_tournesol_login, null)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.setOnDismissListener {
            loginDisposable?.dispose()
            loginDisposable = null
        }

        val usernameInput = dialogView.findViewById<EditText>(R.id.tournesol_username)
        val passwordInput = dialogView.findViewById<EditText>(R.id.tournesol_password)
        val loginButton = dialogView.findViewById<Button>(R.id.tournesol_login_button)
        val statusText = dialogView.findViewById<TextView>(R.id.tournesol_login_status)
        val cancelButton = dialogView.findViewById<Button>(R.id.tournesol_cancel_button)
        val registerButton = dialogView.findViewById<Button>(R.id.tournesol_register_button)

        cancelButton.setOnClickListener { dialog.dismiss() }
        registerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REGISTER_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.tournesol_login_missing_fields),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            loginButton.isEnabled = false
            statusText.visibility = View.VISIBLE

            loginDisposable = TournesolAuthManager.performPasswordLogin(username, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { tokenResponse ->
                        dialog.dismiss()
                        TournesolAuthManager.saveAuthState(context, tokenResponse)
                        onLoginSuccess?.invoke()
                        Toast.makeText(
                            context,
                            context.getString(R.string.tournesol_login_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    { error ->
                        loginButton.isEnabled = true
                        loginButton.setText(R.string.tournesol_login_button)
                        statusText.visibility = View.GONE
                        val message = error.message
                        val errorText = if (message.isNullOrBlank()) {
                            context.getString(R.string.tournesol_login_failed)
                        } else {
                            context.getString(R.string.tournesol_login_failed_with_message, message)
                        }
                        Toast.makeText(context, errorText, Toast.LENGTH_LONG).show()
                        error.printStackTrace()
                    }
                )
        }

        dialog.show()
    }

    companion object {
        private const val REGISTER_URL = "https://tournesol.app/signup"
    }
}
