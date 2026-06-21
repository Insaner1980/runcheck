package com.runcheck.di

import com.runcheck.debug.insights.DebugInsightActionsImpl
import com.runcheck.domain.repository.InsightDebugActions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("kotlin:S6517")
interface InsightDebugModule {
    @Binds
    @Singleton
    fun bindInsightDebugActions(impl: DebugInsightActionsImpl): InsightDebugActions
}
