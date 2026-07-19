#pragma once
#include <vector>
#include <cstdint>

// Real-time granular (overlap-add) pitch shifter.
// Works on mono float blocks. Grain-based resampling keeps latency low
// (grain size ~ 20-40ms) which is what makes this usable for live voice.
class PitchShifter {
public:
    explicit PitchShifter(int32_t sampleRate);

    // pitchRatio: 1.0 = no change, >1.0 = higher pitch, <1.0 = lower pitch
    void setPitchRatio(float pitchRatio);

    // Processes one block in-place-ish: reads `in`, writes `out`, both length n.
    void process(const float* in, float* out, int32_t n);

private:
    int32_t mSampleRate;
    float mPitchRatio = 1.0f;

    // Circular input ring buffer that we read grains from at a variable rate.
    std::vector<float> mRing;
    int32_t mRingSize;
    int32_t mWritePos = 0;
    double mReadPos = 0.0;

    // Two overlapping grains (50% overlap Hann window) to avoid clicks.
    static constexpr int32_t kGrainSize = 1024; // ~21ms @ 48kHz -> keeps total path latency low
    std::vector<float> mWindow;

    float readInterpolated(double pos) const;
};
