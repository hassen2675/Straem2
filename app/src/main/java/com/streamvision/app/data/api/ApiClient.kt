package com.streamvision.app.data.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streamvision.app.data.models.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    private fun get(url: String): String {
        val req = Request.Builder().url(url).build()
        return client.newCall(req).execute().body?.string() ?: ""
    }

    fun authenticate(server: String, u: String, p: String): AuthResponse {
        return try { gson.fromJson(get("$server/player_api.php?username=$u&password=$p"), AuthResponse::class.java) }
        catch (e: Exception) { AuthResponse(null) }
    }

    fun getLiveCategories(s: String, u: String, p: String): List<Category> = try {
        gson.fromJson(get("$s/player_api.php?username=$u&password=$p&action=get_live_categories"),
            object : TypeToken<List<Category>>() {}.type)
    } catch (e: Exception) { emptyList() }

    fun getLiveStreams(s: String, u: String, p: String): List<Channel> = try {
        gson.fromJson(get("$s/player_api.php?username=$u&password=$p&action=get_live_streams"),
            object : TypeToken<List<Channel>>() {}.type)
    } catch (e: Exception) { emptyList() }

    fun getVodCategories(s: String, u: String, p: String): List<Category> = try {
        gson.fromJson(get("$s/player_api.php?username=$u&password=$p&action=get_vod_categories"),
            object : TypeToken<List<Category>>() {}.type)
    } catch (e: Exception) { emptyList() }

    fun getMovies(s: String, u: String, p: String): List<Movie> = try {
        gson.fromJson(get("$s/player_api.php?username=$u&password=$p&action=get_vod_streams"),
            object : TypeToken<List<Movie>>() {}.type)
    } catch (e: Exception) { emptyList() }

    fun getSeriesCategories(s: String, u: String, p: String): List<Category> = try {
        gson.fromJson(get("$s/player_api.php?username=$u&password=$p&action=get_series_categories"),
            object : TypeToken<List<Category>>() {}.type)
    } catch (e: Exception) { emptyList() }

    fun getSeries(s: String, u: String, p: String): List<Series> = try {
        gson.fromJson(get("$s/player_api.php?username=$u&password=$p&action=get_series"),
            object : TypeToken<List<Series>>() {}.type)
    } catch (e: Exception) { emptyList() }

    // Both TS and HLS stream URLs
    fun getLiveUrl(s: String, u: String, p: String, id: Int, useHls: Boolean = false) =
        if (useHls) "$s/live/$u/$p/$id.m3u8" else "$s/live/$u/$p/$id.ts"

    fun getMovieUrl(s: String, u: String, p: String, id: Int, ext: String) =
        "$s/movie/$u/$p/$id.$ext"
}
