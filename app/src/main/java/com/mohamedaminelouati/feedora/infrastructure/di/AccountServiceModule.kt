package com.mohamedaminelouati.feedora.infrastructure.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import com.mohamedaminelouati.feedora.domain.repository.AccountDao
import com.mohamedaminelouati.feedora.domain.repository.ArticleDao
import com.mohamedaminelouati.feedora.domain.repository.FeedDao
import com.mohamedaminelouati.feedora.domain.repository.GroupDao
import com.mohamedaminelouati.feedora.domain.service.AccountService
import com.mohamedaminelouati.feedora.domain.service.RssService
import com.mohamedaminelouati.feedora.infrastructure.preference.SettingsProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountServiceModule {
    @Provides
    @Singleton
    fun provideAccountService(
        @ApplicationContext context: Context,
        accountDao: AccountDao,
        groupDao: GroupDao,
        feedDao: FeedDao,
        articleDao: ArticleDao,
        @ApplicationScope coroutineScope: CoroutineScope,
        settingsProvider: SettingsProvider,
    ): AccountService {
        return AccountService(
            context = context,
            accountDao = accountDao,
            groupDao = groupDao,
            feedDao = feedDao,
            articleDao = articleDao,
            coroutineScope = coroutineScope,
            settingsProvider = settingsProvider,
        )
    }
}