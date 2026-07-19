#pragma once
#include <vector>
#include <cstdint>

// Ring-modulator based "robot" effect.
class RobotEffect {
public:
    explicit RobotEffect(int32_t sampleRate, float carrierHz = 60.0f);
    void setEnabled(bool enabled) { mEnabled = enabled; }
    void setCarrierHz(float hz) { mCarrierHz = hz; }
    void process(const float* in, float* out, int32_t n);
private:
    int32_t mSampleRate;
    float mCarrierHz;
    float mPhase = 0.0f;
    bool mEnabled = false;
};

// Simple delay-line echo.
class EchoEffect {
public:
    explicit EchoEffect(int32_t sampleRate, float delayMs = 220.0f);
    void setMix(float wetMix) { mWet = wetMix; } // 0..1
    void setDelayMs(float ms);
    void setFeedback(float fb) { mFeedback = fb; }
    void process(const float* in, float* out, int32_t n);
private:
    int32_t mSampleRate;
    std::vector<float> mBuffer;
    int32_t mWritePos = 0;
    float mWet = 0.0f;
    float mFeedback = 0.35f;
};

// Schroeder-style reverb (parallel combs + series allpass) - lightweight,
// good enough for a voice-changer "space" effect without heavy CPU cost.
class ReverbEffect {
public:
    explicit ReverbEffect(int32_t sampleRate);
    void setMix(float wetMix) { mWet = wetMix; } // 0..1
    void process(const float* in, float* out, int32_t n);
private:
    struct Comb { std::vector<float> buf; int32_t pos = 0; float feedback; };
    struct AllPass { std::vector<float> buf; int32_t pos = 0; };

    int32_t mSampleRate;
    float mWet = 0.0f;
    std::vector<Comb> mCombs;
    std::vector<AllPass> mAllpasses;
};
