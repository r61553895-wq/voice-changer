package com.voicechanger.app.ai_model

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Runs a TensorFlow Lite voice-conversion model. Alternative backend to
 * OnnxVoiceConversionModel - pick whichever your exported model targets.
 * Place your .tflite file in app/src/main/assets/models/.
 */
class TFLiteVoiceConversionModel(private val context: Context) : VoiceConversionModel {

    override val id: String = "tflite_vc"
    override val runtimeName: String = "TensorFlow Lite"

    private var interpreter: Interpreter? = null
    private var frameSize: Int = 1024

    override suspend fun load(modelPath: String): Boolean {
        return try {
            val afd = context.assets.openFd(modelPath)
            val inputStream = afd.createInputStream()
            val buffer = inputStream.channel.map(
                FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength
            )
            val options = Interpreter.Options().apply { setNumThreads(2) }
            interpreter = Interpreter(buffer, options)
            true
        } catch (e: Exception) {
            interpreter = null
            false
        }
    }

    override fun convert(inputFrame: FloatArray, sampleRate: Int): FloatArray {
        val interp = interpreter ?: return inputFrame
        return try {
            val inputBuffer = ByteBuffer.allocateDirect(4 * inputFrame.size)
                .order(ByteOrder.nativeOrder())
            inputBuffer.asFloatBuffer().put(inputFrame)

            val outputBuffer = ByteBuffer.allocateDirect(4 * inputFrame.size)
                .order(ByteOrder.nativeOrder())

            interp.run(inputBuffer, outputBuffer)

            val out = FloatArray(inputFrame.size)
            outputBuffer.rewind()
            outputBuffer.asFloatBuffer().get(out)
            out
        } catch (e: Exception) {
            inputFrame
        }
    }

    override fun isLoaded(): Boolean = interpreter != null

    override fun unload() {
        interpreter?.close()
        interpreter = null
    }
}
