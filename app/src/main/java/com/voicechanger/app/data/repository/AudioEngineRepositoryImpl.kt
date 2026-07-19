package com.voicechanger.app.data.repository

import com.voicechanger.app.audio_engine.AudioEngine
import com.voicechanger.app.domain.model.EffectParams
import com.voicechanger.app.domain.model.VoicePreset
import com.voicechanger.app.domain.repository.AudioEngineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioEngineRepositoryImpl @Inject constructor(
    private val audioEngine: AudioEngine
) : AudioEngineRepository {

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning

    private val _measuredLatencyMs = MutableStateFlow(0.0)
    override val measuredLatencyMs: StateFlow<Double> = _measuredLatencyMs

    override fun start(): Boolean {
        val ok = audioEngine.start()
        _isRunning.value = ok
        return ok
    }

    override fun stop() {
        audioEngine.stop()
        _isRunning.value = false
    }

    override fun applyPreset(preset: VoicePreset) {
        audioEngine.applyPreset(preset.nativeName)
    }

    override fun updateEffectParams(params: EffectParams) {
        audioEngine.setPitchRatio(params.pitchRatio())
        audioEngine.setFormantRatio(params.formantRatio())
        audioEngine.setReverbMix(params.reverbMix)
        audioEngine.setEchoMix(params.echoMix)
    }

    override fun setAiEnabled(enabled: Boolean) {
        audioEngine.setAiEnabled(enabled)
    }

    fun refreshLatency() {
        _measuredLatencyMs.value = audioEngine.getLatencyMs()
    }
}
