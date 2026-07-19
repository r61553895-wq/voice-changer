package com.voicechanger.app.domain.usecase

import com.voicechanger.app.domain.model.EffectParams
import com.voicechanger.app.domain.repository.AudioEngineRepository
import javax.inject.Inject

class UpdateEffectParamsUseCase @Inject constructor(
    private val repository: AudioEngineRepository
) {
    operator fun invoke(params: EffectParams) = repository.updateEffectParams(params)
}
