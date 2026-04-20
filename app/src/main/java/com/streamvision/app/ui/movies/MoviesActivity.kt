package com.streamvision.app.ui.movies

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamvision.app.R
import com.streamvision.app.data.api.ApiClient
import com.streamvision.app.data.models.*
import com.streamvision.app.ui.player.PlayerActivity
import kotlin.concurrent.thread

class MoviesActivity : AppCompatActivity() {
    private var all = listOf<Movie>(); private var cats = listOf<Category>()
    private var selCat = "all"; private lateinit var adapter: MediaAdapter
    private var server = ""; private var username = ""; private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movies)
        server = intent.getStringExtra("server") ?: ""
        username = intent.getStringExtra("username") ?: ""
        password = intent.getStringExtra("password") ?: ""

        val rv = findViewById<RecyclerView>(R.id.rvMovies)
        val pb = findViewById<ProgressBar>(R.id.progressBar)
        val et = findViewById<EditText>(R.id.etSearch)
        val llC = findViewById<LinearLayout>(R.id.llCategories)

        rv.layoutManager = GridLayoutManager(this, 3)
        adapter = MediaAdapter { m -> startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra("url", ApiClient.getMovieUrl(server, username, password, m.streamId, m.ext))
            putExtra("name", m.name)
        }) }
        rv.adapter = adapter

        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filter(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        thread {
            try {
                cats = ApiClient.getVodCategories(server, username, password)
                all = ApiClient.getMovies(server, username, password)
                runOnUiThread {
                    pb.visibility = View.GONE; rv.visibility = View.VISIBLE
                    addChip(llC, "Alle", "all", true)
                    cats.forEach { addChip(llC, it.name, it.id, false) }
                    adapter.update(all.map { Triple(it.streamId, it.name, it.icon) to Pair(it.year, it.rating) })
                }
            } catch (e: Exception) { runOnUiThread { pb.visibility = View.GONE } }
        }
    }

    private fun addChip(c: LinearLayout, name: String, id: String, sel: Boolean) {
        val tv = TextView(this).apply {
            text = name; textSize = 12f; setPadding(24, 0, 24, 0)
            setTextColor(if (sel) 0xFFFFFFFF.toInt() else 0xFF8892B0.toInt())
            setBackgroundResource(if (sel) R.drawable.chip_selected else R.drawable.chip_normal)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(32))
            lp.marginEnd = dp(8); layoutParams = lp; gravity = Gravity.CENTER
            setOnClickListener {
                selCat = id
                for (i in 0 until c.childCount) { (c.getChildAt(i) as TextView).apply { setBackgroundResource(R.drawable.chip_normal); setTextColor(0xFF8892B0.toInt()) } }
                setBackgroundResource(R.drawable.chip_selected); setTextColor(0xFFFFFFFF.toInt()); filter(null)
            }
        }
        c.addView(tv)
    }

    private fun filter(q: String?) {
        val base = if (selCat == "all") all else all.filter { it.categoryId == selCat }
        val filtered = if (q.isNullOrEmpty()) base else base.filter { it.name.contains(q, true) }
        adapter.update(filtered.map { Triple(it.streamId, it.name, it.icon) to Pair(it.year, it.rating) })
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

class MediaAdapter(
    private val onClick: (Movie) -> Unit
) : RecyclerView.Adapter<MediaAdapter.VH>() {

    private var items = listOf<Pair<Triple<Int, String, String?>, Pair<String?, Double>>>()
    private var movies = listOf<Movie>()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv: android.widget.ImageView = v.findViewById(R.id.ivPoster)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvYear: TextView = v.findViewById(R.id.tvYear)
        val tvRating: TextView = v.findViewById(R.id.tvRating)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_media, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val (info, meta) = items[pos]
        h.tvTitle.text = info.second
        h.tvYear.text = meta.first ?: ""
        h.tvRating.text = if (meta.second > 0) "⭐ ${"%.1f".format(meta.second)}" else ""
        Glide.with(h.iv).load(info.third).centerCrop().placeholder(android.R.color.darker_gray).into(h.iv)
        h.itemView.setOnClickListener {
            if (pos < movies.size) onClick(movies[pos])
        }
    }

    fun update(list: List<Pair<Triple<Int, String, String?>, Pair<String?, Double>>>) {
        items = list; notifyDataSetChanged()
    }

    fun setMovies(list: List<Movie>) { movies = list }
}
