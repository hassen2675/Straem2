package com.streamvision.app.ui.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.streamvision.app.R
import java.text.SimpleDateFormat
import java.util.*

class PlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())

    // Channel list for navigation
    private var channelUrls  = arrayListOf<String>()
    private var channelNames = arrayListOf<String>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Receive channel list + current index
        channelUrls  = intent.getStringArrayListExtra("urls")  ?: arrayListOf(intent.getStringExtra("url") ?: "")
        channelNames = intent.getStringArrayListExtra("names") ?: arrayListOf(intent.getStringExtra("name") ?: "")
        currentIndex = intent.getIntExtra("index", 0)

        val playerView     = findViewById<PlayerView>(R.id.playerView)
        val pbLoading      = findViewById<ProgressBar>(R.id.pbLoading)
        val tvName         = findViewById<TextView>(R.id.tvChannelName)
        val tvError        = findViewById<TextView>(R.id.tvError)
        val btnBack        = findViewById<View>(R.id.btnBack)
        val tvTime         = findViewById<TextView>(R.id.tvTime)
        val btnPrev        = findViewById<TextView>(R.id.btnPrevChannel)
        val btnNext        = findViewById<TextView>(R.id.btnNextChannel)
        val tvIndex        = findViewById<TextView>(R.id.tvChannelIndex)

        // Clock
        val clockRunnable = object : Runnable {
            override fun run() {
                tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                handler.postDelayed(this, 30000)
            }
        }
        handler.post(clockRunnable)

        btnBack.setOnClickListener { finish() }

        // Show/hide nav buttons based on list size
        val llNav = findViewById<View>(R.id.llNavButtons)
        llNav.visibility = if (channelUrls.size > 1) View.VISIBLE else View.GONE

        // Setup ExoPlayer
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        playerView.useController = true

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> pbLoading.visibility = View.VISIBLE
                    Player.STATE_READY    -> pbLoading.visibility = View.GONE
                    Player.STATE_ENDED    -> finish()
                    else -> {}
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                pbLoading.visibility = View.GONE
                tvError.visibility = View.VISIBLE
                tvError.text = "⚠️ Stream Fehler\nVersuche nächsten Kanal..."
                // Auto skip to next channel on error after 2 seconds
                handler.postDelayed({ playNext(tvName, tvIndex, tvError, pbLoading) }, 2000)
            }
        })

        // Next / Prev buttons
        btnNext.setOnClickListener {
            tvError.visibility = View.GONE
            playNext(tvName, tvIndex, tvError, pbLoading)
        }
        btnPrev.setOnClickListener {
            tvError.visibility = View.GONE
            playPrev(tvName, tvIndex, tvError, pbLoading)
        }

        // Start playback
        playChannel(currentIndex, tvName, tvIndex, pbLoading)
    }

    private fun playChannel(index: Int, tvName: TextView, tvIndex: TextView, pbLoading: ProgressBar) {
        if (channelUrls.isEmpty()) return
        currentIndex = ((index % channelUrls.size) + channelUrls.size) % channelUrls.size
        val url  = channelUrls[currentIndex]
        val name = if (currentIndex < channelNames.size) channelNames[currentIndex] else "Kanal ${currentIndex + 1}"

        tvName.text  = name
        tvIndex.text = "${currentIndex + 1}/${channelUrls.size}"
        pbLoading.visibility = View.VISIBLE

        player.stop()
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    private fun playNext(tvName: TextView, tvIndex: TextView, tvError: TextView, pbLoading: ProgressBar) {
        tvError.visibility = View.GONE
        playChannel(currentIndex + 1, tvName, tvIndex, pbLoading)
    }

    private fun playPrev(tvName: TextView, tvIndex: TextView, tvError: TextView, pbLoading: ProgressBar) {
        tvError.visibility = View.GONE
        playChannel(currentIndex - 1, tvName, tvIndex, pbLoading)
    }

    override fun onPause()   { super.onPause();  player.pause() }
    override fun onResume()  { super.onResume(); player.play()  }
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        player.release()
    }
}
