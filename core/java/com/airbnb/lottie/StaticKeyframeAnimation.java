package com.airbnb.lottie;

import android.support.annotation.FloatRange;
import java.util.Collections;

class StaticKeyframeAnimation<T> extends KeyframeAnimation<T> {
    private final T initialValue;

    StaticKeyframeAnimation(T t) {
        super(Collections.emptyList());
        this.initialValue = t;
    }

    @Override
    void addUpdateListener(BaseKeyframeAnimation.AnimationListener animationListener) {
    }

    @Override
    public T getValue() {
        return this.initialValue;
    }

    @Override
    public T getValue(Keyframe<T> keyframe, float f) {
        return this.initialValue;
    }

    @Override
    public void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float f) {
    }
}
