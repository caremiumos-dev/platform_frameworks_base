package com.airbnb.lottie;

class GammaEvaluator {
    GammaEvaluator() {
    }

    private static float EOCF_sRGB(float f) {
        return f <= 0.04045f ? f / 12.92f : (float) Math.pow((0.055f + f) / 1.055f, 2.4000000953674316d);
    }

    private static float OECF_sRGB(float f) {
        return f <= 0.0031308f ? 12.92f * f : (float) ((Math.pow(f, 0.4166666567325592d) * 1.0549999475479126d) - 0.054999999701976776d);
    }

    static int evaluate(float f, int i, int i2) {
        float f2 = ((i >> 24) & 255) / 255.0f;
        float fEOCF_sRGB = EOCF_sRGB(((i >> 16) & 255) / 255.0f);
        float fEOCF_sRGB2 = EOCF_sRGB(((i >> 8) & 255) / 255.0f);
        float fEOCF_sRGB3 = EOCF_sRGB((i & 255) / 255.0f);
        return (Math.round((f2 + (((((i2 >> 24) & 255) / 255.0f) - f2) * f)) * 255.0f) << 24) | (Math.round(OECF_sRGB(fEOCF_sRGB + ((EOCF_sRGB(((i2 >> 16) & 255) / 255.0f) - fEOCF_sRGB) * f)) * 255.0f) << 16) | (Math.round(OECF_sRGB(fEOCF_sRGB2 + ((EOCF_sRGB(((i2 >> 8) & 255) / 255.0f) - fEOCF_sRGB2) * f)) * 255.0f) << 8) | Math.round(OECF_sRGB(fEOCF_sRGB3 + ((EOCF_sRGB((i2 & 255) / 255.0f) - fEOCF_sRGB3) * f)) * 255.0f);
    }
}
