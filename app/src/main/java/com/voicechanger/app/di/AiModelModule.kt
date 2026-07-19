package com.voicechanger.app.di

import com.voicechanger.app.data.repository.VoiceModelRepositoryImpl
import com.voicechanger.app.domain.repository.VoiceModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Swap-point for AI backends: change the binding below (or the runtime
 * string used in VoiceModelRepositoryImpl/ModelFactory) to switch between
 * ONNX Runtime Mobile and TensorFlow Lite without touching UI code.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModelModule {
    @Binds
    @Singleton
    abstract fun bindVoiceModelRepository(
        impl: VoiceModelRepositoryImpl
    ): VoiceModelRepository
}
