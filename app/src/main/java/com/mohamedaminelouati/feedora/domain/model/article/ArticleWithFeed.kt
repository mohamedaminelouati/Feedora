package com.mohamedaminelouati.feedora.domain.model.article

import androidx.room.Embedded
import androidx.room.Relation
import com.mohamedaminelouati.feedora.domain.model.feed.Feed

/**
 * An [article] contains a [feed].
 */
data class ArticleWithFeed(
    @Embedded
    var article: Article,
    @Relation(parentColumn = "feedId", entityColumn = "id")
    var feed: Feed,
)
