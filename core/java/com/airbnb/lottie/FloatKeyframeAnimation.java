package com.airbnb.lottie;

import java.util.List;

class FloatKeyframeAnimation extends KeyframeAnimation<Float> {
    FloatKeyframeAnimation(List<Keyframe<Float>> list) {
        super(list);
    }

    @Override
    Float getValue(Keyframe<Float> keyframe, float f) {
        if (keyframe.startValue == null || keyframe.endValue == null) {
            throw new IllegalStateException("Missing values for keyframe.");
        }
        return Float.valueOf(MiscUtils.lerp(keyframe.startValue.floatValue(), keyframe.endValue.floatValue(), f));
    }

}
