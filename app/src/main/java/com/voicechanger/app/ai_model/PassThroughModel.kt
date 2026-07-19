package com.voicechanger.app.ai_model

/**
 * "Test mode" model used when no AI backend is installed/loaded.
 * Simply returns audio unchanged, letting the DSP-only pipeline (pitch/
 * formant/robot/reverb/echo) run standalone. This is what the MVP ships
 * with by default so it works fully offline with zero model files.
 */
class PassThroughModel : VoiceConversionModel {
    override val id: String = "passthrough"
    override val runtimeName: String = "None (DSP only / test mode)"
    private var loaded = true

    override suspend fun load(modelPath: String): Boolean = true
    override fun convert(inputFrame: FloatArray, sampleRate: Int): FloatArray = inputFrame
    override fun isLoaded(): Boolean = loaded
    override fun unload() { loaded = false }
}
