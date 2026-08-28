package me.ash.reader.domain.service

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
import me.ash.reader.domain.model.account.Account
import me.ash.reader.domain.model.account.AccountType
import me.ash.reader.domain.model.article.ArchivedArticle
import me.ash.reader.domain.model.article.Article
import me.ash.reader.domain.model.feed.Feed
import me.ash.reader.domain.model.group.Group
import me.ash.reader.domain.repository.AccountDao
import me.ash.reader.domain.repository.ArticleDao
import me.ash.reader.domain.repository.FeedDao
import me.ash.reader.domain.repository.GroupDao
import me.ash.reader.infrastructure.di.IODispatcher
import me.ash.reader.infrastructure.preference.KeepArchivedPreference
import me.ash.reader.infrastructure.preference.SyncIntervalPreference
import me.ash.reader.infrastructure.preference.SyncOnStartPreference
import me.ash.reader.infrastructure.preference.SyncOnlyOnWiFiPreference
import me.ash.reader.infrastructure.preference.SyncOnlyWhenChargingPreference
import me.ash.reader.ui.ext.fromDataStoreToJSONString
import me.ash.reader.ui.ext.fromJSONStringToDataStore
import me.ash.reader.ui.ext.getCurrentVersion
import timber.log.Timber

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
            .setPrettyPrinting()
            .create()

    suspend fun exportFullBackup(context: Context, outputStream: OutputStream) = withContext(ioDispatcher) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), 65536)
        val jsonWriter = JsonWriter(writer).apply {
            setIndent("  ")
        }

        jsonWriter.beginObject()
        jsonWriter.name("version").value(1)
        jsonWriter.name("appVersion").value(context.getCurrentVersion().toString())
        jsonWriter.name("timestamp").value(System.currentTimeMillis())

        // 1. Preferences
        jsonWriter.name("preferences")
        val prefJson = context.fromDataStoreToJSONString()
        val prefMapType = object : TypeToken<Map<String, Any?>>() {}.type
        val preferencesMap: Map<String, Any?> = gson.fromJson(prefJson, prefMapType)
        gson.toJson(preferencesMap, prefMapType, jsonWriter)

        // 2. Accounts
        jsonWriter.name("accounts")
        jsonWriter.beginArray()
        val accounts = accountDao.queryAll()
        for (acc in accounts) {
            gson.toJson(acc, Account::class.java, jsonWriter)
        }
        jsonWriter.endArray()

        // 3. Groups
        jsonWriter.name("groups")
        jsonWriter.beginArray()
        val groups = groupDao.queryAllGroups()
        for (grp in groups) {
            gson.toJson(grp, Group::class.java, jsonWriter)
        }
        jsonWriter.endArray()

        // 4. Feeds
        jsonWriter.name("feeds")
        jsonWriter.beginArray()
        val feeds = feedDao.queryAllFeeds()
        for (feed in feeds) {
            gson.toJson(feed, Feed::class.java, jsonWriter)
        }
        jsonWriter.endArray()

        // 5. Articles (Paged to prevent any OOM)
        jsonWriter.name("articles")
        jsonWriter.beginArray()
        val totalArticles = articleDao.countAllArticles()
        val articlePageSize = 200
        var articleOffset = 0
        while (articleOffset < totalArticles) {
            val chunk = articleDao.queryArticlesPaged(articlePageSize, articleOffset)
            if (chunk.isEmpty()) break
            for (art in chunk) {
                gson.toJson(art, Article::class.java, jsonWriter)
            }
            jsonWriter.flush()
            articleOffset += chunk.size
        }
        jsonWriter.endArray()

        // 6. Archived Articles (Paged to prevent any OOM)
        jsonWriter.name("archivedArticles")
        jsonWriter.beginArray()
        val totalArchived = feedDao.countAllArchivedArticles()
        val archivedPageSize = 500
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
    }

    suspend fun exportPreferencesOnly(context: Context, outputStream: OutputStream) = withContext(ioDispatcher) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
        val prefJson = context.fromDataStoreToJSONString()
        writer.write(prefJson)
        writer.flush()
    }

    suspend fun importBackup(context: Context, inputStream: InputStream): Result<BackupImportResult> = withContext(ioDispatcher) {
        runCatching {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 65536)
            val jsonReader = JsonReader(reader).apply {
                isLenient = true
            }

            var accountsCount = 0
            var groupsCount = 0
            var feedsCount = 0
            var articlesCount = 0
            var hasPreferences = false
            var isFullBackup = false

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
                        val prefMapType = object : TypeToken<Map<String, Any?>>() {}.type
                        val preferencesMap: Map<String, Any?> = gson.fromJson(jsonReader, prefMapType)
                        runCatching {
                            val prefJson = gson.toJson(preferencesMap)
                            prefJson.fromJSONStringToDataStore(context)
                        }.onFailure {
                            Timber.w(it, "Failed to restore preferences from backup")
                        }
                    }
                    "accounts" -> {
                        isFullBackup = true
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
                        while (jsonReader.hasNext()) {
                            val art: Article = gson.fromJson(jsonReader, Article::class.java)
                            batch.add(art)
                            if (batch.size >= 200) {
                                articleDao.insertAllArticles(batch)
                                articlesCount += batch.size
                                batch.clear()
                            }
                        }
                        jsonReader.endArray()
                        if (batch.isNotEmpty()) {
                            articleDao.insertAllArticles(batch)
                            articlesCount += batch.size
                            batch.clear()
                        }
                    }
                    "archivedArticles" -> {
                        isFullBackup = true
                        jsonReader.beginArray()
                        val batch = mutableListOf<ArchivedArticle>()
                        while (jsonReader.hasNext()) {
                            val arch: ArchivedArticle = gson.fromJson(jsonReader, ArchivedArticle::class.java)
                            batch.add(arch)
                            if (batch.size >= 500) {
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
                        jsonReader.skipValue()
                    }
                }
            }
            jsonReader.endObject()

            if (isFullBackup) {
                BackupImportResult.Full(
                    accountsCount = accountsCount,
                    groupsCount = groupsCount,
                    feedsCount = feedsCount,
                    articlesCount = articlesCount,
                    hasPreferences = hasPreferences,
                )
            } else {
                BackupImportResult.PreferencesOnly
            }
        }.onFailure {
            Timber.e(it, "Failed to import backup")
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
            }.onFailure {
                Timber.w(it, "Failed to write preferences to DataStore")
            }
            Unit
        }.onFailure {
            Timber.e(it, "Failed to import preferences")
        }
    }
}
