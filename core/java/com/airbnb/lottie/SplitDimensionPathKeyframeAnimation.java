package com.airbnb.lottie;

import android.graphics.PointF;
import java.util.Collections;

class SplitDimensionPathKeyframeAnimation extends KeyframeAnimation<PointF> {
    private final PointF point;
    private final KeyframeAnimation<Float> xAnimation;
    private final KeyframeAnimation<Float> yAnimation;

    SplitDimensionPathKeyframeAnimation(KeyframeAnimation<Float> keyframeAnimation, KeyframeAnimation<Float> keyframeAnimation2) {
        super(Collections.emptyList());
        this.point = new PointF();
        this.xAnimation = keyframeAnimation;
        this.yAnimation = keyframeAnimation2;
    }

    @Override
    public PointF getValue() {
        return getValue((Keyframe<PointF>) null, 0.0f);
    }

    @Override
    PointF getValue(Keyframe<PointF> keyframe, float f) {
        return this.point;
    }


    @Override
    void setProgress(float f) {
        this.xAnimation.setProgress(f);
        this.yAnimation.setProgress(f);
        this.point.set(((Float) this.xAnimation.getValue()).floatValue(), ((Float) this.yAnimation.getValue()).floatValue());
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.listeners.size()) {
                return;
            }
            this.listeners.get(i2).onValueChanged();
            i = i2 + 1;
        }
    }
}
