package me.ash.reader.domain.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
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

data class FullBackupData(
    val version: Int = 1,
    val appVersion: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val preferences: Map<String, Any?>? = null,
    val accounts: List<Account>? = null,
    val groups: List<Group>? = null,
    val feeds: List<Feed>? = null,
    val articles: List<Article>? = null,
    val archivedArticles: List<ArchivedArticle>? = null,
)

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

    suspend fun exportFullBackup(context: Context): ByteArray = withContext(ioDispatcher) {
        val prefJson = context.fromDataStoreToJSONString()
        val prefMapType = object : TypeToken<Map<String, Any?>>() {}.type
        val preferencesMap: Map<String, Any?> = gson.fromJson(prefJson, prefMapType)

        val accounts = accountDao.queryAll()
        val groups = groupDao.queryAllGroups()
        val feeds = feedDao.queryAllFeeds()
        val articles = articleDao.queryAllArticles()
        val archivedArticles = feedDao.queryAllArchivedArticles()

        val fullBackup =
            FullBackupData(
                version = 1,
                appVersion = context.getCurrentVersion().toString(),
                timestamp = System.currentTimeMillis(),
                preferences = preferencesMap,
                accounts = accounts,
                groups = groups,
                feeds = feeds,
                articles = articles,
                archivedArticles = archivedArticles,
            )

        gson.toJson(fullBackup).toByteArray(Charsets.UTF_8)
    }

    suspend fun exportPreferencesOnly(context: Context): ByteArray = withContext(ioDispatcher) {
        context.fromDataStoreToJSONString().toByteArray(Charsets.UTF_8)
    }

    suspend fun importBackup(context: Context, byteArray: ByteArray): Result<BackupImportResult> = withContext(ioDispatcher) {
        runCatching {
            val jsonString = String(byteArray, Charsets.UTF_8)
            val jsonElement = JsonParser.parseString(jsonString)

            if (!jsonElement.isJsonObject) {
                throw IllegalArgumentException("Invalid JSON format")
            }

            val jsonObject = jsonElement.asJsonObject

            val isFullBackup = jsonObject.has("accounts") || jsonObject.has("feeds") ||
                jsonObject.has("groups") || jsonObject.has("articles")

            if (isFullBackup) {
                val fullBackup = gson.fromJson(jsonObject, FullBackupData::class.java)

                if (fullBackup.preferences != null) {
                    runCatching {
                        val prefJson = gson.toJson(fullBackup.preferences)
                        prefJson.fromJSONStringToDataStore(context)
                    }.onFailure {
                        Timber.w(it, "Failed to restore preferences from full backup")
                    }
                }

                fullBackup.accounts?.let { if (it.isNotEmpty()) accountDao.insertAllAccounts(it) }
                fullBackup.groups?.let { if (it.isNotEmpty()) groupDao.insertAllGroups(it) }
                fullBackup.feeds?.let { if (it.isNotEmpty()) feedDao.insertAllFeeds(it) }
                fullBackup.articles?.let { if (it.isNotEmpty()) articleDao.insertAllArticles(it) }
                fullBackup.archivedArticles?.let { if (it.isNotEmpty()) feedDao.insertAllArchivedArticles(it) }

                BackupImportResult.Full(
                    accountsCount = fullBackup.accounts?.size ?: 0,
                    groupsCount = fullBackup.groups?.size ?: 0,
                    feedsCount = fullBackup.feeds?.size ?: 0,
                    articlesCount = fullBackup.articles?.size ?: 0,
                    hasPreferences = fullBackup.preferences != null,
                )
            } else {
                jsonString.fromJSONStringToDataStore(context)
                BackupImportResult.PreferencesOnly
            }
        }.onFailure {
            Timber.e(it, "Failed to import backup")
        }
    }
}
