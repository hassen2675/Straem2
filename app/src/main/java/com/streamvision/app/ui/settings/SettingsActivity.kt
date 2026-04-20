package com.streamvision.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.streamvision.app.R
import com.streamvision.app.ui.login.LoginActivity
import com.streamvision.app.utils.SessionManager

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val saved = SessionManager.load(this)

        findViewById<TextView>(R.id.tvUsername).text = saved?.username ?: "—"
        findViewById<TextView>(R.id.tvStatus).text   = saved?.userInfo?.status ?: "—"
        findViewById<TextView>(R.id.tvExpiry).text   = SessionManager.formatExpDate(saved?.userInfo?.expDate)
        findViewById<TextView>(R.id.tvMaxConn).text  = saved?.userInfo?.maxConnections ?: "—"
        findViewById<TextView>(R.id.tvServer).text   = saved?.server ?: "—"
        findViewById<TextView>(R.id.tvVersion).text  = "StreamVision v1.0"

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            SessionManager.clear(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}
