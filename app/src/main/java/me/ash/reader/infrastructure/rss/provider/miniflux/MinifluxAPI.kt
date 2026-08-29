package me.ash.reader.infrastructure.rss.provider.miniflux

import android.content.Context
import me.ash.reader.infrastructure.exception.MinifluxAPIException
import me.ash.reader.infrastructure.rss.provider.ProviderAPI
import me.ash.reader.ui.ext.encodeBase64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.executeAsync
import java.util.concurrent.ConcurrentHashMap

class MinifluxAPI private constructor(
    context: Context,
    private val serverUrl: String,
    private val username: String,
    private val password: String,
    clientCertificateAlias: String? = null,
) : ProviderAPI(context, clientCertificateAlias) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun normalizeUrl(url: String): String {
        var base = url.trim()
        if (base.endsWith("/")) {
            base = base.substring(0, base.length - 1)
        }
        if (base.endsWith("/v1")) {
            base = base.substring(0, base.length - 3)
        }
        return base
    }

    private val baseEndpoint = normalizeUrl(serverUrl)

    private fun Request.Builder.addAuth(): Request.Builder {
        return if (password.isNotBlank() && username.isBlank()) {
            // API Token authentication
            header("X-Auth-Token", password)
        } else {
            // Basic HTTP Auth
            val authValue = "$username:$password".encodeBase64()
            header("Authorization", "Basic $authValue")
        }
    }

    private suspend inline fun <reified T> executeGet(path: String): T {
        val request = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
            .get()
            .build()
        val response = client.newCall(request).executeAsync()
        val bodyStr = response.body.string()
        if (!response.isSuccessful) {
            val errorMsg = try {
                val errorObj = gson.fromJson(bodyStr, Map::class.java)
                errorObj["error_message"] as? String ?: "Miniflux error ${response.code}"
            } catch (e: Exception) {
                "Miniflux HTTP ${response.code}: ${response.message}"
            }
            throw MinifluxAPIException(errorMsg)
        }
        return toDTO<T>(bodyStr)
    }

    private suspend inline fun <reified T> executePost(path: String, bodyJson: String): T {
        val request = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).executeAsync()
        val bodyStr = response.body.string()
        if (!response.isSuccessful) {
            val errorMsg = try {
                val errorObj = gson.fromJson(bodyStr, Map::class.java)
                errorObj["error_message"] as? String ?: "Miniflux error ${response.code}"
            } catch (e: Exception) {
                "Miniflux HTTP ${response.code}: ${response.message}"
            }
            throw MinifluxAPIException(errorMsg)
        }
        return toDTO<T>(bodyStr)
    }

    private suspend fun executePut(path: String, bodyJson: String? = null) {
        val body = (bodyJson ?: "").toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
            .put(body)
            .build()
        val response = client.newCall(request).executeAsync()
        if (!response.isSuccessful) {
            throw MinifluxAPIException("Miniflux PUT failed with code ${response.code}: ${response.message}")
        }
    }

    private suspend fun executeDelete(path: String) {
        val request = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
            .delete()
            .build()
        val response = client.newCall(request).executeAsync()
        if (!response.isSuccessful) {
            throw MinifluxAPIException("Miniflux DELETE failed with code ${response.code}: ${response.message}")
        }
    }

    suspend fun getMe(): MinifluxDTO.User = executeGet("/v1/me")

    suspend fun validCredentials(): Boolean {
        return try {
            getMe().id > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCategories(): List<MinifluxDTO.Category> =
        executeGet("/v1/categories")

    suspend fun createCategory(title: String): MinifluxDTO.Category =
        executePost("/v1/categories", gson.toJson(MinifluxDTO.CategoryCreation(title)))

    suspend fun getFeeds(): List<MinifluxDTO.Feed> =
        executeGet("/v1/feeds")

    suspend fun createFeed(feedUrl: String, categoryId: Long?): MinifluxDTO.FeedCreationResponse =
        executePost("/v1/feeds", gson.toJson(MinifluxDTO.FeedCreation(feedUrl, categoryId)))

    suspend fun updateFeed(feedId: Long, title: String?, categoryId: Long?) {
        executePut("/v1/feeds/$feedId", gson.toJson(MinifluxDTO.FeedUpdate(title, categoryId)))
    }

    suspend fun deleteFeed(feedId: Long) {
        executeDelete("/v1/feeds/$feedId")
    }

    suspend fun getEntries(
        status: String? = null,
        starred: Boolean? = null,
        limit: Int = 100,
        afterEntryId: Long? = null,
        feedId: Long? = null,
        categoryId: Long? = null,
    ): MinifluxDTO.EntriesResponse {
        val params = mutableListOf<String>()
        status?.let { params.add("status=$it") }
        starred?.let { params.add("starred=$it") }
        params.add("limit=$limit")
        params.add("order=published_at")
        params.add("direction=desc")
        afterEntryId?.let { params.add("after_entry_id=$it") }
        feedId?.let { params.add("feed_id=$it") }
        categoryId?.let { params.add("category_id=$it") }
        val queryString = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        return executeGet("/v1/entries$queryString")
    }

    suspend fun updateEntriesStatus(entryIds: List<Long>, status: String) {
        if (entryIds.isEmpty()) return
        executePut("/v1/entries/status", gson.toJson(MinifluxDTO.EntryStatusUpdate(entryIds, status)))
    }

    suspend fun toggleBookmark(entryId: Long) {
        executePut("/v1/entries/$entryId/bookmark", "")
    }

    companion object {
        private val instances = ConcurrentHashMap<String, MinifluxAPI>()

        fun getInstance(
            context: Context,
            serverUrl: String,
            username: String,
            password: String,
            clientCertificateAlias: String? = null,
        ): MinifluxAPI {
            val key = "$serverUrl:$username:$clientCertificateAlias"
            return instances.getOrPut(key) {
                MinifluxAPI(context, serverUrl, username, password, clientCertificateAlias)
            }
        }

        fun clearInstance() {
            instances.clear()
        }
    }
}
