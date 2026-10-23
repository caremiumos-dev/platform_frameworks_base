package com.airbnb.lottie;

import java.util.List;

public abstract class KeyframeAnimation<T> extends BaseKeyframeAnimation<T, T> {
    KeyframeAnimation(List<? extends Keyframe<T>> list) {
        super(list);
    }

    @Override
    public T getValue() {
        return super.getValue();
    }
}
