package com.airbnb.lottie;

import java.util.List;

class ScaleKeyframeAnimation extends KeyframeAnimation<ScaleXY> {
    ScaleKeyframeAnimation(List<Keyframe<ScaleXY>> list) {
        super(list);
    }

    @Override
    public ScaleXY getValue(Keyframe<ScaleXY> keyframe, float f) {
        if (keyframe.startValue == null || keyframe.endValue == null) {
            throw new IllegalStateException("Missing values for keyframe.");
        }
        ScaleXY scaleXY = keyframe.startValue;
        ScaleXY scaleXY2 = keyframe.endValue;
        return new ScaleXY(MiscUtils.lerp(scaleXY.getScaleX(), scaleXY2.getScaleX(), f), MiscUtils.lerp(scaleXY.getScaleY(), scaleXY2.getScaleY(), f));
    }

}
