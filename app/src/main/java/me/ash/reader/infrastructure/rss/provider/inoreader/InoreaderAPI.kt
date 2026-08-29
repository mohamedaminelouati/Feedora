package me.ash.reader.infrastructure.rss.provider.inoreader

import android.content.Context
import me.ash.reader.infrastructure.exception.InoreaderAPIException
import me.ash.reader.infrastructure.rss.provider.ProviderAPI
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.executeAsync
import java.util.concurrent.ConcurrentHashMap

class InoreaderAPI private constructor(
    context: Context,
    private val tokenOrPassword: String,
    private val username: String? = null,
    clientCertificateAlias: String? = null,
) : ProviderAPI(context, clientCertificateAlias) {

    private val baseEndpoint = "https://www.inoreader.com/reader/api/0"

    private fun Request.Builder.addAuth(): Request.Builder {
        val authHeader = if (tokenOrPassword.startsWith("Bearer ") || tokenOrPassword.startsWith("GoogleLogin ")) {
            tokenOrPassword
        } else {
            "Bearer $tokenOrPassword"
        }
        return header("Authorization", authHeader)
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
            val errorMsg = if (bodyStr.isNotBlank()) bodyStr.trim() else "Inoreader HTTP ${response.code}: ${response.message}"
            throw InoreaderAPIException(errorMsg)
        }
        return toDTO<T>(bodyStr)
    }

    private suspend fun executePostForm(path: String, formBody: FormBody): String {
        val request = Request.Builder()
            .url("$baseEndpoint$path")
            .addAuth()
            .post(formBody)
            .build()
        val response = client.newCall(request).executeAsync()
        val bodyStr = response.body.string()
        if (!response.isSuccessful) {
            val errorMsg = if (bodyStr.isNotBlank()) bodyStr.trim() else "Inoreader HTTP ${response.code}: ${response.message}"
            throw InoreaderAPIException(errorMsg)
        }
        return bodyStr
    }

    suspend fun getUserInfo(): InoreaderDTO.UserInfo =
        executeGet("/user-info")

    suspend fun validCredentials(): Boolean {
        return try {
            getUserInfo().userId != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSubscriptions(): InoreaderDTO.SubscriptionsResponse =
        executeGet("/subscription/list")

    suspend fun getStreamContents(
        streamId: String = "user/-/state/com.google/reading-list",
        limit: Int = 100,
        continuation: String? = null,
    ): InoreaderDTO.StreamResponse {
        val query = buildString {
            append("?n=$limit")
            if (continuation != null) append("&c=$continuation")
        }
        return executeGet("/stream/contents/$streamId$query")
    }

    suspend fun editTag(
        itemId: String,
        addTag: String? = null,
        removeTag: String? = null,
    ) {
        val formBuilder = FormBody.Builder().add("i", itemId)
        addTag?.let { formBuilder.add("a", it) }
        removeTag?.let { formBuilder.add("r", it) }
        executePostForm("/edit-tag", formBuilder.build())
    }

    suspend fun quickAddSubscription(feedUrl: String): InoreaderDTO.QuickAddResponse {
        val formBody = FormBody.Builder().add("quickadd", feedUrl).build()
        val bodyStr = executePostForm("/subscription/quickadd", formBody)
        return toDTO(bodyStr)
    }

    suspend fun unsubscribe(streamId: String) {
        val formBody = FormBody.Builder()
            .add("ac", "unsubscribe")
            .add("s", streamId)
            .build()
        executePostForm("/subscription/edit", formBody)
    }

    companion object {
        private val instances = ConcurrentHashMap<String, InoreaderAPI>()

        fun getInstance(
            context: Context,
            tokenOrPassword: String,
            username: String? = null,
            clientCertificateAlias: String? = null,
        ): InoreaderAPI {
            val key = "$tokenOrPassword:$username:$clientCertificateAlias"
            return instances.getOrPut(key) {
                InoreaderAPI(context, tokenOrPassword, username, clientCertificateAlias)
            }
        }

        fun clearInstance() {
            instances.clear()
        }
    }
}
