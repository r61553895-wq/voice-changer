package com.voicechanger.app.domain.usecase

import com.voicechanger.app.domain.repository.AudioEngineRepository
import javax.inject.Inject

class StartVoiceChangerUseCase @Inject constructor(
    private val repository: AudioEngineRepository
) {
    operator fun invoke(): Boolean = repository.start()
}
