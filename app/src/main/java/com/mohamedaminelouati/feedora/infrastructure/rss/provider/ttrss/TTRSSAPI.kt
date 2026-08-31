package com.mohamedaminelouati.feedora.infrastructure.rss.provider.ttrss

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.mohamedaminelouati.feedora.infrastructure.exception.TTRSSAPIException
import com.mohamedaminelouati.feedora.infrastructure.rss.provider.ProviderAPI
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.executeAsync
import java.util.concurrent.ConcurrentHashMap

class TTRSSAPI private constructor(
    context: Context,
    private val serverUrl: String,
    private val username: String,
    private val password: String,
    clientCertificateAlias: String? = null,
) : ProviderAPI(context, clientCertificateAlias) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private var sessionId: String? = null

    private fun normalizeUrl(url: String): String {
        var base = url.trim()
        if (!base.endsWith("/api/") && !base.endsWith("/api")) {
            if (!base.endsWith("/")) {
                base += "/"
            }
            base += "api/"
        }
        if (!base.endsWith("/")) {
            base += "/"
        }
        return base
    }

    private val apiEndpoint = normalizeUrl(serverUrl)

    private suspend fun <T> executeOp(payload: Map<String, Any?>, typeToken: java.lang.reflect.Type): T {
        val finalPayload = payload.toMutableMap()
        if (!finalPayload.containsKey("sid") && sessionId != null) {
            finalPayload["sid"] = sessionId
        }
        val bodyJson = gson.toJson(finalPayload)
        val request = Request.Builder()
            .url(apiEndpoint)
            .post(bodyJson.toRequestBody(jsonMediaType))
            .build()
        val response = client.newCall(request).executeAsync()
        if (!response.isSuccessful) {
            throw TTRSSAPIException("TTRSS HTTP error: ${response.code}")
        }
        val bodyStr = response.body.string()
        val result: TTRSSDTO.Response<T> = gson.fromJson(bodyStr, typeToken)
        if (result.status != 0) {
            throw TTRSSAPIException("TTRSS error response: $bodyStr")
        }
        return result.content ?: throw TTRSSAPIException("Empty response content from TTRSS")
    }

    suspend fun login(): String {
        val payload = mapOf(
            "op" to "login",
            "user" to username,
            "password" to password
        )
        val type = object : TypeToken<TTRSSDTO.Response<TTRSSDTO.LoginResponse>>() {}.type
        val resp: TTRSSDTO.LoginResponse = executeOp(payload, type)
        sessionId = resp.sessionId
        return resp.sessionId
    }

    private suspend fun ensureSession() {
        if (sessionId == null) {
            login()
        }
    }

    suspend fun validCredentials(): Boolean {
        return try {
            login().isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCategories(): List<TTRSSDTO.Category> {
        ensureSession()
        val payload = mapOf("op" to "getCategories", "sid" to sessionId)
        val type = object : TypeToken<TTRSSDTO.Response<List<TTRSSDTO.Category>>>() {}.type
        return executeOp(payload, type)
    }

    suspend fun getFeeds(catId: Long = -4): List<TTRSSDTO.Feed> {
        ensureSession()
        val payload = mapOf(
            "op" to "getFeeds",
            "cat_id" to catId,
            "sid" to sessionId
        )
        val type = object : TypeToken<TTRSSDTO.Response<List<TTRSSDTO.Feed>>>() {}.type
        return executeOp(payload, type)
    }

    suspend fun getHeadlines(
        feedId: Long = -4,
        limit: Int = 100,
        viewMode: String = "all_articles"
    ): List<TTRSSDTO.Headline> {
        ensureSession()
        val payload = mapOf(
            "op" to "getHeadlines",
            "feed_id" to feedId,
            "limit" to limit,
            "view_mode" to viewMode,
            "show_content" to true,
            "sid" to sessionId
        )
        val type = object : TypeToken<TTRSSDTO.Response<List<TTRSSDTO.Headline>>>() {}.type
        return executeOp(payload, type)
    }

    suspend fun updateArticle(articleId: Long, mode: Int, field: Int) {
        ensureSession()
        val payload = mapOf(
            "op" to "updateArticle",
            "article_ids" to articleId.toString(),
            "mode" to mode,
            "field" to field,
            "sid" to sessionId
        )
        val type = object : TypeToken<TTRSSDTO.Response<Map<String, Any>>>() {}.type
        executeOp<Map<String, Any>>(payload, type)
    }

    suspend fun subscribeToFeed(feedUrl: String, categoryId: Long? = null) {
        ensureSession()
        val payload = mutableMapOf<String, Any?>(
            "op" to "subscribeToFeed",
            "feed_url" to feedUrl,
            "sid" to sessionId
        )
        categoryId?.let { payload["category_id"] = it }
        val type = object : TypeToken<TTRSSDTO.Response<Map<String, Any>>>() {}.type
        executeOp<Map<String, Any>>(payload, type)
    }

    suspend fun unsubscribeFeed(feedId: Long) {
        ensureSession()
        val payload = mapOf(
            "op" to "unsubscribeFeed",
            "feed_id" to feedId,
            "sid" to sessionId
        )
        val type = object : TypeToken<TTRSSDTO.Response<Map<String, Any>>>() {}.type
        executeOp<Map<String, Any>>(payload, type)
    }

    companion object {
        private val instances = ConcurrentHashMap<String, TTRSSAPI>()

        fun getInstance(
            context: Context,
            serverUrl: String,
            username: String,
            password: String,
            clientCertificateAlias: String? = null,
        ): TTRSSAPI {
            val key = "$serverUrl:$username:$clientCertificateAlias"
            return instances.getOrPut(key) {
                TTRSSAPI(context, serverUrl, username, password, clientCertificateAlias)
            }
        }

        fun clearInstance() {
            instances.clear()
        }
    }
}
