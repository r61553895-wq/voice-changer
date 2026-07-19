package com.voicechanger.app.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicechanger.app.domain.model.EffectParams
import com.voicechanger.app.domain.model.EngineState
import com.voicechanger.app.domain.model.VoicePreset
import com.voicechanger.app.domain.repository.AudioEngineRepository
import com.voicechanger.app.domain.usecase.ApplyPresetUseCase
import com.voicechanger.app.domain.usecase.LoadVoiceModelUseCase
import com.voicechanger.app.domain.usecase.StartVoiceChangerUseCase
import com.voicechanger.app.domain.usecase.StopVoiceChangerUseCase
import com.voicechanger.app.domain.usecase.UpdateEffectParamsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val startVoiceChanger: StartVoiceChangerUseCase,
    private val stopVoiceChanger: StopVoiceChangerUseCase,
    private val applyPreset: ApplyPresetUseCase,
    private val updateEffectParams: UpdateEffectParamsUseCase,
    private val loadVoiceModel: LoadVoiceModelUseCase,
    private val audioEngineRepository: AudioEngineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state

    fun onMicPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasMicPermission = granted) }
    }

    fun toggleEngine() {
        val running = _state.value.isRunning
        if (running) {
            stopVoiceChanger()
            _state.update { it.copy(isRunning = false) }
        } else {
            if (!_state.value.hasMicPermission) {
                _state.update { it.copy(errorMessage = "Нужен доступ к микрофону") }
                return
            }
            val ok = startVoiceChanger()
            if (ok) {
                applyPreset(_state.value.activePreset)
                updateEffectParams(_state.value.effectParams)
                _state.update { it.copy(isRunning = true, errorMessage = null) }
                observeLatency()
            } else {
                _state.update { it.copy(errorMessage = "Не удалось запустить аудио движок") }
            }
        }
    }

    fun selectPreset(preset: VoicePreset) {
        _state.update { it.copy(activePreset = preset, effectParams = presetToParams(preset)) }
        applyPreset(preset)
    }

    fun updatePitch(semitones: Float) = updateParams { it.copy(pitchSemitones = semitones) }
    fun updateVoiceDepth(depth: Float) = updateParams { it.copy(voiceDepth = depth) }
    fun updateReverb(mix: Float) = updateParams { it.copy(reverbMix = mix) }
    fun updateEcho(mix: Float) = updateParams { it.copy(echoMix = mix) }

    fun toggleAiModel(enabled: Boolean) {
        viewModelScope.launch {
            val loaded = if (enabled) loadVoiceModel("onnx_rvc") else true.also {
                audioEngineRepository.setAiEnabled(false)
            }
            _state.update { it.copy(aiModelEnabled = enabled && loaded, aiModelName = if (enabled && loaded) "onnx_rvc" else null) }
        }
    }

    private fun updateParams(transform: (EffectParams) -> EffectParams) {
        _state.update { it.copy(effectParams = transform(it.effectParams)) }
        updateEffectParams(_state.value.effectParams)
    }

    private fun presetToParams(preset: VoicePreset): EffectParams = when (preset) {
        VoicePreset.NORMAL -> EffectParams()
        VoicePreset.DEEP -> EffectParams(pitchSemitones = -4.5f, voiceDepth = 0.6f)
        VoicePreset.FEMALE -> EffectParams(pitchSemitones = 5f, voiceDepth = -0.6f)
        VoicePreset.ROBOT -> EffectParams()
        VoicePreset.MONSTER -> EffectParams(pitchSemitones = -9f, voiceDepth = 0.9f, reverbMix = 0.25f)
        VoicePreset.ANIME -> EffectParams(pitchSemitones = 8f, voiceDepth = -0.8f)
    }

    private fun observeLatency() {
        viewModelScope.launch {
            while (_state.value.isRunning) {
                delay(500)
                audioEngineRepository.measuredLatencyMs.value.let { ms ->
                    _state.update { it.copy(measuredLatencyMs = ms) }
                }
            }
        }
    }
}
