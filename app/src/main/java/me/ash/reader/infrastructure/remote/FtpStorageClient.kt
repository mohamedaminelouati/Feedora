package me.ash.reader.infrastructure.remote

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient

class FtpStorageClient(
    private val config: RemoteServerConfig,
) : RemoteStorageClient {

    private fun createClient(): FTPClient {
        val client = if (config.protocol == RemoteStorageProtocol.FTPS) {
            val isImplicit = config.port == 990
            val sslContext = if (config.trustInsecureSsl) {
                val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    }
                )
                val ctx = SSLContext.getInstance("TLS")
                ctx.init(null, trustAllCerts, SecureRandom())
                ctx
            } else {
                null
            }
            val ftps = if (sslContext != null) FTPSClient(isImplicit, sslContext) else FTPSClient(isImplicit)
            ftps
        } else {
            FTPClient()
        }

        client.connectTimeout = 15000
        client.controlEncoding = "UTF-8"
        client.autodetectUTF8 = true
        return client
    }

    private fun connectAndLogin(client: FTPClient) {
        val cleanHost = config.host.removePrefix("ftp://").removePrefix("ftps://").trimEnd('/')
        client.connect(cleanHost, config.port)
        val reply = client.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect()
            throw IllegalStateException("FTP server refused connection (code: $reply)")
        }

        val user = if (config.username.isBlank()) "anonymous" else config.username
        val pass = if (config.password.isBlank()) "anonymous@" else config.password

        if (!client.login(user, pass)) {
            client.disconnect()
            throw IllegalStateException("FTP authentication failed for user: $user")
        }

        if (client is FTPSClient) {
            client.execPBSZ(0)
            client.execPROT("P")
        }

        client.enterLocalPassiveMode()
        client.setFileType(FTP.BINARY_FILE_TYPE)
    }

    private fun ensureRemoteDirectory(client: FTPClient, path: String) {
        val normalized = path.replace("\\", "/").trim('/')
        if (normalized.isBlank()) return

        val parts = normalized.split("/").filter { it.isNotBlank() }
        client.changeWorkingDirectory("/")
        for (part in parts) {
            if (!client.changeWorkingDirectory(part)) {
                if (client.makeDirectory(part)) {
                    client.changeWorkingDirectory(part)
                } else {
                    throw IllegalStateException("Failed to create remote FTP directory: $part")
                }
            }
        }
    }

    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = createClient()
            try {
                connectAndLogin(client)
                ensureRemoteDirectory(client, config.remotePath)
            } finally {
                runCatching {
                    if (client.isConnected) {
                        client.logout()
                        client.disconnect()
                    }
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
            val client = createClient()
            try {
                connectAndLogin(client)
                ensureRemoteDirectory(client, config.remotePath)

                val outputStream = client.storeFileStream(fileName)
                    ?: throw IllegalStateException("Could not open FTP output stream for $fileName: ${client.replyString}")

                val buffer = ByteArray(65536)
                var uploaded = 0L
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    uploaded += read
                    if (contentLength > 0) {
                        onProgress((uploaded.toFloat() / contentLength).coerceIn(0f, 1f))
                    }
                }
                outputStream.flush()
                outputStream.close()

                if (!client.completePendingCommand()) {
                    throw IllegalStateException("Failed to complete FTP upload: ${client.replyString}")
                }
                onProgress(1.0f)
            } finally {
                runCatching {
                    if (client.isConnected) {
                        client.logout()
                        client.disconnect()
                    }
                }
            }
        }
    }

    override suspend fun download(
        fileName: String,
        outputStream: OutputStream,
        onProgress: (Float) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = createClient()
            try {
                connectAndLogin(client)
                ensureRemoteDirectory(client, config.remotePath)

                val inputStream = client.retrieveFileStream(fileName)
                    ?: throw IllegalStateException("Could not open FTP input stream for $fileName: ${client.replyString}")

                val buffer = ByteArray(65536)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
                inputStream.close()

                if (!client.completePendingCommand()) {
                    throw IllegalStateException("Failed to complete FTP download: ${client.replyString}")
                }
                onProgress(1.0f)
            } finally {
                runCatching {
                    if (client.isConnected) {
                        client.logout()
                        client.disconnect()
                    }
                }
            }
        }
    }

    override suspend fun listBackups(): Result<List<RemoteBackupFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val client = createClient()
            try {
                connectAndLogin(client)
                ensureRemoteDirectory(client, config.remotePath)

                val files = client.listFiles() ?: emptyArray()
                files.filter { it.isFile && it.name.endsWith(".json", ignoreCase = true) }
                    .map {
                        RemoteBackupFile(
                            name = it.name,
                            size = it.size,
                            lastModified = it.timestamp?.timeInMillis ?: System.currentTimeMillis(),
                            isDirectory = false,
                        )
                    }
                    .sortedByDescending { it.lastModified }
            } finally {
                runCatching {
                    if (client.isConnected) {
                        client.logout()
                        client.disconnect()
                    }
                }
            }
        }
    }

    override suspend fun delete(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val client = createClient()
            try {
                connectAndLogin(client)
                ensureRemoteDirectory(client, config.remotePath)
                if (!client.deleteFile(fileName)) {
                    throw IllegalStateException("Failed to delete FTP file $fileName: ${client.replyString}")
                }
            } finally {
                runCatching {
                    if (client.isConnected) {
                        client.logout()
                        client.disconnect()
                    }
                }
            }
        }
    }
}
