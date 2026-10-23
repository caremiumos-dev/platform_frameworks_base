package com.airbnb.lottie;

import android.graphics.Matrix;
import android.graphics.PointF;

class TransformKeyframeAnimation {
    private final BaseKeyframeAnimation<?, PointF> anchorPoint;
    private final Matrix matrix = new Matrix();
    private final BaseKeyframeAnimation<?, Integer> opacity;
    private final BaseKeyframeAnimation<?, PointF> position;
    private final BaseKeyframeAnimation<?, Float> rotation;
    private final BaseKeyframeAnimation<?, ScaleXY> scale;

    TransformKeyframeAnimation(AnimatableTransform animatableTransform) {
        this.anchorPoint = animatableTransform.getAnchorPoint().createAnimation();
        this.position = animatableTransform.getPosition().createAnimation();
        this.scale = animatableTransform.getScale().createAnimation();
        this.rotation = animatableTransform.getRotation().createAnimation();
        this.opacity = animatableTransform.getOpacity().createAnimation();
    }

    void addAnimationsToLayer(BaseLayer baseLayer) {
        baseLayer.addAnimation(this.anchorPoint);
        baseLayer.addAnimation(this.position);
        baseLayer.addAnimation(this.scale);
        baseLayer.addAnimation(this.rotation);
        baseLayer.addAnimation(this.opacity);
    }

    void addListener(BaseKeyframeAnimation.AnimationListener animationListener) {
        this.anchorPoint.addUpdateListener(animationListener);
        this.position.addUpdateListener(animationListener);
        this.scale.addUpdateListener(animationListener);
        this.rotation.addUpdateListener(animationListener);
        this.opacity.addUpdateListener(animationListener);
    }

    Matrix getMatrix() {
        this.matrix.reset();
        PointF value = this.position.getValue();
        if (value.x != 0.0f || value.y != 0.0f) {
            this.matrix.preTranslate(value.x, value.y);
        }
        float fFloatValue = this.rotation.getValue().floatValue();
        if (fFloatValue != 0.0f) {
            this.matrix.preRotate(fFloatValue);
        }
        ScaleXY value2 = this.scale.getValue();
        if (value2.getScaleX() != 1.0f || value2.getScaleY() != 1.0f) {
            this.matrix.preScale(value2.getScaleX(), value2.getScaleY());
        }
        PointF value3 = this.anchorPoint.getValue();
        if (value3.x != 0.0f || value3.y != 0.0f) {
            this.matrix.preTranslate(-value3.x, -value3.y);
        }
        return this.matrix;
    }

    BaseKeyframeAnimation<?, Integer> getOpacity() {
        return this.opacity;
    }
}
