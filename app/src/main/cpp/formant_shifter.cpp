#include "formant_shifter.h"
#include <cmath>
#include <algorithm>

FormantShifter::FormantShifter(int32_t sampleRate, int32_t fftSize)
    : mSampleRate(sampleRate), mFftSize(fftSize) {
    mHop = mFftSize / 4;
    mInBuf.assign(mFftSize, 0.0f);
    mOutBuf.assign(mFftSize * 2, 0.0f);
    mWindow.resize(mFftSize);
    for (int32_t i = 0; i < mFftSize; ++i) {
        mWindow[i] = 0.5f * (1.0f - cosf(2.0f * (float)M_PI * i / (mFftSize - 1)));
    }
    mFftBuf.resize(mFftSize);
}

void FormantShifter::setFormantRatio(float formantRatio) {
    mFormantRatio = std::clamp(formantRatio, 0.5f, 2.0f);
}

// Iterative radix-2 Cooley-Tukey. mFftSize must be a power of two.
void FormantShifter::fft(std::vector<std::complex<float>>& a, bool inverse) {
    const size_t n = a.size();
    for (size_t i = 1, j = 0; i < n; ++i) {
        size_t bit = n >> 1;
        for (; j & bit; bit >>= 1) j ^= bit;
        j ^= bit;
        if (i < j) std::swap(a[i], a[j]);
    }
    for (size_t len = 2; len <= n; len <<= 1) {
        float ang = (float)(2.0 * M_PI / len) * (inverse ? 1.0f : -1.0f);
        std::complex<float> wlen(cosf(ang), sinf(ang));
        for (size_t i = 0; i < n; i += len) {
            std::complex<float> w(1.0f, 0.0f);
            for (size_t k = 0; k < len / 2; ++k) {
                auto u = a[i + k];
                auto v = a[i + k + len / 2] * w;
                a[i + k] = u + v;
                a[i + k + len / 2] = u - v;
                w *= wlen;
            }
        }
    }
    if (inverse) {
        for (auto& x : a) x /= (float) n;
    }
}

std::vector<float> FormantShifter::extractEnvelope(const std::vector<std::complex<float>>& spectrum) {
    // Cepstral smoothing: log-magnitude -> IFFT -> keep low quefrency
    // (vocal tract shape) -> FFT back -> smooth envelope in dB.
    std::vector<std::complex<float>> logMag(mFftSize);
    for (int32_t i = 0; i < mFftSize; ++i) {
        float mag = std::abs(spectrum[i]) + 1e-6f;
        logMag[i] = std::complex<float>(logf(mag), 0.0f);
    }
    fft(logMag, true); // to "cepstrum"
    int32_t lifterCutoff = mFftSize / 32; // keep only slow-varying envelope info
    for (int32_t i = lifterCutoff; i < mFftSize - lifterCutoff; ++i) {
        logMag[i] = 0.0f;
    }
    fft(logMag, false); // back to smoothed log-magnitude envelope
    std::vector<float> env(mFftSize);
    for (int32_t i = 0; i < mFftSize; ++i) env[i] = expf(logMag[i].real());
    return env;
}

void FormantShifter::process(const float* in, float* out, int32_t n) {
    if (std::fabs(mFormantRatio - 1.0f) < 0.01f) {
        std::copy(in, in + n, out);
        return;
    }

    // NOTE: simplified block-synchronous STFT (block size == fftSize here for
    // MVP clarity/performance on-device). Production build should use a
    // proper hop-based ring buffer; see README "improve quality" section.
    int32_t count = std::min(n, mFftSize);
    for (int32_t i = 0; i < count; ++i) mFftBuf[i] = std::complex<float>(in[i] * mWindow[i], 0.0f);
    for (int32_t i = count; i < mFftSize; ++i) mFftBuf[i] = 0.0f;

    fft(mFftBuf, false);
    auto envelope = extractEnvelope(mFftBuf);

    // Warp the spectral envelope by formantRatio (resample envelope on the
    // frequency axis) while keeping the excitation (pitch) spectrum as-is.
    std::vector<std::complex<float>> shifted(mFftSize);
    for (int32_t i = 0; i < mFftSize / 2; ++i) {
        float srcBin = i / mFormantRatio;
        int32_t b0 = std::clamp((int32_t) srcBin, 0, mFftSize / 2 - 1);
        int32_t b1 = std::clamp(b0 + 1, 0, mFftSize / 2 - 1);
        float frac = srcBin - b0;
        float envAtI = envelope[b0] * (1 - frac) + envelope[b1] * frac;
        float gain = envAtI / (envelope[i] + 1e-6f);
        shifted[i] = mFftBuf[i] * gain;
        if (i > 0) shifted[mFftSize - i] = std::conj(shifted[i]);
    }

    fft(shifted, true);
    for (int32_t i = 0; i < count; ++i) {
        out[i] = shifted[i].real();
    }
    for (int32_t i = count; i < n; ++i) out[i] = in[i];
}
