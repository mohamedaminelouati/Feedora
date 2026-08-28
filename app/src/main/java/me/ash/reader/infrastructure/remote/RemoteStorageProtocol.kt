package me.ash.reader.infrastructure.remote

enum class RemoteStorageProtocol(val defaultPort: Int, val scheme: String) {
    WEBDAV(443, "https"),
    FTP(21, "ftp"),
    FTPS(990, "ftps"),
    SFTP(22, "sftp");

    companion object {
        fun fromString(value: String): RemoteStorageProtocol {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: WEBDAV
        }
    }
}

data class RemoteBackupFile(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean = false,
)

data class RemoteServerConfig(
    val protocol: RemoteStorageProtocol = RemoteStorageProtocol.WEBDAV,
    val host: String = "",
    val port: Int = protocol.defaultPort,
    val remotePath: String = "/ReadYou/",
    val username: String = "",
    val password: String = "",
    val trustInsecureSsl: Boolean = false,
)
