package com.streamvision.app.data.models

import com.google.gson.annotations.SerializedName

data class AuthResponse(@SerializedName("user_info") val userInfo: UserInfo?)

data class UserInfo(
    @SerializedName("username") val username: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("exp_date") val expDate: String? = null,
    @SerializedName("max_connections") val maxConnections: String = "1",
    @SerializedName("active_cons") val activeCons: String = "0"
)

data class Category(
    @SerializedName("category_id") val id: String = "",
    @SerializedName("category_name") val name: String = ""
)

data class Channel(
    @SerializedName("stream_id") val streamId: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("stream_icon") val icon: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("epg_channel_id") val epgId: String? = null
) {
    val isHD get() = name.contains("HD", true) && !name.contains("FHD", true) && !name.contains("4K", true)
    val isFHD get() = name.contains("FHD", true) || name.contains("4K", true)
    val quality get() = when { isFHD -> "4K"; isHD -> "HD"; else -> "" }
}

data class Movie(
    @SerializedName("stream_id") val streamId: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("stream_icon") val icon: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("rating_5based") val rating: Double = 0.0,
    @SerializedName("year") val year: String? = null,
    @SerializedName("container_extension") val ext: String = "mp4"
)

data class Series(
    @SerializedName("series_id") val seriesId: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("rating_5based") val rating: Double = 0.0,
    @SerializedName("releaseDate") val releaseDate: String? = null
)

data class AppSession(val server: String, val username: String, val password: String, val userInfo: UserInfo)
