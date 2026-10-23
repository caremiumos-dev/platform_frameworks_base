package com.airbnb.lottie;

import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

abstract class BaseKeyframeAnimation<K, A> {

    @Nullable
    private Keyframe<K> cachedKeyframe;
    private final List<? extends Keyframe<K>> keyframes;
    final List<BaseKeyframeAnimation.AnimationListener> listeners = new ArrayList();
    private boolean isDiscrete = false;
    private float progress = 0.0f;

    interface AnimationListener {
        void onValueChanged();
    }

    BaseKeyframeAnimation(List<? extends Keyframe<K>> list) {
        this.keyframes = list;
    }

    private Keyframe<K> getCurrentKeyframe() {
        if (this.keyframes.isEmpty()) {
            throw new IllegalStateException("There are no keyframes");
        }
        if (this.cachedKeyframe != null && this.cachedKeyframe.containsProgress(this.progress)) {
            return this.cachedKeyframe;
        }
        Keyframe<K> keyframe = this.keyframes.get(0);
        if (this.progress < keyframe.getStartProgress()) {
            this.cachedKeyframe = keyframe;
            return keyframe;
        }
        for (int i = 0; !keyframe.containsProgress(this.progress) && i < this.keyframes.size(); i++) {
            keyframe = this.keyframes.get(i);
        }
        this.cachedKeyframe = keyframe;
        return keyframe;
    }

    private float getCurrentKeyframeProgress() {
        if (this.isDiscrete) {
            return 0.0f;
        }
        Keyframe<K> currentKeyframe = getCurrentKeyframe();
        if (currentKeyframe.isStatic()) {
            return 0.0f;
        }
        return currentKeyframe.interpolator.getInterpolation((this.progress - currentKeyframe.getStartProgress()) / (currentKeyframe.getEndProgress() - currentKeyframe.getStartProgress()));
    }

    @FloatRange(from = 0.0f, to = 1.0f)
    private float getEndProgress() {
        if (this.keyframes.isEmpty()) {
            return 1.0f;
        }
        return this.keyframes.get(this.keyframes.size() - 1).getEndProgress();
    }

    @FloatRange(from = 0.0f, to = 1.0f)
    private float getStartDelayProgress() {
        if (this.keyframes.isEmpty()) {
            return 0.0f;
        }
        return this.keyframes.get(0).getStartProgress();
    }

    void addUpdateListener(BaseKeyframeAnimation.AnimationListener animationListener) {
        this.listeners.add(animationListener);
    }

    float getProgress() {
        return this.progress;
    }

    public A getValue() {
        return getValue(getCurrentKeyframe(), getCurrentKeyframeProgress());
    }

    abstract A getValue(Keyframe<K> keyframe, float f);

    void setIsDiscrete() {
        this.isDiscrete = true;
    }

    void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float f) {
        if (f < getStartDelayProgress()) {
            f = 0.0f;
        } else if (f > getEndProgress()) {
            f = 1.0f;
        }
        if (f == this.progress) {
            return;
        }
        this.progress = f;
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
