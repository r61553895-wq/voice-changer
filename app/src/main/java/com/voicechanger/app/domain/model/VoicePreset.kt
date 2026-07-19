package com.voicechanger.app.domain.model

/**
 * Built-in voice presets. Each maps to native DSP parameters in
 * AudioEngine.applyPreset(). Keep names in sync with audio_engine.cpp.
 */
enum class VoicePreset(val nativeName: String, val displayName: String, val emoji: String) {
    NORMAL("NORMAL", "Normal", "🎙️"),
    DEEP("DEEP", "Deep", "🕴️"),
    FEMALE("FEMALE", "Female", "👩"),
    ROBOT("ROBOT", "Robot", "🤖"),
    MONSTER("MONSTER", "Monster", "👹"),
    ANIME("ANIME", "Anime", "✨")
}
