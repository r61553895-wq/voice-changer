package com.voicechanger.app.data.repository

import android.content.Context
import com.voicechanger.app.ai_model.ModelFactory
import com.voicechanger.app.ai_model.VoiceConversionModel
import com.voicechanger.app.domain.repository.VoiceModelInfo
import com.voicechanger.app.domain.repository.VoiceModelRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VoiceModelRepository {

    // Catalog of models the app knows about. In production, populate this
    // by scanning app/src/main/assets/models/ or a downloaded models dir.
    private val catalog = listOf(
        VoiceModelInfo("passthrough", "Test mode (no AI)", "None", isLoaded = true),
        VoiceModelInfo("onnx_rvc", "RVC voice (ONNX)", "ONNX", isLoaded = false),
        VoiceModelInfo("tflite_vc", "Voice conversion (TFLite)", "TFLite", isLoaded = false)
    )

    private var activeModel: VoiceConversionModel? = null
    private var activeInfo: VoiceModelInfo? = catalog.first()

    override suspend fun listAvailableModels(): List<VoiceModelInfo> = catalog

    override suspend fun loadModel(modelId: String): Boolean {
        val entry = catalog.find { it.id == modelId } ?: return false
        activeModel?.unload()
        val model = ModelFactory.create(entry.runtime, context)
        val path = "models/$modelId.${if (entry.runtime == "ONNX") "onnx" else "tflite"}"
        val ok = if (entry.runtime == "None") true else model.load(path)
        if (ok) {
            activeModel = model
            activeInfo = entry.copy(isLoaded = true)
        }
        return ok
    }

    override fun unloadModel() {
        activeModel?.unload()
        activeModel = null
        activeInfo = catalog.first()
    }

    override fun currentModel(): VoiceModelInfo? = activeInfo
}
