package com.voicechanger.app.domain.usecase

import com.voicechanger.app.domain.repository.AudioEngineRepository
import com.voicechanger.app.domain.repository.VoiceModelRepository
import javax.inject.Inject

/**
 * Loads a pluggable AI model (ONNX/TFLite) and, if successful, tells the
 * native engine to route audio through it (AudioEngine.setAiEnabled).
 */
class LoadVoiceModelUseCase @Inject constructor(
    private val voiceModelRepository: VoiceModelRepository,
    private val audioEngineRepository: AudioEngineRepository
) {
    suspend operator fun invoke(modelId: String): Boolean {
        val loaded = voiceModelRepository.loadModel(modelId)
        audioEngineRepository.setAiEnabled(loaded)
        return loaded
    }
}
