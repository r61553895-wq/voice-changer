package com.voicechanger.app.domain.usecase

import com.voicechanger.app.domain.model.VoicePreset
import com.voicechanger.app.domain.repository.AudioEngineRepository
import javax.inject.Inject

class ApplyPresetUseCase @Inject constructor(
    private val repository: AudioEngineRepository
) {
    operator fun invoke(preset: VoicePreset) = repository.applyPreset(preset)
}
