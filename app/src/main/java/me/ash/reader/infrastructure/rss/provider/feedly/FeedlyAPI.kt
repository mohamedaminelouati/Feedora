package me.ash.reader.infrastructure.rss.provider.feedly

import android.content.Context
import com.google.gson.reflect.TypeToken
import me.ash.reader.infrastructure.exception.FeedlyAPIException
import me.ash.reader.infrastructure.rss.provider.ProviderAPI
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.executeAsync
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

class FeedlyAPI private constructor(
    context: Context,
    private val accessToken: String,
    clientCertificateAlias: String? = null,
) : ProviderAPI(context, clientCertificateAlias) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseEndpoint = "https://cloud.feedly.com/v3"

    private fun Request.Builder.addAuth(): Request.Builder {
        val authHeader = if (accessToken.startsWith("Bearer ") || accessToken.startsWith("OAuth ")) {
            accessToken
        } else {
            "Bearer $accessToken"
        }
        return header("Authorization", authHeader)
    }

    private suspend fun executeRawGet(path: String): String {
        val request = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
            .get()
            .build()
        val response = client.newCall(request).executeAsync()
        if (!response.isSuccessful) {
            throw FeedlyAPIException("Feedly GET failed with code ${response.code}")
        }
        return response.body.string()
    }

    private suspend inline fun <reified T> executeGet(path: String): T {
        val bodyStr = executeRawGet(path)
        return toDTO<T>(bodyStr)
    }

    private suspend fun executePost(path: String, bodyJson: String): String {
        val request = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).executeAsync()
        if (!response.isSuccessful) {
            throw FeedlyAPIException("Feedly POST failed with code ${response.code}")
        }
        return response.body.string()
    }

    private suspend fun executeDelete(path: String) {
        val request = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
            .delete()
            .build()
        val response = client.newCall(request).executeAsync()
        if (!response.isSuccessful) {
            throw FeedlyAPIException("Feedly DELETE failed with code ${response.code}")
        }
    }

    suspend fun getProfile(): FeedlyDTO.Profile = executeGet("/profile")

    suspend fun validCredentials(): Boolean {
        return try {
            getProfile().id.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSubscriptions(): List<FeedlyDTO.Subscription> {
        val type = object : TypeToken<List<FeedlyDTO.Subscription>>() {}.type
        val bodyStr = executeRawGet("/subscriptions")
        return gson.fromJson(bodyStr, type)
    }

    suspend fun getCategories(): List<FeedlyDTO.Category> {
        val type = object : TypeToken<List<FeedlyDTO.Category>>() {}.type
        val bodyStr = executeRawGet("/categories")
        return gson.fromJson(bodyStr, type)
    }

    suspend fun getStreamContents(
        streamId: String,
        count: Int = 100,
        unreadOnly: Boolean = false,
        continuation: String? = null,
    ): FeedlyDTO.StreamContents {
        val encodedStreamId = URLEncoder.encode(streamId, "UTF-8")
        val query = buildString {
            append("?streamId=$encodedStreamId")
            append("&count=$count")
            if (unreadOnly) append("&unreadOnly=true")
            if (continuation != null) append("&continuation=$continuation")
        }
        return executeGet("/streams/contents$query")
    }

    suspend fun updateMarkers(action: String, entryIds: List<String>) {
        if (entryIds.isEmpty()) return
        val payload = FeedlyDTO.MarkersUpdate(
            action = action,
            type = "entries",
            entryIds = entryIds,
        )
        executePost("/markers", gson.toJson(payload))
    }

    suspend fun subscribe(feedId: String, title: String, categories: List<FeedlyDTO.Category>? = null) {
        val payload = FeedlyDTO.SubscribeRequest(
            id = feedId,
            title = title,
            categories = categories,
        )
        executePost("/subscriptions", gson.toJson(payload))
    }

    suspend fun unsubscribe(feedId: String) {
        val encodedId = URLEncoder.encode(feedId, "UTF-8")
        executeDelete("/subscriptions/$encodedId")
    }

    companion object {
        private val instances = ConcurrentHashMap<String, FeedlyAPI>()

        fun getInstance(
            context: Context,
            accessToken: String,
            clientCertificateAlias: String? = null,
        ): FeedlyAPI {
            val key = "$accessToken:$clientCertificateAlias"
            return instances.getOrPut(key) {
                FeedlyAPI(context, accessToken, clientCertificateAlias)
            }
        }

        fun clearInstance() {
            instances.clear()
        }
    }
}
