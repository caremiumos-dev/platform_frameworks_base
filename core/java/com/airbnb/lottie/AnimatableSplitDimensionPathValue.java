package com.airbnb.lottie;

import android.graphics.PointF;

class AnimatableSplitDimensionPathValue implements AnimatableValue<PointF> {
    private final AnimatableFloatValue animatableXDimension;
    private final AnimatableFloatValue animatableYDimension;

    AnimatableSplitDimensionPathValue(AnimatableFloatValue animatableFloatValue, AnimatableFloatValue animatableFloatValue2) {
        this.animatableXDimension = animatableFloatValue;
        this.animatableYDimension = animatableFloatValue2;
    }

    @Override
    public BaseKeyframeAnimation<?, PointF> createAnimation() {
        return new SplitDimensionPathKeyframeAnimation(this.animatableXDimension.createAnimation(), this.animatableYDimension.createAnimation());
    }

    @Override
    public boolean hasAnimation() {
        if (this.animatableXDimension.hasAnimation()) {
            return true;
        }
        return this.animatableYDimension.hasAnimation();
    }
}
