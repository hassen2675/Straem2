package com.streamvision.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.streamvision.app.R
import com.streamvision.app.ui.live.LiveActivity
import com.streamvision.app.ui.movies.MoviesActivity
import com.streamvision.app.ui.series.SeriesActivity
import com.streamvision.app.ui.settings.SettingsActivity
import com.streamvision.app.utils.SessionManager

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val server   = intent.getStringExtra("server") ?: ""
        val username = intent.getStringExtra("username") ?: ""
        val password = intent.getStringExtra("password") ?: ""

        val saved = SessionManager.load(this)
        findViewById<TextView>(R.id.tvUser).text    = "👤 $username"
        findViewById<TextView>(R.id.tvExpiry).text  = "⏰ ${SessionManager.formatExpDate(saved?.userInfo?.expDate)}"

        fun go(cls: Class<*>) = startActivity(Intent(this, cls).apply {
            putExtra("server", server)
            putExtra("username", username)
            putExtra("password", password)
        })

        findViewById<LinearLayout>(R.id.tileLive).setOnClickListener    { go(LiveActivity::class.java) }
        findViewById<LinearLayout>(R.id.tileMovies).setOnClickListener  { go(MoviesActivity::class.java) }
        findViewById<LinearLayout>(R.id.tileSeries).setOnClickListener  { go(SeriesActivity::class.java) }
        findViewById<LinearLayout>(R.id.tileSettings).setOnClickListener { go(SettingsActivity::class.java) }
        findViewById<LinearLayout>(R.id.tileAccount).setOnClickListener  { go(SettingsActivity::class.java) }
        findViewById<LinearLayout>(R.id.tileExit).setOnClickListener    { finishAffinity() }
    }
}
