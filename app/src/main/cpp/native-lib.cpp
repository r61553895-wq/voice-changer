#include <jni.h>
#include <memory>
#include "audio_engine.h"

static std::unique_ptr<AudioEngine> gEngine;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeStart(JNIEnv*, jobject) {
    if (!gEngine) gEngine = std::make_unique<AudioEngine>();
    return gEngine->start();
}

JNIEXPORT void JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeStop(JNIEnv*, jobject) {
    if (gEngine) gEngine->stop();
}

JNIEXPORT void JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeSetPitch(JNIEnv*, jobject, jfloat ratio) {
    if (gEngine) gEngine->setPitchRatio(ratio);
}

JNIEXPORT void JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeSetFormant(JNIEnv*, jobject, jfloat ratio) {
    if (gEngine) gEngine->setFormantRatio(ratio);
}

JNIEXPORT void JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeSetReverb(JNIEnv*, jobject, jfloat mix) {
    if (gEngine) gEngine->setReverbMix(mix);
}

JNIEXPORT void JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeSetEcho(JNIEnv*, jobject, jfloat mix) {
    if (gEngine) gEngine->setEchoMix(mix);
}

JNIEXPORT void JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeSetRobot(JNIEnv*, jobject, jboolean enabled) {
    if (gEngine) gEngine->setRobotEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeSetAiEnabled(JNIEnv*, jobject, jboolean enabled) {
    if (gEngine) gEngine->setAiEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeApplyPreset(JNIEnv* env, jobject, jstring preset) {
    if (!gEngine) return;
    const char* cstr = env->GetStringUTFChars(preset, nullptr);
    gEngine->applyPreset(cstr);
    env->ReleaseStringUTFChars(preset, cstr);
}

JNIEXPORT jdouble JNICALL
Java_com_voicechanger_app_audio_1engine_AudioEngine_nativeGetLatencyMs(JNIEnv*, jobject) {
    return gEngine ? gEngine->getMeasuredLatencyMs() : 0.0;
}

} // extern "C"
