package com.voicechanger.app.domain.usecase

import com.voicechanger.app.domain.repository.AudioEngineRepository
import javax.inject.Inject

class StopVoiceChangerUseCase @Inject constructor(
    private val repository: AudioEngineRepository
) {
    operator fun invoke() = repository.stop()
}
