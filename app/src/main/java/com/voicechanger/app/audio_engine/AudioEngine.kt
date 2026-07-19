package com.voicechanger.app.audio_engine

/**
 * Thin JNI wrapper around the native C++/Oboe real-time audio pipeline
 * (see app/src/main/cpp/audio_engine.cpp). This class owns no state of
 * its own beyond the native handle - all DSP happens off the JVM.
 */
class AudioEngine {

    companion object {
        init {
            System.loadLibrary("voiceengine")
        }
    }

    fun start(): Boolean = nativeStart()
    fun stop() = nativeStop()

    fun setPitchRatio(ratio: Float) = nativeSetPitch(ratio)
    fun setFormantRatio(ratio: Float) = nativeSetFormant(ratio)
    fun setReverbMix(mix: Float) = nativeSetReverb(mix)
    fun setEchoMix(mix: Float) = nativeSetEcho(mix)
    fun setRobotEnabled(enabled: Boolean) = nativeSetRobot(enabled)
    fun setAiEnabled(enabled: Boolean) = nativeSetAiEnabled(enabled)
    fun applyPreset(nativePresetName: String) = nativeApplyPreset(nativePresetName)
    fun getLatencyMs(): Double = nativeGetLatencyMs()

    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeSetPitch(ratio: Float)
    private external fun nativeSetFormant(ratio: Float)
    private external fun nativeSetReverb(mix: Float)
    private external fun nativeSetEcho(mix: Float)
    private external fun nativeSetRobot(enabled: Boolean)
    private external fun nativeSetAiEnabled(enabled: Boolean)
    private external fun nativeApplyPreset(preset: String)
    private external fun nativeGetLatencyMs(): Double
}
