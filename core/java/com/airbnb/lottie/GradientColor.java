package com.airbnb.lottie;

class GradientColor {
    private final int[] colors;
    private final float[] positions;

    GradientColor(float[] fArr, int[] iArr) {
        this.positions = fArr;
        this.colors = iArr;
    }

    int[] getColors() {
        return this.colors;
    }

    float[] getPositions() {
        return this.positions;
    }

    int getSize() {
        return this.colors.length;
    }

    void lerp(GradientColor gradientColor, GradientColor gradientColor2, float f) {
        if (gradientColor.colors.length != gradientColor2.colors.length) {
            throw new IllegalArgumentException("Cannot interpolate between gradients. Lengths vary (" + gradientColor.colors.length + " vs " + gradientColor2.colors.length + ")");
        }
        for (int i = 0; i < gradientColor.colors.length; i++) {
            this.positions[i] = MiscUtils.lerp(gradientColor.positions[i], gradientColor2.positions[i], f);
            this.colors[i] = GammaEvaluator.evaluate(f, gradientColor.colors[i], gradientColor2.colors[i]);
        }
    }
}
