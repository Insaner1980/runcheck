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
abstract class InsightDebugModule {
    @Binds
    @Singleton
    abstract fun bindInsightDebugActions(impl: DebugInsightActionsImpl): InsightDebugActions
}
