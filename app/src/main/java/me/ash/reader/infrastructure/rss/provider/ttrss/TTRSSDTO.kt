package me.ash.reader.infrastructure.rss.provider.ttrss

import com.google.gson.annotations.SerializedName

object TTRSSDTO {

    data class Response<T>(
        @SerializedName("seq") val seq: Int? = null,
        @SerializedName("status") val status: Int,
        @SerializedName("content") val content: T?,
    )

    data class LoginResponse(
        @SerializedName("session_id") val sessionId: String,
        @SerializedName("api_level") val apiLevel: Int? = null,
    )

    data class LoggedInResponse(
        @SerializedName("status") val status: Boolean,
    )

    data class Category(
        @SerializedName("id") val id: Long,
        @SerializedName("title") val title: String,
        @SerializedName("unread") val unread: Int? = null,
    )

    data class Feed(
        @SerializedName("id") val id: Long,
        @SerializedName("title") val title: String,
        @SerializedName("feed_url") val feedUrl: String,
        @SerializedName("cat_id") val catId: Long? = null,
        @SerializedName("unread") val unread: Int? = null,
        @SerializedName("has_icon") val hasIcon: Boolean? = null,
    )

    data class Headline(
        @SerializedName("id") val id: Long,
        @SerializedName("title") val title: String,
        @SerializedName("link") val link: String,
        @SerializedName("updated") val updated: Long? = null,
        @SerializedName("content") val content: String? = null,
        @SerializedName("unread") val unread: Boolean? = true,
        @SerializedName("marked") val marked: Boolean? = false,
        @SerializedName("author") val author: String? = null,
        @SerializedName("feed_id") val feedId: Long? = null,
    )

    data class SubscribeResponse(
        @SerializedName("status") val status: Map<String, Any>?,
    )
}
