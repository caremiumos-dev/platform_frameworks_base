package com.airbnb.lottie;

import java.util.List;

class GradientColorKeyframeAnimation extends KeyframeAnimation<GradientColor> {
    private final GradientColor gradientColor;

    GradientColorKeyframeAnimation(List<? extends Keyframe<GradientColor>> list) {
        super(list);
        GradientColor gradientColor = list.get(0).startValue;
        int size = gradientColor == null ? 0 : gradientColor.getSize();
        this.gradientColor = new GradientColor(new float[size], new int[size]);
    }

    @Override
    GradientColor getValue(Keyframe<GradientColor> keyframe, float f) {
        this.gradientColor.lerp(keyframe.startValue, keyframe.endValue, f);
        return this.gradientColor;
    }

}
