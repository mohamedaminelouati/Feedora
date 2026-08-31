package com.mohamedaminelouati.feedora.domain.service

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import com.mohamedaminelouati.feedora.infrastructure.preference.CloudBackupFrequency
import com.mohamedaminelouati.feedora.infrastructure.preference.CloudBackupPreferencesManager
import com.mohamedaminelouati.feedora.infrastructure.preference.CloudBackupSettings
import com.mohamedaminelouati.feedora.infrastructure.remote.RemoteBackupFile
import com.mohamedaminelouati.feedora.infrastructure.remote.RemoteServerConfig
import com.mohamedaminelouati.feedora.infrastructure.remote.RemoteStorageClient
import com.mohamedaminelouati.feedora.infrastructure.remote.RemoteStorageClientFactory
import com.mohamedaminelouati.feedora.infrastructure.remote.RemoteStorageProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner.Silent::class)
class CloudBackupServiceTest {

    @Mock
    private lateinit var backupService: BackupService

    @Mock
    private lateinit var clientFactory: RemoteStorageClientFactory

    @Mock
    private lateinit var preferencesManager: CloudBackupPreferencesManager

    private lateinit var cloudBackupService: CloudBackupService
    private val mockContext: Context = mock()
    private val mockClient: RemoteStorageClient = mock()

    @Before
    fun setUp() {
        `when`(mockContext.cacheDir).thenReturn(File(System.getProperty("java.io.tmpdir") ?: "."))

        cloudBackupService = CloudBackupService(
            context = mockContext,
            backupService = backupService,
            clientFactory = clientFactory,
            preferencesManager = preferencesManager,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun testTestConnectionSuccess() = runBlocking {
        val config = RemoteServerConfig(
            protocol = RemoteStorageProtocol.WEBDAV,
            host = "example.com",
            port = 443,
            remotePath = "/Feedora/",
        )
        `when`(clientFactory.createClient(config)).thenReturn(mockClient)
        `when`(mockClient.testConnection()).thenReturn(Result.success(Unit))

        val result = cloudBackupService.testConnection(config)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testListRemoteBackups() = runBlocking {
        val config = RemoteServerConfig(
            protocol = RemoteStorageProtocol.FTP,
            host = "ftp.example.com",
            port = 21,
        )
        val settings = CloudBackupSettings(config = config)
        `when`(preferencesManager.getSettings()).thenReturn(settings)
        `when`(clientFactory.createClient(config)).thenReturn(mockClient)

        val files = listOf(
            RemoteBackupFile("backup-1.json", 1024, 1000L),
            RemoteBackupFile("backup-2.json", 2048, 2000L),
        )
        `when`(mockClient.listBackups()).thenReturn(Result.success(files))

        val result = cloudBackupService.listRemoteBackups()
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }
}
