package com.cunyi.ai.di

import android.content.Context
import com.cunyi.ai.BuildConfig
import com.cunyi.ai.model.LiteRTEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLiteRTEngine(@ApplicationContext context: Context): LiteRTEngine {
        return LiteRTEngine(context)
    }
}
