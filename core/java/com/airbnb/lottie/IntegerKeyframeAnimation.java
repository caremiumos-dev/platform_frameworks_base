package com.airbnb.lottie;

import java.util.List;

class IntegerKeyframeAnimation extends KeyframeAnimation<Integer> {
    IntegerKeyframeAnimation(List<Keyframe<Integer>> list) {
        super(list);
    }

    @Override
    Integer getValue(Keyframe<Integer> keyframe, float f) {
        if (keyframe.startValue == null || keyframe.endValue == null) {
            throw new IllegalStateException("Missing values for keyframe.");
        }
        return Integer.valueOf(MiscUtils.lerp(keyframe.startValue.intValue(), keyframe.endValue.intValue(), f));
    }

}
