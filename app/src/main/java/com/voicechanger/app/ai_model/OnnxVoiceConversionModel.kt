package com.voicechanger.app.ai_model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.FloatBuffer

/**
 * Runs an ONNX-exported voice-conversion model (e.g. an RVC checkpoint
 * exported via `torch.onnx.export`, or any other seq2seq/vocoder graph
 * that takes a raw waveform frame and returns a converted waveform frame).
 *
 * Drop your .onnx file into app/src/main/assets/models/ and call
 * load("models/your_model.onnx"). Input/output tensor names below
 * ("audio_in" / "audio_out") must match your exported graph - adjust to
 * match your model's actual IO names.
 */
class OnnxVoiceConversionModel(private val context: Context) : VoiceConversionModel {

    override val id: String = "onnx_rvc"
    override val runtimeName: String = "ONNX Runtime Mobile"

    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null

    override suspend fun load(modelPath: String): Boolean {
        return try {
            env = OrtEnvironment.getEnvironment()
            val bytes = context.assets.open(modelPath).use { it.readBytes() }
            val options = OrtSession.SessionOptions().apply {
                // NNAPI acceleration when available; falls back to CPU otherwise.
                addNnapi()
            }
            session = env!!.createSession(bytes, options)
            true
        } catch (e: Exception) {
            session = null
            false
        }
    }

    override fun convert(inputFrame: FloatArray, sampleRate: Int): FloatArray {
        val currentSession = session ?: return inputFrame
        val currentEnv = env ?: return inputFrame
        return try {
            val shape = longArrayOf(1, inputFrame.size.toLong())
            OnnxTensor.createTensor(currentEnv, FloatBuffer.wrap(inputFrame), shape).use { tensor ->
                val inputs = mapOf("audio_in" to tensor)
                currentSession.run(inputs).use { results ->
                    val output = results.get(0).value as Array<FloatArray>
                    output[0]
                }
            }
        } catch (e: Exception) {
            inputFrame // graceful fallback: never break the audio path
        }
    }

    override fun isLoaded(): Boolean = session != null

    override fun unload() {
        session?.close()
        session = null
        env = null
    }
}
