package com.voicechanger.app.domain.repository

import com.voicechanger.app.domain.model.EffectParams
import com.voicechanger.app.domain.model.VoicePreset
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over the native (C++/Oboe) real-time audio pipeline.
 * The presentation layer never touches JNI directly - it goes through this.
 */
interface AudioEngineRepository {
    val isRunning: StateFlow<Boolean>
    val measuredLatencyMs: StateFlow<Double>

    fun start(): Boolean
    fun stop()

    fun applyPreset(preset: VoicePreset)
    fun updateEffectParams(params: EffectParams)
    fun setAiEnabled(enabled: Boolean)
}
