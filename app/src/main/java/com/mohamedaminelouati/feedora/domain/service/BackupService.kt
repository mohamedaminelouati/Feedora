package com.mohamedaminelouati.feedora.domain.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.mohamedaminelouati.feedora.domain.model.account.Account
import com.mohamedaminelouati.feedora.domain.model.account.AccountType
import com.mohamedaminelouati.feedora.domain.model.article.ArchivedArticle
import com.mohamedaminelouati.feedora.domain.model.article.Article
import com.mohamedaminelouati.feedora.domain.model.feed.Feed
import com.mohamedaminelouati.feedora.domain.model.group.Group
import com.mohamedaminelouati.feedora.domain.repository.AccountDao
import com.mohamedaminelouati.feedora.domain.repository.ArticleDao
import com.mohamedaminelouati.feedora.domain.repository.FeedDao
import com.mohamedaminelouati.feedora.domain.repository.GroupDao
import com.mohamedaminelouati.feedora.infrastructure.di.IODispatcher
import com.mohamedaminelouati.feedora.infrastructure.preference.KeepArchivedPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.SyncIntervalPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.SyncOnStartPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.SyncOnlyOnWiFiPreference
import com.mohamedaminelouati.feedora.infrastructure.preference.SyncOnlyWhenChargingPreference
import com.mohamedaminelouati.feedora.ui.ext.fromDataStoreToJSONString
import com.mohamedaminelouati.feedora.ui.ext.fromJSONStringToDataStore
import com.mohamedaminelouati.feedora.ui.ext.getCurrentVersion

sealed interface BackupImportResult {
    data class Full(
        val accountsCount: Int,
        val groupsCount: Int,
        val feedsCount: Int,
        val articlesCount: Int,
        val hasPreferences: Boolean,
    ) : BackupImportResult

    data object PreferencesOnly : BackupImportResult
}

private class DateTypeAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {
    override fun serialize(src: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.time ?: 0L)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date {
        if (json == null || json.isJsonNull) return Date()
        return if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            Date(json.asLong)
        } else {
            val str = json.asString
            runCatching { Date(str.toLong()) }.getOrElse {
                runCatching {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).parse(str)
                }.getOrNull() ?: Date()
            }
        }
    }
}

private class AccountTypeAdapter : JsonSerializer<AccountType>, JsonDeserializer<AccountType> {
    override fun serialize(src: AccountType?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.id ?: 1)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): AccountType {
        if (json == null || json.isJsonNull) return AccountType.Local
        val id = if (json.isJsonObject && json.asJsonObject.has("id")) {
            json.asJsonObject.get("id").asInt
        } else if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            json.asInt
        } else {
            1
        }
        return runCatching { AccountType(id) }.getOrDefault(AccountType.Local)
    }
}

private class SyncIntervalAdapter : JsonSerializer<SyncIntervalPreference>, JsonDeserializer<SyncIntervalPreference> {
    override fun serialize(src: SyncIntervalPreference?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.value ?: SyncIntervalPreference.default.value)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): SyncIntervalPreference {
        if (json == null || json.isJsonNull) return SyncIntervalPreference.default
        val value = if (json.isJsonObject && json.asJsonObject.has("value")) {
            json.asJsonObject.get("value").asLong
        } else if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            json.asLong
        } else {
            SyncIntervalPreference.default.value
        }
        return SyncIntervalPreference.values.find { it.value == value } ?: SyncIntervalPreference.default
    }
}

private class SyncOnStartAdapter : JsonSerializer<SyncOnStartPreference>, JsonDeserializer<SyncOnStartPreference> {
    override fun serialize(src: SyncOnStartPreference?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.value ?: SyncOnStartPreference.default.value)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): SyncOnStartPreference {
        if (json == null || json.isJsonNull) return SyncOnStartPreference.default
        val value = if (json.isJsonObject && json.asJsonObject.has("value")) {
            json.asJsonObject.get("value").asBoolean
        } else if (json.isJsonPrimitive && json.asJsonPrimitive.isBoolean) {
            json.asBoolean
        } else {
            SyncOnStartPreference.default.value
        }
        return SyncOnStartPreference.values.find { it.value == value } ?: SyncOnStartPreference.default
    }
}

