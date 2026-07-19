#include "audio_engine.h"
#include <android/log.h>
#include <cstring>

#define TAG "VoiceEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

AudioEngine::AudioEngine() = default;
AudioEngine::~AudioEngine() { stop(); }

bool AudioEngine::openStreams() {
    // --- Input (mic) stream: exclusive mode + low latency for min round trip ---
    oboe::AudioStreamBuilder inBuilder;
    inBuilder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(48000)
            ->setInputPreset(oboe::InputPreset::VoiceCommunication)
            ->setDataCallback(this)
            ->setErrorCallback(this);

    oboe::Result r = inBuilder.openStream(mInputStream);
    if (r != oboe::Result::OK) {
        LOGE("Failed to open input stream: %s", oboe::convertToText(r));
        return false;
    }

    // --- Output stream: same low-latency exclusive path ---
    oboe::AudioStreamBuilder outBuilder;
    outBuilder.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(mInputStream->getSampleRate())
            ->setUsage(oboe::Usage::VoiceCommunication);

    r = outBuilder.openStream(mOutputStream);
    if (r != oboe::Result::OK) {
        LOGE("Failed to open output stream: %s", oboe::convertToText(r));
        return false;
    }

    int32_t sampleRate = mInputStream->getSampleRate();
    mPitchShifter = std::make_unique<PitchShifter>(sampleRate);
    mFormantShifter = std::make_unique<FormantShifter>(sampleRate);
    mRobot = std::make_unique<RobotEffect>(sampleRate);
    mEcho = std::make_unique<EchoEffect>(sampleRate);
    mReverb = std::make_unique<ReverbEffect>(sampleRate);

    int32_t maxFrames = mInputStream->getFramesPerBurst() * 4;
    mScratchA.assign(maxFrames, 0.0f);
    mScratchB.assign(maxFrames, 0.0f);

    return true;
}

bool AudioEngine::start() {
    if (!openStreams()) return false;

    // Input stream is driven by its own callback thread; we read from it
    // synchronously inside the OUTPUT callback for tight, glitch-free sync
    // (classic Oboe full-duplex "pull" pattern) -> this is what keeps the
    // round-trip latency under ~40-80ms on most devices.
    oboe::Result r1 = mInputStream->requestStart();
    oboe::Result r2 = mOutputStream->requestStart();
    if (r1 != oboe::Result::OK || r2 != oboe::Result::OK) {
        LOGE("Failed to start streams");
        return false;
    }
    LOGI("Audio engine started @ %d Hz, burst=%d frames",
         mInputStream->getSampleRate(), mInputStream->getFramesPerBurst());
    return true;
}

void AudioEngine::stop() {
    if (mOutputStream) { mOutputStream->requestStop(); mOutputStream->close(); mOutputStream.reset(); }
    if (mInputStream)  { mInputStream->requestStop();  mInputStream->close();  mInputStream.reset(); }
}

void AudioEngine::applyPreset(const char* presetName) {
    std::string p(presetName);
    if (p == "NORMAL") {
        setPitchRatio(1.0f); setFormantRatio(1.0f); setRobotEnabled(false);
        setEchoMix(0.0f); setReverbMix(0.0f);
    } else if (p == "DEEP") {
        setPitchRatio(0.78f); setFormantRatio(0.75f); setRobotEnabled(false);
    } else if (p == "FEMALE") {
        setPitchRatio(1.35f); setFormantRatio(1.30f); setRobotEnabled(false);
    } else if (p == "ROBOT") {
        setPitchRatio(1.0f); setFormantRatio(1.0f); setRobotEnabled(true);
    } else if (p == "MONSTER") {
        setPitchRatio(0.55f); setFormantRatio(0.6f); setRobotEnabled(false);
        setReverbMix(0.25f);
    } else if (p == "ANIME") {
        setPitchRatio(1.6f); setFormantRatio(1.5f); setRobotEnabled(false);
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream* outStream, void* audioData, int32_t numFrames) {
    auto* out = static_cast<float*>(audioData);

    if ((int32_t) mScratchA.size() < numFrames) {
        mScratchA.resize(numFrames);
        mScratchB.resize(numFrames);
    }

    // Pull the freshest mic audio (non-blocking read w/ short timeout keeps
    // us glitch-free without adding a full extra buffer of latency).
    auto result = mInputStream->read(mScratchA.data(), numFrames, 0 /* no wait */);
    int32_t framesRead = result ? result.value() : 0;
    for (int32_t i = framesRead; i < numFrames; ++i) mScratchA[i] = 0.0f;

    // ---- DSP pipeline: pitch -> formant -> robot -> reverb -> echo ----
    mPitchShifter->setPitchRatio(mParams.pitchRatio);
    mPitchShifter->process(mScratchA.data(), mScratchB.data(), numFrames);

    mFormantShifter->setFormantRatio(mParams.formantRatio);
    mFormantShifter->process(mScratchB.data(), mScratchA.data(), numFrames);

    mRobot->setEnabled(mParams.robotEnabled);
    mRobot->process(mScratchA.data(), mScratchB.data(), numFrames);

    mReverb->setMix(mParams.reverbMix);
    mReverb->process(mScratchB.data(), mScratchA.data(), numFrames);

    mEcho->setMix(mParams.echoMix);
    mEcho->process(mScratchA.data(), mScratchB.data(), numFrames);

    // ---- Optional AI voice-conversion hook (ONNX/TFLite via JNI) ----
    if (mParams.aiEnabled && mAiCallback) {
        mAiCallback(mScratchB.data(), numFrames, mOutputStream->getSampleRate(), mAiUserData);
    }

    std::memcpy(out, mScratchB.data(), numFrames * sizeof(float));

    double inputLatencyMs = 0.0;
    auto latResult = mInputStream->calculateLatencyMillis();
    if (latResult) inputLatencyMs = latResult.value();
    mMeasuredLatencyMs = inputLatencyMs;

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    LOGE("Stream error after close: %s", oboe::convertToText(error));
    // Attempt automatic recovery (e.g. after Bluetooth headset switch).
    if (stream == mOutputStream.get() || stream == mInputStream.get()) {
        stop();
        start();
    }
}
