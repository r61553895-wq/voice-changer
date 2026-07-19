package com.voicechanger.app.ai_model

/**
 * Common contract every AI voice-conversion backend must implement.
 * This is the single seam you touch to swap in a real RVC-exported ONNX
 * model, a TFLite model, or anything else - nothing above this interface
 * (ViewModel, UI, use cases) needs to change.
 */
interface VoiceConversionModel {
    val id: String
    val runtimeName: String

    /** Loads model weights/graph from assets or app storage. */
    suspend fun load(modelPath: String): Boolean

    /**
     * Runs inference on one audio frame (mono float PCM, -1..1).
     * Must be fast enough to run every ~20-40ms block to stay real-time;
     * returns the converted audio frame of the same length.
     */
    fun convert(inputFrame: FloatArray, sampleRate: Int): FloatArray

    fun isLoaded(): Boolean
    fun unload()
}
