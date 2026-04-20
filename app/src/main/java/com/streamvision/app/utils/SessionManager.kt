package com.streamvision.app.utils

import android.content.Context
import com.google.gson.Gson
import com.streamvision.app.data.models.AppSession
import com.streamvision.app.data.models.UserInfo

object SessionManager {
    private const val P = "sv_session"
    private val gson = Gson()

    fun save(ctx: Context, s: AppSession) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
        .putString("server", s.server).putString("username", s.username)
        .putString("password", s.password).putString("ui", gson.toJson(s.userInfo)).apply()

    fun load(ctx: Context): AppSession? {
        val p = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
        val sv = p.getString("server", null) ?: return null
        val u  = p.getString("username", null) ?: return null
        val pw = p.getString("password", null) ?: return null
        val ui = try { gson.fromJson(p.getString("ui", null), UserInfo::class.java) } catch (e: Exception) { return null }
        return AppSession(sv, u, pw, ui)
    }

    fun clear(ctx: Context) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply()

    fun getFavorites(ctx: Context): MutableSet<Int> =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getStringSet("favs", emptySet())!!
            .map { it.toInt() }.toMutableSet()

    fun toggleFavorite(ctx: Context, id: Int): Boolean {
        val favs = getFavorites(ctx)
        val added = if (favs.contains(id)) { favs.remove(id); false } else { favs.add(id); true }
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
            .putStringSet("favs", favs.map { it.toString() }.toSet()).apply()
        return added
    }

    fun isFavorite(ctx: Context, id: Int) = getFavorites(ctx).contains(id)

    fun formatExpDate(exp: String?): String {
        if (exp == null || exp == "null" || exp.isEmpty()) return "∞ Unbegrenzt"
        return try {
            java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(exp.toLong() * 1000))
        } catch (e: Exception) { exp }
    }

    // Stream format preference
    fun useHls(ctx: Context): Boolean =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getBoolean("use_hls", false)

    fun setUseHls(ctx: Context, value: Boolean) =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putBoolean("use_hls", value).apply()
}
