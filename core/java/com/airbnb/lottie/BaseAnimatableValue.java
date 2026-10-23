package com.airbnb.lottie;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

abstract class BaseAnimatableValue<V, O> implements AnimatableValue<O> {
    final V initialValue;
    final List<Keyframe<V>> keyframes;

    BaseAnimatableValue(V v) {
        this(Collections.emptyList(), v);
    }

    BaseAnimatableValue(List<Keyframe<V>> list, V v) {
        this.keyframes = list;
        this.initialValue = v;
    }

    @SuppressWarnings("unchecked")
    O convertType(V v) {
        return (O) v;
    }

    public O getInitialValue() {
        return convertType(this.initialValue);
    }

    @Override
    public boolean hasAnimation() {
        return !this.keyframes.isEmpty();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("parseInitialValue=").append(this.initialValue);
        if (!this.keyframes.isEmpty()) {
            sb.append(", values=").append(Arrays.toString(this.keyframes.toArray()));
        }
        return sb.toString();
    }
}
