#include "effects.h"
#include <cmath>
#include <algorithm>

// ---------------- Robot ----------------
RobotEffect::RobotEffect(int32_t sampleRate, float carrierHz)
    : mSampleRate(sampleRate), mCarrierHz(carrierHz) {}

void RobotEffect::process(const float* in, float* out, int32_t n) {
    if (!mEnabled) { std::copy(in, in + n, out); return; }
    float inc = 2.0f * (float)M_PI * mCarrierHz / (float) mSampleRate;
    for (int32_t i = 0; i < n; ++i) {
        out[i] = in[i] * sinf(mPhase);
        mPhase += inc;
        if (mPhase > 2.0f * (float)M_PI) mPhase -= 2.0f * (float)M_PI;
    }
}

// ---------------- Echo ----------------
EchoEffect::EchoEffect(int32_t sampleRate, float delayMs) : mSampleRate(sampleRate) {
    setDelayMs(delayMs);
}

void EchoEffect::setDelayMs(float ms) {
    int32_t size = std::max(1, (int32_t)(mSampleRate * ms / 1000.0f));
    mBuffer.assign(size, 0.0f);
    mWritePos = 0;
}

void EchoEffect::process(const float* in, float* out, int32_t n) {
    if (mWet <= 0.001f || mBuffer.empty()) { std::copy(in, in + n, out); return; }
    int32_t size = (int32_t) mBuffer.size();
    for (int32_t i = 0; i < n; ++i) {
        float delayed = mBuffer[mWritePos];
        float inSample = in[i];
        mBuffer[mWritePos] = inSample + delayed * mFeedback;
        out[i] = inSample * (1.0f - mWet) + delayed * mWet;
        mWritePos = (mWritePos + 1) % size;
    }
}

// ---------------- Reverb ----------------
ReverbEffect::ReverbEffect(int32_t sampleRate) : mSampleRate(sampleRate) {
    // Classic Schroeder comb delays (ms), scaled to sample rate.
    const float combMs[4]     = {29.7f, 37.1f, 41.1f, 43.7f};
    const float combFeedback[4] = {0.805f, 0.827f, 0.783f, 0.764f};
    for (int i = 0; i < 4; ++i) {
        Comb c;
        c.buf.assign(std::max(1, (int32_t)(sampleRate * combMs[i] / 1000.0f)), 0.0f);
        c.feedback = combFeedback[i];
        mCombs.push_back(std::move(c));
    }
    const float apMs[2] = {5.0f, 1.7f};
    for (int i = 0; i < 2; ++i) {
        AllPass ap;
        ap.buf.assign(std::max(1, (int32_t)(sampleRate * apMs[i] / 1000.0f)), 0.0f);
        mAllpasses.push_back(std::move(ap));
    }
}

void ReverbEffect::process(const float* in, float* out, int32_t n) {
    if (mWet <= 0.001f) { std::copy(in, in + n, out); return; }
    for (int32_t i = 0; i < n; ++i) {
        float x = in[i];
        float combSum = 0.0f;
        for (auto& c : mCombs) {
            float delayed = c.buf[c.pos];
            c.buf[c.pos] = x + delayed * c.feedback;
            combSum += delayed;
            c.pos = (c.pos + 1) % (int32_t) c.buf.size();
        }
        combSum *= 0.25f;

        float ap = combSum;
        for (auto& a : mAllpasses) {
            float bufOut = a.buf[a.pos];
            float apOut = -ap + bufOut;
            a.buf[a.pos] = ap + bufOut * 0.5f;
            a.pos = (a.pos + 1) % (int32_t) a.buf.size();
            ap = apOut;
        }
        out[i] = x * (1.0f - mWet) + ap * mWet;
    }
}
