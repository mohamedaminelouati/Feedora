package com.mohamedaminelouati.feedora.infrastructure.remote

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Singleton
class RemoteStorageClientFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    fun createClient(config: RemoteServerConfig): RemoteStorageClient {
        return when (config.protocol) {
            RemoteStorageProtocol.WEBDAV -> WebDavStorageClient(config, okHttpClient)
            RemoteStorageProtocol.FTP, RemoteStorageProtocol.FTPS -> FtpStorageClient(config)
            RemoteStorageProtocol.SFTP -> SftpStorageClient(config)
        }
    }
}
