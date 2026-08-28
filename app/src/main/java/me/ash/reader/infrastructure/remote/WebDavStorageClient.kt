package me.ash.reader.infrastructure.remote

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class WebDavStorageClient(
    private val config: RemoteServerConfig,
    baseClient: OkHttpClient = OkHttpClient(),
) : RemoteStorageClient {

    private val client: OkHttpClient = buildClient(baseClient, config)
    private val baseUrl: String = buildBaseUrl(config)

    private fun buildBaseUrl(cfg: RemoteServerConfig): String {
        var host = cfg.host.trim()
        val scheme = if (host.startsWith("http://", ignoreCase = true)) {
            host = host.substring(7)
            "http"
        } else if (host.startsWith("https://", ignoreCase = true)) {
            host = host.substring(8)
            "https"
        } else {
            if (cfg.port == 80) "http" else "https"
        }

        val hostAndPort = if (host.contains(":") || (cfg.port == 80 && scheme == "http") || (cfg.port == 443 && scheme == "https")) {
            host.trimEnd('/')
        } else {
            "${host.trimEnd('/')}:${cfg.port}"
        }

        var path = cfg.remotePath.trim()
        if (!path.startsWith("/")) path = "/$path"
        if (!path.endsWith("/")) path = "$path/"

        return "$scheme://$hostAndPort$path"
    }

    private fun buildClient(base: OkHttpClient, cfg: RemoteServerConfig): OkHttpClient {
        val builder = base.newBuilder()
        if (cfg.trustInsecureSsl) {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        }
        return builder.build()
    }

    private fun createRequestBuilder(url: String): Request.Builder {
        val builder = Request.Builder().url(url)
        if (config.username.isNotBlank()) {
            builder.header("Authorization", Credentials.basic(config.username, config.password))
        }
        return builder
    }

    private suspend fun ensureDirectoryExists(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val segments = baseUrl.removePrefix("http://").removePrefix("https://")
                .substringAfter("/")
                .split("/")
                .filter { it.isNotBlank() }

            val rootScheme = if (baseUrl.startsWith("https://")) "https://" else "http://"
            val hostPart = baseUrl.removePrefix(rootScheme).substringBefore("/")

            var currentPath = "$rootScheme$hostPart"
            for (seg in segments) {
                currentPath = "$currentPath/$seg"
                val checkReq = createRequestBuilder(currentPath)
                    .method("PROPFIND", null)
                    .header("Depth", "0")
                    .build()
                val checkResp = client.newCall(checkReq).execute()
                val code = checkResp.code
                checkResp.close()

                if (code == 404) {
                    val mkcolReq = createRequestBuilder(currentPath)
                        .method("MKCOL", null)
                        .build()
                    val mkcolResp = client.newCall(mkcolReq).execute()
                    val mkcolCode = mkcolResp.code
                    mkcolResp.close()
                    if (mkcolCode !in 200..299 && mkcolCode != 405) {
                        throw IllegalStateException("Failed to create WebDAV directory: $currentPath (HTTP $mkcolCode)")
                    }
                }
            }
        }
    }

    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureDirectoryExists().getOrThrow()
            val request = createRequestBuilder(baseUrl)
                .method("PROPFIND", null)
                .header("Depth", "0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 207) {
                    throw IllegalStateException("WebDAV connection failed (HTTP ${response.code}: ${response.message})")
                }
            }
        }
    }

    override suspend fun upload(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        onProgress: (Float) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureDirectoryExists().getOrThrow()
            val fileUrl = "$baseUrl$fileName"

            val requestBody = object : RequestBody() {
                override fun contentType() = "application/json; charset=utf-8".toMediaTypeOrNull()
                override fun contentLength() = contentLength

                override fun writeTo(sink: BufferedSink) {
                    val buffer = ByteArray(65536)
                    var uploaded = 0L
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                        uploaded += read
                        if (contentLength > 0) {
                            onProgress((uploaded.toFloat() / contentLength).coerceIn(0f, 1f))
                        }
                    }
                }
            }

            val request = createRequestBuilder(fileUrl)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("WebDAV upload failed (HTTP ${response.code}: ${response.message})")
                }
            }
            onProgress(1.0f)
        }
    }

    override suspend fun download(
        fileName: String,
        outputStream: OutputStream,
        onProgress: (Float) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileUrl = "$baseUrl$fileName"
            val request = createRequestBuilder(fileUrl).get().build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("WebDAV download failed (HTTP ${response.code}: ${response.message})")
                }
                val body = response.body ?: throw IllegalStateException("Empty response body")
                val totalLength = body.contentLength()
                val source = body.byteStream()
                val buffer = ByteArray(65536)
                var downloaded = 0L
                var read: Int
                while (source.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    downloaded += read
                    if (totalLength > 0) {
                        onProgress((downloaded.toFloat() / totalLength).coerceIn(0f, 1f))
                    }
                }
                outputStream.flush()
            }
            onProgress(1.0f)
        }
    }

    override suspend fun listBackups(): Result<List<RemoteBackupFile>> = withContext(Dispatchers.IO) {
        runCatching {
            ensureDirectoryExists().getOrThrow()
            val request = createRequestBuilder(baseUrl)
                .method("PROPFIND", null)
                .header("Depth", "1")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 207) {
                    throw IllegalStateException("WebDAV list failed (HTTP ${response.code}: ${response.message})")
                }
                val responseXml = response.body?.string().orEmpty()
                parseWebDavPropfind(responseXml)
            }
        }
    }

    override suspend fun delete(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val fileUrl = "$baseUrl$fileName"
            val request = createRequestBuilder(fileUrl).delete().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    throw IllegalStateException("WebDAV delete failed (HTTP ${response.code}: ${response.message})")
                }
            }
        }
    }

    private fun parseWebDavPropfind(xml: String): List<RemoteBackupFile> {
        if (xml.isBlank()) return emptyList()
        val result = mutableListOf<RemoteBackupFile>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))

            val responses: NodeList = doc.getElementsByTagNameNS("*", "response")
            val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }

            for (i in 0 until responses.length) {
                val elem = responses.item(i) as? Element ?: continue
                val href = elem.getElementsByTagNameNS("*", "href").item(0)?.textContent?.trim().orEmpty()
                val name = href.trimEnd('/').substringAfterLast('/')

                if (name.isBlank() || href.endsWith("/") && baseUrl.endsWith(href)) {
                    continue // Skip root directory self-reference
                }

                val isCollection = elem.getElementsByTagNameNS("*", "collection").length > 0
                if (isCollection) continue

                val sizeStr = elem.getElementsByTagNameNS("*", "getcontentlength").item(0)?.textContent?.trim()
                val size = sizeStr?.toLongOrNull() ?: 0L

                val modStr = elem.getElementsByTagNameNS("*", "getlastmodified").item(0)?.textContent?.trim()
                val lastModified = if (!modStr.isNullOrBlank()) {
                    runCatching { dateFormat.parse(modStr)?.time }.getOrNull() ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }

                if (name.endsWith(".json", ignoreCase = true)) {
                    result.add(
                        RemoteBackupFile(
                            name = name,
                            size = size,
                            lastModified = lastModified,
                            isDirectory = false,
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return result.sortedByDescending { it.lastModified }
    }
}
