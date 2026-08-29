package me.ash.reader.infrastructure.rss.provider.miniflux

import com.google.gson.annotations.SerializedName

object MinifluxDTO {

    data class User(
        @SerializedName("id") val id: Long,
        @SerializedName("username") val username: String?,
        @SerializedName("is_admin") val isAdmin: Boolean? = false,
    )

    data class Category(
        @SerializedName("id") val id: Long,
        @SerializedName("title") val title: String,
        @SerializedName("user_id") val userId: Long? = null,
    )

    data class CategoryCreation(
        @SerializedName("title") val title: String,
    )

    data class Icon(
        @SerializedName("id") val id: Long,
        @SerializedName("data") val data: String?,
        @SerializedName("mime_type") val mimeType: String?,
    )

    data class Feed(
        @SerializedName("id") val id: Long,
        @SerializedName("user_id") val userId: Long? = null,
        @SerializedName("feed_url") val feedUrl: String,
        @SerializedName("site_url") val siteUrl: String? = null,
        @SerializedName("title") val title: String,
        @SerializedName("checked_at") val checkedAt: String? = null,
        @SerializedName("category") val category: Category? = null,
        @SerializedName("icon") val icon: Icon? = null,
    )

    data class FeedCreation(
        @SerializedName("feed_url") val feedUrl: String,
        @SerializedName("category_id") val categoryId: Long? = null,
    )

    data class FeedCreationResponse(
        @SerializedName("feed_id") val feedId: Long,
    )

    data class FeedUpdate(
        @SerializedName("title") val title: String? = null,
        @SerializedName("category_id") val categoryId: Long? = null,
    )

    data class Entry(
        @SerializedName("id") val id: Long,
        @SerializedName("user_id") val userId: Long? = null,
        @SerializedName("feed_id") val feedId: Long,
        @SerializedName("title") val title: String,
        @SerializedName("url") val url: String,
        @SerializedName("comments_url") val commentsUrl: String? = null,
        @SerializedName("published_at") val publishedAt: String? = null,
        @SerializedName("content") val content: String? = null,
        @SerializedName("author") val author: String? = null,
        @SerializedName("status") val status: String? = null, // "read", "unread", "removed"
        @SerializedName("starred") val starred: Boolean? = false,
        @SerializedName("reading_time") val readingTime: Int? = null,
        @SerializedName("feed") val feed: Feed? = null,
    )

    data class EntriesResponse(
        @SerializedName("total") val total: Int,
        @SerializedName("entries") val entries: List<Entry>?,
    )

    data class EntryStatusUpdate(
        @SerializedName("entry_ids") val entryIds: List<Long>,
        @SerializedName("status") val status: String,
    )
}
