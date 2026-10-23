package com.airbnb.lottie;

import java.util.List;

class ColorKeyframeAnimation extends KeyframeAnimation<Integer> {
    ColorKeyframeAnimation(List<Keyframe<Integer>> list) {
        super(list);
    }

    @Override
    public Integer getValue(Keyframe<Integer> keyframe, float f) {
        if (keyframe.startValue == null || keyframe.endValue == null) {
            throw new IllegalStateException("Missing values for keyframe.");
        }
        return Integer.valueOf(GammaEvaluator.evaluate(f, keyframe.startValue.intValue(), keyframe.endValue.intValue()));
    }

}
