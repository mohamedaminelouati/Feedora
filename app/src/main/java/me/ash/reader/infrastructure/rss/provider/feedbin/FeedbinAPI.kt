package me.ash.reader.infrastructure.rss.provider.feedbin

import android.content.Context
import com.google.gson.reflect.TypeToken
import me.ash.reader.infrastructure.exception.FeedbinAPIException
import me.ash.reader.infrastructure.rss.provider.ProviderAPI
import me.ash.reader.ui.ext.encodeBase64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.executeAsync
import java.util.concurrent.ConcurrentHashMap

class FeedbinAPI private constructor(
    context: Context,
    private val username: String,
    private val password: String,
    clientCertificateAlias: String? = null,
) : ProviderAPI(context, clientCertificateAlias) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val baseEndpoint = "https://api.feedbin.com"

    private fun Request.Builder.addAuth(): Request.Builder {
        val authValue = "$username:$password".encodeBase64()
        return header("Authorization", "Basic $authValue")
    }

    private suspend fun executeRawGet(path: String): String {
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
                errorObj["message"] as? String ?: "Feedbin error ${response.code}"
            } catch (e: Exception) {
                "Feedbin HTTP ${response.code}: ${response.message}"
            }
            throw FeedbinAPIException(errorMsg)
        }
        return bodyStr
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
        val bodyStr = response.body.string()
        if (!response.isSuccessful) {
            val errorMsg = try {
                val errorObj = gson.fromJson(bodyStr, Map::class.java)
                errorObj["message"] as? String ?: "Feedbin error ${response.code}"
            } catch (e: Exception) {
                "Feedbin HTTP ${response.code}: ${response.message}"
            }
            throw FeedbinAPIException(errorMsg)
        }
        return bodyStr
    }

    private suspend fun executeDelete(path: String, bodyJson: String? = null) {
        val reqBuilder = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
        if (bodyJson != null) {
            reqBuilder.delete(bodyJson.toRequestBody(jsonMediaType))
        } else {
            reqBuilder.delete()
        }
        val response = client.newCall(reqBuilder.build()).executeAsync()
        if (!response.isSuccessful) {
            throw FeedbinAPIException("Feedbin DELETE failed with code ${response.code}")
        }
    }

    suspend fun validCredentials(): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseEndpoint/v2/authentication.json")
                .addAuth()
                .get()
                .build()
            val response = client.newCall(request).executeAsync()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSubscriptions(): List<FeedbinDTO.Subscription> {
        val type = object : TypeToken<List<FeedbinDTO.Subscription>>() {}.type
        val bodyStr = executeRawGet("/v2/subscriptions.json")
        return gson.fromJson(bodyStr, type)
    }

    suspend fun getTaggings(): List<FeedbinDTO.Tagging> {
        val type = object : TypeToken<List<FeedbinDTO.Tagging>>() {}.type
        val bodyStr = executeRawGet("/v2/taggings.json")
        return gson.fromJson(bodyStr, type)
    }

    suspend fun getEntries(page: Int = 1, perPage: Int = 100): List<FeedbinDTO.Entry> {
        val type = object : TypeToken<List<FeedbinDTO.Entry>>() {}.type
        val bodyStr = executeRawGet("/v2/entries.json?page=$page&per_page=$perPage")
        return gson.fromJson(bodyStr, type)
    }

    suspend fun getUnreadEntryIds(): List<Long> {
        val type = object : TypeToken<List<Long>>() {}.type
        val bodyStr = executeRawGet("/v2/unread_entries.json")
        return gson.fromJson(bodyStr, type)
    }

    suspend fun getStarredEntryIds(): List<Long> {
        val type = object : TypeToken<List<Long>>() {}.type
        val bodyStr = executeRawGet("/v2/starred_entries.json")
        return gson.fromJson(bodyStr, type)
    }

    suspend fun markAsRead(entryIds: List<Long>) {
        if (entryIds.isEmpty()) return
        executeDelete("/v2/unread_entries.json", gson.toJson(FeedbinDTO.UnreadEntriesUpdate(entryIds)))
    }

    suspend fun markAsUnread(entryIds: List<Long>) {
        if (entryIds.isEmpty()) return
        executePost("/v2/unread_entries.json", gson.toJson(FeedbinDTO.UnreadEntriesUpdate(entryIds)))
    }

    suspend fun markAsStarred(entryIds: List<Long>) {
        if (entryIds.isEmpty()) return
        executePost("/v2/starred_entries.json", gson.toJson(FeedbinDTO.StarredEntriesUpdate(entryIds)))
    }

    suspend fun unmarkAsStarred(entryIds: List<Long>) {
        if (entryIds.isEmpty()) return
        executeDelete("/v2/starred_entries.json", gson.toJson(FeedbinDTO.StarredEntriesUpdate(entryIds)))
    }

    suspend fun createSubscription(feedUrl: String): FeedbinDTO.Subscription {
        val bodyStr = executePost("/v2/subscriptions.json", gson.toJson(FeedbinDTO.CreateSubscription(feedUrl)))
        return toDTO(bodyStr)
    }

    suspend fun deleteSubscription(subscriptionId: Long) {
        executeDelete("/v2/subscriptions/$subscriptionId.json")
    }

    companion object {
        private val instances = ConcurrentHashMap<String, FeedbinAPI>()

        fun getInstance(
            context: Context,
            username: String,
            password: String,
            clientCertificateAlias: String? = null,
        ): FeedbinAPI {
            val key = "$username:$clientCertificateAlias"
            return instances.getOrPut(key) {
                FeedbinAPI(context, username, password, clientCertificateAlias)
            }
        }

        fun clearInstance() {
            instances.clear()
        }
    }
}