private class SyncOnlyOnWiFiAdapter : JsonSerializer<SyncOnlyOnWiFiPreference>, JsonDeserializer<SyncOnlyOnWiFiPreference> {
    override fun serialize(src: SyncOnlyOnWiFiPreference?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.value ?: SyncOnlyOnWiFiPreference.default.value)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): SyncOnlyOnWiFiPreference {
        if (json == null || json.isJsonNull) return SyncOnlyOnWiFiPreference.default
        val value = if (json.isJsonObject && json.asJsonObject.has("value")) {
            json.asJsonObject.get("value").asBoolean
        } else if (json.isJsonPrimitive && json.asJsonPrimitive.isBoolean) {
            json.asBoolean
        } else {
            SyncOnlyOnWiFiPreference.default.value
        }
        return SyncOnlyOnWiFiPreference.values.find { it.value == value } ?: SyncOnlyOnWiFiPreference.default
    }
}

private class SyncOnlyWhenChargingAdapter : JsonSerializer<SyncOnlyWhenChargingPreference>, JsonDeserializer<SyncOnlyWhenChargingPreference> {
    override fun serialize(src: SyncOnlyWhenChargingPreference?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.value ?: SyncOnlyWhenChargingPreference.default.value)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): SyncOnlyWhenChargingPreference {
        if (json == null || json.isJsonNull) return SyncOnlyWhenChargingPreference.default
        val value = if (json.isJsonObject && json.asJsonObject.has("value")) {
            json.asJsonObject.get("value").asBoolean
        } else if (json.isJsonPrimitive && json.asJsonPrimitive.isBoolean) {
            json.asBoolean
        } else {
            SyncOnlyWhenChargingPreference.default.value
        }
        return SyncOnlyWhenChargingPreference.values.find { it.value == value } ?: SyncOnlyWhenChargingPreference.default
    }
}

private class KeepArchivedAdapter : JsonSerializer<KeepArchivedPreference>, JsonDeserializer<KeepArchivedPreference> {
    override fun serialize(src: KeepArchivedPreference?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.value ?: KeepArchivedPreference.default.value)
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): KeepArchivedPreference {
        if (json == null || json.isJsonNull) return KeepArchivedPreference.default
        val value = if (json.isJsonObject && json.asJsonObject.has("value")) {
            json.asJsonObject.get("value").asLong
        } else if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            json.asLong
        } else {
            KeepArchivedPreference.default.value
        }
        return KeepArchivedPreference.values.find { it.value == value } ?: KeepArchivedPreference.default
    }
}

