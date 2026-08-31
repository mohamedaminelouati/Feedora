package com.mohamedaminelouati.feedora.infrastructure.rss.provider.inoreader

import com.google.gson.annotations.SerializedName

object InoreaderDTO {

    data class UserInfo(
        @SerializedName("userId") val userId: String?,
        @SerializedName("userName") val userName: String?,
        @SerializedName("userProfileId") val userProfileId: String?,
        @SerializedName("userEmail") val userEmail: String?,
    )

    data class Category(
        @SerializedName("id") val id: String,
        @SerializedName("label") val label: String,
    )

    data class Subscription(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("categories") val categories: List<Category>?,
        @SerializedName("sortid") val sortId: String? = null,
        @SerializedName("url") val url: String,
        @SerializedName("htmlUrl") val htmlUrl: String? = null,
        @SerializedName("iconUrl") val iconUrl: String? = null,
    )

    data class SubscriptionsResponse(
        @SerializedName("subscriptions") val subscriptions: List<Subscription>?,
    )

    data class Content(
        @SerializedName("content") val content: String?,
    )

    data class Canonical(
        @SerializedName("href") val href: String?,
    )

    data class Origin(
        @SerializedName("streamId") val streamId: String?,
        @SerializedName("title") val title: String?,
        @SerializedName("htmlUrl") val htmlUrl: String?,
    )

    data class Item(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String?,
        @SerializedName("published") val published: Long?,
        @SerializedName("updated") val updated: Long?,
        @SerializedName("canonical") val canonical: List<Canonical>?,
        @SerializedName("alternate") val alternate: List<Canonical>?,
        @SerializedName("categories") val categories: List<String>?,
        @SerializedName("origin") val origin: Origin?,
        @SerializedName("summary") val summary: Content?,
        @SerializedName("author") val author: String?,
    )

    data class StreamResponse(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String?,
        @SerializedName("items") val items: List<Item>?,
        @SerializedName("continuation") val continuation: String?,
    )

    data class QuickAddResponse(
        @SerializedName("numResults") val numResults: Int?,
        @SerializedName("streamId") val streamId: String?,
        @SerializedName("query") val query: String?,
    )
}
