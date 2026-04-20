package com.streamvision.app.ui.live

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamvision.app.R
import com.streamvision.app.data.api.ApiClient
import com.streamvision.app.data.models.Category
import com.streamvision.app.data.models.Channel
import com.streamvision.app.ui.player.PlayerActivity
import com.streamvision.app.utils.SessionManager
import kotlin.concurrent.thread

class LiveActivity : AppCompatActivity() {

    private var allChannels = listOf<Channel>()
    private var allCategories = listOf<Category>()
    private var filteredChannels = listOf<Channel>()
    private var selectedCatId = "all"
    private var selectedChannel: Channel? = null
    private var server = ""; private var username = ""; private var password = ""
    private var useHls = true

    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live)

        server   = intent.getStringExtra("server") ?: ""
        username = intent.getStringExtra("username") ?: ""
        password = intent.getStringExtra("password") ?: ""

        val rvChannels  = findViewById<RecyclerView>(R.id.rvChannels)
        val rvCats      = findViewById<RecyclerView>(R.id.rvCategories)
        val pb          = findViewById<ProgressBar>(R.id.progressBar)
        val et          = findViewById<EditText>(R.id.etSearch)
        val btnBack     = findViewById<View>(R.id.btnBack)
        val btnPlay     = findViewById<TextView>(R.id.btnPlay)
        val tvPrevName  = findViewById<TextView>(R.id.tvPreviewName)
        val tvPrevEpg   = findViewById<TextView>(R.id.tvPreviewEpg)
        val tvPrevQual  = findViewById<TextView>(R.id.tvPreviewQuality)
        val ivPreview   = findViewById<ImageView>(R.id.ivPreview)
        val tvPrevIcon  = findViewById<TextView>(R.id.tvPreviewIcon)

        btnBack.setOnClickListener { finish() }

        // Category adapter
        categoryAdapter = CategoryAdapter { cat ->
            selectedCatId = cat.id
            categoryAdapter.setSelected(cat.id)
            filterChannels(et.text.toString())
        }

        rvCats.layoutManager = LinearLayoutManager(this)
        rvCats.adapter = categoryAdapter

        // Channel adapter
        channelAdapter = ChannelAdapter(
            ctx = this,
            items = emptyList(),
            onClick = { ch ->
                selectedChannel = ch
                // Update preview
                tvPrevName.text = ch.name
                tvPrevEpg.text = ""
                tvPrevIcon.visibility = View.VISIBLE
                btnPlay.visibility = View.VISIBLE
                if (!ch.icon.isNullOrEmpty()) {
                    tvPrevIcon.visibility = View.GONE
                    Glide.with(this).load(ch.icon).into(ivPreview)
                }
                if (ch.quality.isNotEmpty()) {
                    tvPrevQual.visibility = View.VISIBLE
                    tvPrevQual.text = ch.quality
                    tvPrevQual.setTextColor(if (ch.isFHD) 0xFF6C63FF.toInt() else 0xFF00D9A5.toInt())
                }
            },
            onDblClick = { ch -> playChannel(ch) },
            onFav = { ch -> SessionManager.toggleFavorite(this, ch.streamId) }
        )

        rvChannels.layoutManager = LinearLayoutManager(this)
        rvChannels.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        rvChannels.adapter = channelAdapter

        // Play button
        btnPlay.setOnClickListener {
            selectedChannel?.let { playChannel(it) }
        }

        // Search
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterChannels(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        // Load data
        thread {
            try {
                val cats = ApiClient.getLiveCategories(server, username, password)
                val chs  = ApiClient.getLiveStreams(server, username, password)
                allCategories = cats
                allChannels   = chs

                runOnUiThread {
                    pb.visibility = View.GONE
                    rvChannels.visibility = View.VISIBLE

                    // Build category list with counts
                    val allCat = Category("all", "ALL  ${chs.size}")
                    val favCat = Category("fav", "⭐ Favoriten")
                    val catList = mutableListOf(allCat, favCat)
                    cats.forEach { c ->
                        val count = chs.count { it.categoryId == c.id }
                        catList.add(Category(c.id, "${c.name}  $count"))
                    }
                    categoryAdapter.update(catList)

                    filterChannels("")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    pb.visibility = View.GONE
                    Toast.makeText(this, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterChannels(query: String) {
        val favIds = SessionManager.getFavorites(this)
        val base = when (selectedCatId) {
            "all" -> allChannels
            "fav" -> allChannels.filter { favIds.contains(it.streamId) }
            else  -> allChannels.filter { it.categoryId == selectedCatId }
        }
        filteredChannels = if (query.isEmpty()) base
                           else base.filter { it.name.contains(query, ignoreCase = true) }
        channelAdapter.update(filteredChannels)
    }

    private fun playChannel(ch: Channel) {
        val urls  = ArrayList(filteredChannels.map { ApiClient.getLiveUrl(server, username, password, it.streamId, useHls) })
        val names = ArrayList(filteredChannels.map { it.name })
        val idx   = filteredChannels.indexOfFirst { it.streamId == ch.streamId }
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putStringArrayListExtra("urls", urls)
            putStringArrayListExtra("names", names)
            putExtra("index", idx)
        })
    }
}

// ─── Category Adapter ────────────────────────────────────────────────────────
class CategoryAdapter(
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var items = listOf<Category>()
    private var selectedId = "all"

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView  = v.findViewById(R.id.tvCatName)
        val tvCount: TextView = v.findViewById(R.id.tvCatCount)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_category, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val cat = items[pos]
        val parts = cat.name.split("  ")
        h.tvName.text  = parts[0]
        h.tvCount.text = if (parts.size > 1) parts[1] else ""

        val selected = cat.id == selectedId
        h.itemView.setBackgroundResource(if (selected) R.drawable.cat_selected_bg else android.R.color.transparent)
        h.tvName.setTextColor(if (selected) 0xFF6C63FF.toInt() else 0xFF8892B0.toInt())
        h.tvName.textSize = if (selected) 13.5f else 13f

        h.itemView.setOnClickListener { onClick(cat) }
    }

    fun update(list: List<Category>) { items = list; notifyDataSetChanged() }
    fun setSelected(id: String) { selectedId = id; notifyDataSetChanged() }
}

// ─── Channel Adapter ─────────────────────────────────────────────────────────
class ChannelAdapter(
    private val ctx: android.content.Context,
    private var items: List<Channel>,
    private val onClick: (Channel) -> Unit,
    private val onDblClick: (Channel) -> Unit,
    private val onFav: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private var lastClickTime = 0L
    private var lastClickId   = -1

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNum:     TextView                      = v.findViewById(R.id.tvNum)
        val ivIcon:    android.widget.ImageView      = v.findViewById(R.id.ivIcon)
        val tvName:    TextView                      = v.findViewById(R.id.tvName)
        val tvSub:     TextView                      = v.findViewById(R.id.tvSub)
        val tvQuality: TextView                      = v.findViewById(R.id.tvQuality)
        val ivFav:     android.widget.ImageView      = v.findViewById(R.id.ivFav)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_channel, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val ch = items[pos]
        h.tvNum.text  = "${pos + 1}"
        h.tvName.text = ch.name
        h.tvSub.text  = ""

        if (ch.quality.isNotEmpty()) {
            h.tvQuality.visibility = View.VISIBLE
            h.tvQuality.text = ch.quality
            h.tvQuality.setTextColor(if (ch.isFHD) 0xFF6C63FF.toInt() else 0xFF00D9A5.toInt())
        } else h.tvQuality.visibility = View.GONE

        val isFav = SessionManager.isFavorite(ctx, ch.streamId)
        h.ivFav.setImageResource(if (isFav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)

        if (!ch.icon.isNullOrEmpty())
            Glide.with(ctx).load(ch.icon).placeholder(R.drawable.ic_logo).into(h.ivIcon)

        h.itemView.setOnClickListener {
            val now = System.currentTimeMillis()
            if (ch.streamId == lastClickId && now - lastClickTime < 500) {
                onDblClick(ch) // double tap = play
            } else {
                onClick(ch) // single tap = preview
            }
            lastClickTime = now
            lastClickId   = ch.streamId
        }
        h.ivFav.setOnClickListener { onFav(ch); notifyItemChanged(pos) }
    }

    fun update(list: List<Channel>) { items = list; notifyDataSetChanged() }
    fun getCurrentList(): List<Channel> = items
}
