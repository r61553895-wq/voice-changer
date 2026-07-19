package com.voicechanger.app.ai_model

import android.content.Context

/**
 * Central place to add new AI backends. To plug in a new runtime, add a
 * case here (and implement VoiceConversionModel) - no other file in the
 * app needs to change.
 */
object ModelFactory {
    fun create(runtime: String, context: Context): VoiceConversionModel = when (runtime) {
        "ONNX" -> OnnxVoiceConversionModel(context)
        "TFLite" -> TFLiteVoiceConversionModel(context)
        else -> PassThroughModel()
    }
}
