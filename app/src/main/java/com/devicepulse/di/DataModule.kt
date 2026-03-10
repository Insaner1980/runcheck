package com.devicepulse.di

import android.content.Context
import com.devicepulse.data.battery.BatteryDataSourceFactory
import com.devicepulse.data.thermal.ThermalDataSource
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideBatteryDataSourceFactory(
        @ApplicationContext context: Context
    ): BatteryDataSourceFactory = BatteryDataSourceFactory(context)

    @Provides
    @Singleton
    fun provideThermalDataSource(
        @ApplicationContext context: Context
    ): ThermalDataSource = ThermalDataSource(context)
}
