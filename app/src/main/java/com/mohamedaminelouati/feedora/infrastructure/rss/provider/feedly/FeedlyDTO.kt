package com.mohamedaminelouati.feedora.infrastructure.rss.provider.feedly

import com.google.gson.annotations.SerializedName

object FeedlyDTO {

    data class Profile(
        @SerializedName("id") val id: String,
        @SerializedName("email") val email: String?,
        @SerializedName("givenName") val givenName: String?,
        @SerializedName("familyName") val familyName: String?,
    )

    data class Category(
        @SerializedName("id") val id: String,
        @SerializedName("label") val label: String,
    )

    data class Subscription(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("categories") val categories: List<Category>?,
        @SerializedName("website") val website: String? = null,
        @SerializedName("visualUrl") val visualUrl: String? = null,
    )

    data class Content(
        @SerializedName("content") val content: String?,
        @SerializedName("direction") val direction: String? = null,
    )

    data class Alternate(
        @SerializedName("href") val href: String?,
        @SerializedName("type") val type: String? = null,
    )

    data class Origin(
        @SerializedName("streamId") val streamId: String?,
        @SerializedName("title") val title: String?,
        @SerializedName("htmlUrl") val htmlUrl: String?,
    )

    data class Visual(
        @SerializedName("url") val url: String?,
        @SerializedName("width") val width: Int? = null,
        @SerializedName("height") val height: Int? = null,
    )

    data class Entry(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String?,
        @SerializedName("published") val published: Long?,
        @SerializedName("crawled") val crawled: Long?,
        @SerializedName("author") val author: String?,
        @SerializedName("content") val content: Content?,
        @SerializedName("summary") val summary: Content?,
        @SerializedName("alternate") val alternate: List<Alternate>?,
        @SerializedName("origin") val origin: Origin?,
        @SerializedName("visual") val visual: Visual?,
        @SerializedName("tags") val tags: List<Category>? = null,
        @SerializedName("unread") val unread: Boolean? = true,
    )

    data class StreamContents(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String?,
        @SerializedName("items") val items: List<Entry>?,
        @SerializedName("continuation") val continuation: String?,
    )

    data class MarkersUpdate(
        @SerializedName("action") val action: String, // "markAsRead", "keepUnread", "markAsSaved", "markAsUnsaved"
        @SerializedName("type") val type: String = "entries",
        @SerializedName("entryIds") val entryIds: List<String>,
    )

    data class SubscribeRequest(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("categories") val categories: List<Category>?,
    )
}
