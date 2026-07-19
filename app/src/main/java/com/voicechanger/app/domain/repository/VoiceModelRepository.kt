package com.voicechanger.app.domain.repository

/** Metadata describing a pluggable AI voice-conversion model. */
data class VoiceModelInfo(
    val id: String,
    val displayName: String,
    val runtime: String, // "ONNX", "TFLite", "None"
    val isLoaded: Boolean
)

/**
 * Abstraction for discovering/loading AI voice conversion models
 * (RVC exported to ONNX, or a TFLite model). Swapping the underlying
 * implementation (Onnx/TFLite/PassThrough) never requires touching the UI
 * or ViewModel - only the DI binding in AiModelModule.
 */
interface VoiceModelRepository {
    suspend fun listAvailableModels(): List<VoiceModelInfo>
    suspend fun loadModel(modelId: String): Boolean
    fun unloadModel()
    fun currentModel(): VoiceModelInfo?
}
