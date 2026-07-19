#pragma once
#include <vector>
#include <complex>
#include <cstdint>

// Shifts formants (timbre) independently of pitch using a compact
// FFT -> cepstral smoothing -> spectral envelope warp -> IFFT pipeline.
// This is what makes "Deep"/"Female" presets sound natural instead of
// just sounding like a sped-up/slowed-down tape.
class FormantShifter {
public:
    explicit FormantShifter(int32_t sampleRate, int32_t fftSize = 1024);

    // formantRatio: 1.0 = unchanged, >1.0 = smaller vocal tract (higher/female-ish),
    // <1.0 = larger vocal tract (deeper/male-ish)
    void setFormantRatio(float formantRatio);

    void process(const float* in, float* out, int32_t n);

private:
    int32_t mSampleRate;
    int32_t mFftSize;
    int32_t mHop;
    float mFormantRatio = 1.0f;

    std::vector<float> mInBuf;   // overlap-add input accumulator
    std::vector<float> mOutBuf;  // overlap-add output accumulator
    std::vector<float> mWindow;

    std::vector<std::complex<float>> mFftBuf;

    static void fft(std::vector<std::complex<float>>& a, bool inverse);
    std::vector<float> extractEnvelope(const std::vector<std::complex<float>>& spectrum);
};
