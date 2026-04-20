package com.streamvision.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.streamvision.app.R
import com.streamvision.app.data.api.ApiClient
import com.streamvision.app.data.models.AppSession
import com.streamvision.app.ui.home.HomeActivity
import com.streamvision.app.utils.SessionManager
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    // ✅ Server URL fest eingebaut — Nutzer sieht nur Username + Passwort
    private val SERVER = "http://rianto.xyz:8080"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-Login wenn Session gespeichert
        SessionManager.load(this)?.let { session ->
            startHome(session)
            return
        }

        setContentView(R.layout.activity_login)

        val etUser = findViewById<EditText>(R.id.etUsername)
        val etPass = findViewById<EditText>(R.id.etPassword)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val tvError = findViewById<TextView>(R.id.tvError)

        // Load saved credentials
        val prefs = getSharedPreferences("sv_prefs", MODE_PRIVATE)
        etUser.setText(prefs.getString("username", ""))
        etPass.setText(prefs.getString("password", ""))

        btnConnect.setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                showError(tvError, "⚠️ Bitte alle Felder ausfüllen")
                return@setOnClickListener
            }

            btnConnect.isEnabled = false
            btnConnect.text = "Verbinde..."
            tvError.visibility = View.GONE

            thread {
                try {
                    val resp = ApiClient.authenticate(SERVER, user, pass)
                    runOnUiThread {
                        if (resp.userInfo != null) {
                            // Save credentials
                            prefs.edit().putString("username", user).putString("password", pass).apply()
                            val session = AppSession(SERVER, user, pass, resp.userInfo)
                            SessionManager.save(this, session)
                            startHome(session)
                        } else {
                            showError(tvError, "❌ Ungültige Anmeldedaten")
                            resetBtn(btnConnect)
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        showError(tvError, "❌ Verbindungsfehler: ${e.message}")
                        resetBtn(btnConnect)
                    }
                }
            }
        }
    }

    private fun showError(tv: TextView, msg: String) {
        tv.text = msg
        tv.visibility = View.VISIBLE
    }

    private fun resetBtn(btn: Button) {
        btn.isEnabled = true
        btn.text = "ANMELDEN"
    }

    private fun startHome(session: AppSession) {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            putExtra("server", session.server)
            putExtra("username", session.username)
            putExtra("password", session.password)
        })
        finish()
    }
}
