package com.mohamedaminelouati.feedora.infrastructure.rss.provider.feedbin

import com.google.gson.annotations.SerializedName

object FeedbinDTO {

    data class Subscription(
        @SerializedName("id") val id: Long,
        @SerializedName("created_at") val createdAt: String? = null,
        @SerializedName("feed_id") val feedId: Long,
        @SerializedName("title") val title: String,
        @SerializedName("feed_url") val feedUrl: String,
        @SerializedName("site_url") val siteUrl: String? = null,
    )

    data class Tagging(
        @SerializedName("id") val id: Long,
        @SerializedName("feed_id") val feedId: Long,
        @SerializedName("name") val name: String,
    )

    data class Entry(
        @SerializedName("id") val id: Long,
        @SerializedName("feed_id") val feedId: Long,
        @SerializedName("title") val title: String?,
        @SerializedName("author") val author: String? = null,
        @SerializedName("summary") val summary: String? = null,
        @SerializedName("content") val content: String? = null,
        @SerializedName("url") val url: String,
        @SerializedName("published") val published: String? = null,
        @SerializedName("created_at") val createdAt: String? = null,
    )

    data class CreateSubscription(
        @SerializedName("feed_url") val feedUrl: String,
    )

    data class UnreadEntriesUpdate(
        @SerializedName("unread_entries") val unreadEntries: List<Long>,
    )

    data class StarredEntriesUpdate(
        @SerializedName("starred_entries") val starredEntries: List<Long>,
    )
}
