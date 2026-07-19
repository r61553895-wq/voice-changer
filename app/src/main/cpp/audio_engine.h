#pragma once
#include <oboe/Oboe.h>
#include <memory>
#include <atomic>
#include <mutex>
#include "pitch_shifter.h"
#include "formant_shifter.h"
#include "effects.h"

// AI hook: raw block is exposed to an optional external model callback
// (set from Kotlin/JNI) so an ONNX/TFLite model can process/replace the
// DSP output before it hits the speaker. Returning false = pass DSP through.
using AiInferenceCallback = bool(*)(float* buffer, int32_t numFrames, int32_t sampleRate, void* userData);

struct VoiceParams {
    std::atomic<float> pitchRatio{1.0f};
    std::atomic<float> formantRatio{1.0f};
    std::atomic<float> reverbMix{0.0f};
    std::atomic<float> echoMix{0.0f};
    std::atomic<bool>  robotEnabled{false};
    std::atomic<bool>  aiEnabled{false};
};

class AudioEngine : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
public:
    AudioEngine();
    ~AudioEngine() override;

    bool start();
    void stop();

    void setPitchRatio(float ratio) { mParams.pitchRatio = ratio; }
    void setFormantRatio(float ratio) { mParams.formantRatio = ratio; }
    void setReverbMix(float mix) { mParams.reverbMix = mix; }
    void setEchoMix(float mix) { mParams.echoMix = mix; }
    void setRobotEnabled(bool enabled) { mParams.robotEnabled = enabled; }
    void setAiEnabled(bool enabled) { mParams.aiEnabled = enabled; }
    void applyPreset(const char* presetName);

    void setAiCallback(AiInferenceCallback cb, void* userData) {
        mAiCallback = cb;
        mAiUserData = userData;
    }

    double getMeasuredLatencyMs() const { return mMeasuredLatencyMs; }

    // oboe::AudioStreamDataCallback
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData, int32_t numFrames) override;
    // oboe::AudioStreamErrorCallback
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    std::shared_ptr<oboe::AudioStream> mInputStream;
    std::shared_ptr<oboe::AudioStream> mOutputStream;

    std::unique_ptr<PitchShifter> mPitchShifter;
    std::unique_ptr<FormantShifter> mFormantShifter;
    std::unique_ptr<RobotEffect> mRobot;
    std::unique_ptr<EchoEffect> mEcho;
    std::unique_ptr<ReverbEffect> mReverb;

    VoiceParams mParams;
    std::vector<float> mScratchA;
    std::vector<float> mScratchB;

    AiInferenceCallback mAiCallback = nullptr;
    void* mAiUserData = nullptr;

    std::atomic<double> mMeasuredLatencyMs{0.0};

    bool openStreams();
};
