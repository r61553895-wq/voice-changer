package com.voicechanger.app.domain.model

data class EngineState(
    val isRunning: Boolean = false,
    val activePreset: VoicePreset = VoicePreset.NORMAL,
    val effectParams: EffectParams = EffectParams(),
    val aiModelEnabled: Boolean = false,
    val aiModelName: String? = null,
    val measuredLatencyMs: Double = 0.0,
    val hasMicPermission: Boolean = false,
    val errorMessage: String? = null
)
