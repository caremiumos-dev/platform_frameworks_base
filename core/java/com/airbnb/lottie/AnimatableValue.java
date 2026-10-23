package com.airbnb.lottie;

interface AnimatableValue<O> {

    public interface Factory<V> {
        V valueFromObject(Object obj, float f);
    }

    BaseKeyframeAnimation<?, O> createAnimation();

    boolean hasAnimation();
}
