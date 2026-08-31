package com.mohamedaminelouati.feedora.domain.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import com.mohamedaminelouati.feedora.R
import com.mohamedaminelouati.feedora.domain.model.general.toVersion
import com.mohamedaminelouati.feedora.infrastructure.di.IODispatcher
import com.mohamedaminelouati.feedora.infrastructure.di.MainDispatcher
import com.mohamedaminelouati.feedora.infrastructure.net.Download
import com.mohamedaminelouati.feedora.infrastructure.net.NetworkDataSource
import com.mohamedaminelouati.feedora.infrastructure.net.downloadToFileWithProgress
import com.mohamedaminelouati.feedora.infrastructure.preference.*
import com.mohamedaminelouati.feedora.infrastructure.preference.NewVersionSizePreference.formatSize
import com.mohamedaminelouati.feedora.ui.ext.getCurrentVersion
import com.mohamedaminelouati.feedora.ui.ext.getLatestApk
import com.mohamedaminelouati.feedora.ui.ext.showToast
import com.mohamedaminelouati.feedora.ui.ext.skipVersionNumber
import javax.inject.Inject

class AppService @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val networkDataSource: NetworkDataSource,
    @IODispatcher
    private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
) {

    suspend fun checkUpdate(showToast: Boolean = true): Boolean? = withContext(ioDispatcher) {
        try {
            val response = networkDataSource.getReleaseLatest(context.getString(R.string.update_link))
            when {
                response.code() == 403 -> {
                    withContext(mainDispatcher) {
                        if (showToast) context.showToast(context.getString(R.string.rate_limit))
                    }
                    return@withContext null
                }

                response.body() == null -> {
                    withContext(mainDispatcher) {
                        if (showToast) context.showToast(context.getString(R.string.check_failure))
                    }
                    return@withContext null
                }
            }
            val skipVersion = context.skipVersionNumber.toVersion()
            val currentVersion = context.getCurrentVersion()
            val latest = response.body()!!
            val latestVersion = latest.tag_name.toVersion()
//            val latestVersion = "1.0.0".toVersion()
            val latestLog = latest.body ?: ""
            val latestPublishDate = latest.published_at ?: latest.created_at ?: ""
            val latestSize = latest.assets?.first()?.size ?: 0
            val latestDownloadUrl = latest.assets?.first()?.browser_download_url ?: ""

            Log.i("RLog", "current version $currentVersion")
            if (latestVersion.whetherNeedUpdate(currentVersion, skipVersion)) {
                Log.i("RLog", "new version $latestVersion")
                NewVersionNumberPreference.put(context, this, latestVersion.toString())
                NewVersionLogPreference.put(context, this, latestLog)
                NewVersionPublishDatePreference.put(context, this, latestPublishDate)
                NewVersionSizePreference.put(context, this, latestSize.formatSize())
                NewVersionDownloadUrlPreference.put(context, this, latestDownloadUrl)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("RLog", "checkUpdate: ${e.message}")
            withContext(mainDispatcher) {
                if (showToast) context.showToast(context.getString(R.string.check_failure))
            }
            null
        }
    }

    suspend fun downloadFile(url: String): Flow<Download> =
        withContext(ioDispatcher) {
            Log.i("RLog", "downloadFile start: $url")
            try {
                return@withContext networkDataSource.downloadFile(url)
                    .downloadToFileWithProgress(context.getLatestApk())
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("RLog", "downloadFile: ${e.message}")
                withContext(mainDispatcher) {
                    context.showToast(context.getString(R.string.download_failure))
                }
            }
            emptyFlow()
        }
}
