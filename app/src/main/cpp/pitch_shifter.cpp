#include "pitch_shifter.h"
#include <cmath>
#include <algorithm>

PitchShifter::PitchShifter(int32_t sampleRate) : mSampleRate(sampleRate) {
    mRingSize = kGrainSize * 8;
    mRing.assign(mRingSize, 0.0f);

    mWindow.resize(kGrainSize);
    for (int32_t i = 0; i < kGrainSize; ++i) {
        mWindow[i] = 0.5f * (1.0f - cosf(2.0f * (float)M_PI * i / (kGrainSize - 1)));
    }
}

void PitchShifter::setPitchRatio(float pitchRatio) {
    mPitchRatio = std::clamp(pitchRatio, 0.5f, 2.0f);
}

float PitchShifter::readInterpolated(double pos) const {
    int32_t i0 = ((int32_t) std::floor(pos)) % mRingSize;
    if (i0 < 0) i0 += mRingSize;
    int32_t i1 = (i0 + 1) % mRingSize;
    float frac = (float)(pos - std::floor(pos));
    return mRing[i0] * (1.0f - frac) + mRing[i1] * frac;
}

void PitchShifter::process(const float* in, float* out, int32_t n) {
    if (std::fabs(mPitchRatio - 1.0f) < 0.001f) {
        // Bypass fast-path
        std::copy(in, in + n, out);
        return;
    }

    for (int32_t i = 0; i < n; ++i) {
        // Write incoming sample into the ring buffer
        mRing[mWritePos] = in[i];
        mWritePos = (mWritePos + 1) % mRingSize;

        // Two-grain overlap-add read, spaced half a grain apart, each
        // windowed with Hann to crossfade smoothly -> avoids the
        // "granular buzz" of a naive single-tap resampler.
        double posA = mReadPos;
        double posB = mReadPos + kGrainSize / 2.0;

        double distToWrite = std::fmod((mWritePos - posA) + mRingSize, (double) mRingSize);
        double phase = std::fmod(distToWrite, kGrainSize / 2.0) / (kGrainSize / 2.0);

        float sampleA = readInterpolated(posA);
        float sampleB = readInterpolated(posB);

        float wA = 0.5f * (1.0f - cosf((float)(2.0 * M_PI * phase)));
        float wB = 1.0f - wA;

        out[i] = sampleA * wA + sampleB * wB;

        // Advance the read pointer by pitchRatio samples per output sample:
        // ratio > 1 => read faster than write => pitch up; < 1 => pitch down.
        mReadPos += mPitchRatio;
        double maxLead = mRingSize - kGrainSize;
        if (mReadPos > mWritePos) mReadPos -= mRingSize;
        if (mReadPos < mWritePos - maxLead) mReadPos = mWritePos - maxLead;
    }
}
