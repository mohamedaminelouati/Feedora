package com.mohamedaminelouati.feedora.infrastructure.remote

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpProgressMonitor
import java.io.InputStream
import java.io.OutputStream
import java.util.Vector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SftpStorageClient(
    private val config: RemoteServerConfig,
) : RemoteStorageClient {

    private fun openSession(): Session {
        val jsch = JSch()
        val cleanHost = config.host.removePrefix("sftp://").removePrefix("ssh://").trimEnd('/')
        val user = if (config.username.isBlank()) "root" else config.username
        val session = jsch.getSession(user, cleanHost, config.port)
        session.setPassword(config.password)
        session.setConfig("StrictHostKeyChecking", "no")
        session.timeout = 15000
        session.connect()
        return session
    }

    private fun openSftpChannel(session: Session): ChannelSftp {
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(15000)
        return channel
    }

    private fun ensureRemoteDirectory(sftp: ChannelSftp, path: String) {
        val normalized = path.replace("\\", "/").trim('/')
        if (normalized.isBlank()) return

        val parts = normalized.split("/").filter { it.isNotBlank() }
        sftp.cd("/")
        for (part in parts) {
            try {
                sftp.cd(part)
            } catch (_: Exception) {
                sftp.mkdir(part)
                sftp.cd(part)
            }
        }
    }

    override suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession()
            try {
                val sftp = openSftpChannel(session)
                try {
                    ensureRemoteDirectory(sftp, config.remotePath)
                } finally {
                    sftp.disconnect()
                }
            } finally {
                session.disconnect()
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
            val session = openSession()
            try {
                val sftp = openSftpChannel(session)
                try {
                    ensureRemoteDirectory(sftp, config.remotePath)
                    val monitor = object : SftpProgressMonitor {
                        private var count = 0L
                        override fun init(op: Int, src: String?, dest: String?, max: Long) {
                            count = 0L
                        }

                        override fun count(bytes: Long): Boolean {
                            count += bytes
                            if (contentLength > 0) {
                                onProgress((count.toFloat() / contentLength).coerceIn(0f, 1f))
                            }
                            return true
                        }

                        override fun end() {
                            onProgress(1.0f)
                        }
                    }
                    sftp.put(inputStream, fileName, monitor, ChannelSftp.OVERWRITE)
                } finally {
                    sftp.disconnect()
                }
            } finally {
                session.disconnect()
            }
        }
    }

    override suspend fun download(
        fileName: String,
        outputStream: OutputStream,
        onProgress: (Float) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession()
            try {
                val sftp = openSftpChannel(session)
                try {
                    ensureRemoteDirectory(sftp, config.remotePath)
                    sftp.get(fileName, outputStream)
                    outputStream.flush()
                    onProgress(1.0f)
                } finally {
                    sftp.disconnect()
                }
            } finally {
                session.disconnect()
            }
        }
    }

    override suspend fun listBackups(): Result<List<RemoteBackupFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession()
            try {
                val sftp = openSftpChannel(session)
                try {
                    ensureRemoteDirectory(sftp, config.remotePath)
                    @Suppress("UNCHECKED_CAST")
                    val entries = sftp.ls(".") as Vector<ChannelSftp.LsEntry>
                    entries.filter { !it.attrs.isDir && it.filename.endsWith(".json", ignoreCase = true) }
                        .map {
                            RemoteBackupFile(
                                name = it.filename,
                                size = it.attrs.size,
                                lastModified = it.attrs.mTime * 1000L,
                                isDirectory = false,
                            )
                        }
                        .sortedByDescending { it.lastModified }
                } finally {
                    sftp.disconnect()
                }
            } finally {
                session.disconnect()
            }
        }
    }

    override suspend fun delete(fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val session = openSession()
            try {
                val sftp = openSftpChannel(session)
                try {
                    ensureRemoteDirectory(sftp, config.remotePath)
                    sftp.rm(fileName)
                } finally {
                    sftp.disconnect()
                }
            } finally {
                session.disconnect()
            }
        }
    }
}