@Singleton
class BackupService
@Inject
constructor(
    private val accountDao: AccountDao,
    private val groupDao: GroupDao,
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val gson: Gson =
        GsonBuilder()
            .registerTypeAdapter(Date::class.java, DateTypeAdapter())
            .registerTypeAdapter(AccountType::class.java, AccountTypeAdapter())
            .registerTypeAdapter(SyncIntervalPreference::class.java, SyncIntervalAdapter())
            .registerTypeAdapter(SyncOnStartPreference::class.java, SyncOnStartAdapter())
            .registerTypeAdapter(SyncOnlyOnWiFiPreference::class.java, SyncOnlyOnWiFiAdapter())
            .registerTypeAdapter(SyncOnlyWhenChargingPreference::class.java, SyncOnlyWhenChargingAdapter())
            .registerTypeAdapter(KeepArchivedPreference::class.java, KeepArchivedAdapter())
            .create()

    suspend fun exportFullBackup(
        context: Context,
        outputStream: OutputStream,
        onProgress: suspend (progress: Float, status: String) -> Unit = { _, _ -> },
    ) = withContext(ioDispatcher) {
        onProgress(0.02f, "Initializing…")
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), 131072)
        val jsonWriter = JsonWriter(writer)

        jsonWriter.beginObject()
        jsonWriter.name("version").value(1)
        jsonWriter.name("appVersion").value(context.getCurrentVersion().toString())
        jsonWriter.name("timestamp").value(System.currentTimeMillis())

        // 1. Preferences
        onProgress(0.05f, "Preferences…")
        jsonWriter.name("preferences")
        val prefJson = context.fromDataStoreToJSONString()
        val prefMapType = object : TypeToken<Map<String, Any?>>() {}.type
        val preferencesMap: Map<String, Any?> = gson.fromJson(prefJson, prefMapType)
        gson.toJson(preferencesMap, prefMapType, jsonWriter)

        // 2. Accounts
        onProgress(0.08f, "Accounts…")
        jsonWriter.name("accounts")
        jsonWriter.beginArray()
        val accounts = accountDao.queryAll()
        for (acc in accounts) {
            gson.toJson(acc, Account::class.java, jsonWriter)
        }
        jsonWriter.endArray()

        // 3. Groups
        onProgress(0.10f, "Groups…")
        jsonWriter.name("groups")
        jsonWriter.beginArray()
        val groups = groupDao.queryAllGroups()
        for (grp in groups) {
            gson.toJson(grp, Group::class.java, jsonWriter)
        }
        jsonWriter.endArray()

        // 4. Feeds
        onProgress(0.12f, "Feeds…")
        jsonWriter.name("feeds")
        jsonWriter.beginArray()
        val feeds = feedDao.queryAllFeeds()
        for (feed in feeds) {
            gson.toJson(feed, Feed::class.java, jsonWriter)
        }
        jsonWriter.endArray()

        // 5. Articles (Paged in batches of 1000 to maximize performance while preventing OOM)
        jsonWriter.name("articles")
        jsonWriter.beginArray()
        val totalArticles = articleDao.countAllArticles()
        val articlePageSize = 1000
        var articleOffset = 0

        if (totalArticles == 0) {
            onProgress(0.90f, "0 article")
        }

        while (articleOffset < totalArticles) {
            val chunk = articleDao.queryArticlesPaged(articlePageSize, articleOffset)
            if (chunk.isEmpty()) break
            for (art in chunk) {
                gson.toJson(art, Article::class.java, jsonWriter)
            }
            jsonWriter.flush()
            articleOffset += chunk.size
            val currentProgress = 0.12f + 0.78f * (articleOffset.toFloat() / maxOf(1, totalArticles))
            onProgress(currentProgress, "$articleOffset / $totalArticles")
        }
        jsonWriter.endArray()

        // 6. Archived Articles (Paged in batches of 1000)
        onProgress(0.92f, "Archives…")
        jsonWriter.name("archivedArticles")
        jsonWriter.beginArray()
        val totalArchived = feedDao.countAllArchivedArticles()
        val archivedPageSize = 1000
        var archivedOffset = 0
        while (archivedOffset < totalArchived) {
            val chunk = feedDao.queryArchivedArticlesPaged(archivedPageSize, archivedOffset)
            if (chunk.isEmpty()) break
            for (arch in chunk) {
                gson.toJson(arch, ArchivedArticle::class.java, jsonWriter)
            }
            jsonWriter.flush()
            archivedOffset += chunk.size
        }
        jsonWriter.endArray()

        jsonWriter.endObject()
        jsonWriter.flush()
        writer.flush()
        onProgress(1.0f, "Done")
    }

    suspend fun exportPreferencesOnly(context: Context, outputStream: OutputStream) = withContext(ioDispatcher) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), 32768)
        val prefJson = context.fromDataStoreToJSONString()
        writer.write(prefJson)
        writer.flush()
    }

    suspend fun importBackup(
        context: Context,
        inputStream: InputStream,
        onProgress: suspend (progress: Float, status: String) -> Unit = { _, _ -> },
    ): Result<BackupImportResult> = withContext(ioDispatcher) {
        runCatching {
            onProgress(0.05f, "Reading backup…")
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 131072)
            val jsonReader = JsonReader(reader).apply {
                isLenient = true
            }

            var accountsCount = 0
            var groupsCount = 0
            var feedsCount = 0
            var articlesCount = 0
            var hasPreferences = false
            var isFullBackup = false
            val legacyPreferencesMap = mutableMapOf<String, Any?>()

            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                val fieldName = jsonReader.nextName()
                when (fieldName) {
                    "version", "appVersion", "timestamp" -> {
                        isFullBackup = true
                        jsonReader.skipValue()
                    }
                    "preferences" -> {
                        isFullBackup = true
                        hasPreferences = true
                        onProgress(0.10f, "Preferences…")
                        val prefMapType = object : TypeToken<Map<String, Any?>>() {}.type
                        val preferencesMap: Map<String, Any?> = gson.fromJson(jsonReader, prefMapType)
                        runCatching {
                            val prefJson = gson.toJson(preferencesMap)
                            prefJson.fromJSONStringToDataStore(context)
                        }
                    }
                    "accounts" -> {
                        isFullBackup = true
                        onProgress(0.15f, "Accounts…")
                        jsonReader.beginArray()
                        val batch = mutableListOf<Account>()
                        while (jsonReader.hasNext()) {
                            val acc: Account = gson.fromJson(jsonReader, Account::class.java)
                            batch.add(acc)
                        }
                        jsonReader.endArray()
                        if (batch.isNotEmpty()) {
                            accountDao.insertAllAccounts(batch)
                            accountsCount += batch.size
                        }
                    }
                    "groups" -> {
                        isFullBackup = true
                        onProgress(0.20f, "Groups…")
                        jsonReader.beginArray()
                        val batch = mutableListOf<Group>()
                        while (jsonReader.hasNext()) {
                            val grp: Group = gson.fromJson(jsonReader, Group::class.java)
                            batch.add(grp)
                        }
                        jsonReader.endArray()
                        if (batch.isNotEmpty()) {
                            groupDao.insertAllGroups(batch)
                            groupsCount += batch.size
                        }
                    }
                    "feeds" -> {
                        isFullBackup = true
                        onProgress(0.25f, "Feeds…")
                        jsonReader.beginArray()
                        val batch = mutableListOf<Feed>()
                        while (jsonReader.hasNext()) {
                            val feed: Feed = gson.fromJson(jsonReader, Feed::class.java)
                            batch.add(feed)
                        }
                        jsonReader.endArray()
                        if (batch.isNotEmpty()) {
                            feedDao.insertAllFeeds(batch)
                            feedsCount += batch.size
                        }
                    }
                    "articles" -> {
                        isFullBackup = true
                        jsonReader.beginArray()
                        val batch = mutableListOf<Article>()
                        val batchSize = 1000
                        while (jsonReader.hasNext()) {
                            val art: Article = gson.fromJson(jsonReader, Article::class.java)
                            batch.add(art)
                            if (batch.size >= batchSize) {
                                articleDao.insertAllArticles(batch)
                                articlesCount += batch.size
                                batch.clear()
                                onProgress(0.30f + (articlesCount % 10000) * 0.00005f, "$articlesCount articles")
                            }
                        }
                        jsonReader.endArray()
                        if (batch.isNotEmpty()) {
                            articleDao.insertAllArticles(batch)
                            articlesCount += batch.size
                            batch.clear()
                        }
                        onProgress(0.85f, "$articlesCount articles restored")
                    }
                    "archivedArticles" -> {
                        isFullBackup = true
                        jsonReader.beginArray()
                        val batch = mutableListOf<ArchivedArticle>()
                        while (jsonReader.hasNext()) {
                            val arch: ArchivedArticle = gson.fromJson(jsonReader, ArchivedArticle::class.java)
                            batch.add(arch)
                            if (batch.size >= 1000) {
                                feedDao.insertAllArchivedArticles(batch)
                                batch.clear()
                            }
                        }
                        jsonReader.endArray()
                        if (batch.isNotEmpty()) {
                            feedDao.insertAllArchivedArticles(batch)
                            batch.clear()
                        }
                    }
                    else -> {
                        val value: Any? = when (jsonReader.peek()) {
                            JsonToken.BOOLEAN -> jsonReader.nextBoolean()
                            JsonToken.NUMBER -> {
                                val numStr = jsonReader.nextString()
                                numStr.toLongOrNull() ?: numStr.toDoubleOrNull() ?: numStr
                            }
                            JsonToken.STRING -> jsonReader.nextString()
                            else -> {
                                jsonReader.skipValue()
                                null
                            }
                        }
                        if (value != null) {
                            legacyPreferencesMap[fieldName] = value
                        }
                    }
                }
            }
            jsonReader.endObject()

            onProgress(1.0f, "Done")

            if (isFullBackup) {
                BackupImportResult.Full(
                    accountsCount = accountsCount,
                    groupsCount = groupsCount,
                    feedsCount = feedsCount,
                    articlesCount = articlesCount,
                    hasPreferences = hasPreferences,
                )
            } else {
                if (legacyPreferencesMap.isNotEmpty()) {
                    runCatching {
                        val prefJson = gson.toJson(legacyPreferencesMap)
                        prefJson.fromJSONStringToDataStore(context)
                    }
                }
                BackupImportResult.PreferencesOnly
            }
        }
    }

    suspend fun importPreferencesOnly(context: Context, inputStream: InputStream): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val jsonElement = JsonParser.parseString(jsonString)
            if (!jsonElement.isJsonObject) {
                throw IllegalArgumentException("Invalid preferences JSON format")
            }
            val obj = jsonElement.asJsonObject
            val prefJson = if (obj.has("preferences")) {
                gson.toJson(obj.get("preferences"))
            } else {
                jsonString
            }
            runCatching {
                prefJson.fromJSONStringToDataStore(context)
            }
            Unit
        }
    }
}
