package com.voicechanger.app.domain.model

/**
 * User-adjustable effect parameters, driven by UI sliders.
 * Ranges are chosen to stay musically/vocally usable (avoid extreme
 * artifacts) while still giving a noticeable "Voice depth" / reverb / echo feel.
 */
data class EffectParams(
    val pitchSemitones: Float = 0f,      // -12..+12 semitones
    val voiceDepth: Float = 0f,          // -1f (thin) .. +1f (deep) -> maps to formant ratio
    val reverbMix: Float = 0f,           // 0f..1f
    val echoMix: Float = 0f              // 0f..1f
) {
    fun pitchRatio(): Float = Math.pow(2.0, pitchSemitones / 12.0).toFloat()
    fun formantRatio(): Float = 1.0f - (voiceDepth * 0.4f) // depth>0 => ratio<1 => deeper
}
