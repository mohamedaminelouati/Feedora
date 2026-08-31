package com.mohamedaminelouati.feedora.infrastructure.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mohamedaminelouati.feedora.BuildConfig
import com.mohamedaminelouati.feedora.domain.data.DiffMapHolder
import com.mohamedaminelouati.feedora.domain.service.AccountService
import com.mohamedaminelouati.feedora.domain.service.AppService
import com.mohamedaminelouati.feedora.domain.service.LocalRssService
import com.mohamedaminelouati.feedora.domain.service.OpmlService
import com.mohamedaminelouati.feedora.domain.service.RssService
import com.mohamedaminelouati.feedora.infrastructure.db.AndroidDatabase
import com.mohamedaminelouati.feedora.infrastructure.di.ApplicationScope
import com.mohamedaminelouati.feedora.infrastructure.di.IODispatcher
import com.mohamedaminelouati.feedora.infrastructure.net.NetworkDataSource
import com.mohamedaminelouati.feedora.infrastructure.preference.SettingsProvider
import com.mohamedaminelouati.feedora.infrastructure.rss.OPMLDataSource
import com.mohamedaminelouati.feedora.infrastructure.rss.RssHelper
import com.mohamedaminelouati.feedora.ui.ext.del
import com.mohamedaminelouati.feedora.ui.ext.getLatestApk
import com.mohamedaminelouati.feedora.ui.ext.isGitHub
import okhttp3.OkHttpClient

/** The Application class, where the Dagger components is generated. */
@HiltAndroidApp
class AndroidApp : Application(), Configuration.Provider {

    /**
     * From: [Feeder](https://gitlab.com/spacecowboy/Feeder).
     *
     * Install Conscrypt to handle TLSv1.3 pre Android10.
     */
    init {
        // Cancel TLSv1.3 support pre Android10
        // Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    @Inject lateinit var androidDatabase: AndroidDatabase

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var workManager: WorkManager

    @Inject lateinit var networkDataSource: NetworkDataSource

    @Inject lateinit var OPMLDataSource: OPMLDataSource

    @Inject lateinit var rssHelper: RssHelper

    @Inject lateinit var notificationHelper: NotificationHelper

    @Inject lateinit var appService: AppService

    @Inject lateinit var androidStringsHelper: AndroidStringsHelper

    @Inject lateinit var accountService: AccountService

    @Inject lateinit var localRssService: LocalRssService

    @Inject lateinit var opmlService: OpmlService

    @Inject lateinit var rssService: RssService

    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    @Inject @IODispatcher lateinit var ioDispatcher: CoroutineDispatcher

    @Inject lateinit var okHttpClient: OkHttpClient

    @Inject lateinit var imageLoader: ImageLoader

    @Inject lateinit var imageDownloader: AndroidImageDownloader

    @Inject lateinit var settingsProvider: SettingsProvider

    @Inject lateinit var diffMapHolder: DiffMapHolder

    /**
     * When the application startup.
     * 1. Set the uncaught exception handler
     * 2. Initialize the default account if there is none
     * 3. Synchronize once
     * 4. Check for new version
     */
    override fun onCreate() {
        super.onCreate()
        CrashHandler(this)
        applicationScope.launch {
            accountInit()
            workerInit()
            checkUpdate()
        }
        Coil.setImageLoader(imageLoader)
    }

    /** Override the [Configuration.Builder] to provide the [HiltWorkerFactory]. */
    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setWorkerCoroutineContext(Dispatchers.IO)
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()

    private suspend fun accountInit() {
        withContext(ioDispatcher) {
            if (accountService.isNoAccount()) {
                launch { accountService.initWithDefaultAccount() }
                    .invokeOnCompletion {
                        rssService.get().doSyncOneTime(accountService.getCurrentAccountId())
                    }
            }
        }
    }

    private suspend fun workerInit() {
        rssService.get().initSync()
    }

    private suspend fun checkUpdate() {
        if (!isGitHub) return
        withContext(ioDispatcher) {
            applicationContext.getLatestApk().let { if (it.exists()) it.del() }
        }
        appService.checkUpdate(showToast = false)
    }
}
