package org.schabi.newpipe.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.schabi.newpipe.R

class TournesolLoginDialog(private val context: Context) {
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
        val cancelButton = dialogView.findViewById<Button>(R.id.tournesol_cancel_button)

        cancelButton.setOnClickListener { dialog.dismiss() }

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
            loginButton.setText(R.string.tournesol_login_in_progress)

            loginDisposable = TournesolAuthManager.performPasswordLogin(username, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { tokenResponse ->
                        dialog.dismiss()
                        TournesolAuthManager.saveAuthState(context, tokenResponse)
                        Toast.makeText(
                            context,
                            context.getString(R.string.tournesol_login_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    { error ->
                        loginButton.isEnabled = true
                        loginButton.setText(R.string.tournesol_login_button)
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
}
