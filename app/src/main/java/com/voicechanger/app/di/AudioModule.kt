package com.voicechanger.app.di

import com.voicechanger.app.audio_engine.AudioEngine
import com.voicechanger.app.data.repository.AudioEngineRepositoryImpl
import com.voicechanger.app.domain.repository.AudioEngineRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioProviderModule {
    @Provides
    @Singleton
    fun provideAudioEngine(): AudioEngine = AudioEngine()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioBindModule {
    @Binds
    @Singleton
    abstract fun bindAudioEngineRepository(
        impl: AudioEngineRepositoryImpl
    ): AudioEngineRepository
}
