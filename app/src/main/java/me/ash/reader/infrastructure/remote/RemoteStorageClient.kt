package me.ash.reader.infrastructure.remote

import java.io.InputStream
import java.io.OutputStream

interface RemoteStorageClient {
    suspend fun testConnection(): Result<Unit>

    suspend fun upload(
        fileName: String,
        inputStream: InputStream,
        contentLength: Long,
        onProgress: (Float) -> Unit = {},
    ): Result<Unit>

    suspend fun download(
        fileName: String,
        outputStream: OutputStream,
        onProgress: (Float) -> Unit = {},
    ): Result<Unit>

    suspend fun listBackups(): Result<List<RemoteBackupFile>>

    suspend fun delete(fileName: String): Result<Unit>
}
