package com.airbnb.lottie;

import android.graphics.PointF;
import android.util.Log;
import java.util.Collections;
import org.json.JSONObject;

class AnimatableTransform implements ModifierContent {
    private final AnimatablePathValue anchorPoint;
    private final AnimatableIntegerValue opacity;
    private final AnimatableValue<PointF> position;
    private final AnimatableFloatValue rotation;
    private final AnimatableScaleValue scale;

    static class Factory {
        private Factory() {
        }

        static AnimatableTransform newInstance() {
            return new AnimatableTransform(new AnimatablePathValue(), new AnimatablePathValue(), AnimatableScaleValue.Factory.newInstance(), AnimatableFloatValue.Factory.newInstance(), AnimatableIntegerValue.Factory.newInstance(), null);
        }

        static AnimatableTransform newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            AnimatablePathValue animatablePathValue;
            AnimatableValue<PointF> animatableValueCreateAnimatablePathOrSplitDimensionPath;
            AnimatableFloatValue animatableFloatValueNewInstance;
            AnimatableTransform animatableTransform = null;
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("a");
            if (jSONObjectOptJSONObject != null) {
                animatablePathValue = new AnimatablePathValue(jSONObjectOptJSONObject.opt("k"), lottieComposition);
            } else {
                Log.w("LOTTIE", "Layer has no transform property. You may be using an unsupported layer type such as a camera.");
                animatablePathValue = new AnimatablePathValue();
            }
            JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("p");
            if (jSONObjectOptJSONObject2 != null) {
                animatableValueCreateAnimatablePathOrSplitDimensionPath = AnimatablePathValue.createAnimatablePathOrSplitDimensionPath(jSONObjectOptJSONObject2, lottieComposition);
            } else {
                throwMissingTransform("position");
                animatableValueCreateAnimatablePathOrSplitDimensionPath = null;
            }
            JSONObject jSONObjectOptJSONObject3 = jSONObject.optJSONObject("s");
            AnimatableScaleValue animatableScaleValueNewInstance = jSONObjectOptJSONObject3 != null ? AnimatableScaleValue.Factory.newInstance(jSONObjectOptJSONObject3, lottieComposition) : new AnimatableScaleValue(Collections.emptyList(), new ScaleXY());
            JSONObject jSONObjectOptJSONObject4 = jSONObject.optJSONObject("r");
            if (jSONObjectOptJSONObject4 == null) {
                jSONObjectOptJSONObject4 = jSONObject.optJSONObject("rz");
            }
            if (jSONObjectOptJSONObject4 != null) {
                animatableFloatValueNewInstance = AnimatableFloatValue.Factory.newInstance(jSONObjectOptJSONObject4, lottieComposition, false);
            } else {
                throwMissingTransform("rotation");
                animatableFloatValueNewInstance = null;
            }
            JSONObject jSONObjectOptJSONObject5 = jSONObject.optJSONObject("o");
            return new AnimatableTransform(animatablePathValue, animatableValueCreateAnimatablePathOrSplitDimensionPath, animatableScaleValueNewInstance, animatableFloatValueNewInstance, jSONObjectOptJSONObject5 != null ? AnimatableIntegerValue.Factory.newInstance(jSONObjectOptJSONObject5, lottieComposition) : new AnimatableIntegerValue(Collections.emptyList(), 100), animatableTransform);
        }

        private static void throwMissingTransform(String str) {
            throw new IllegalArgumentException("Missing transform for " + str);
        }
    }

    private AnimatableTransform(AnimatablePathValue animatablePathValue, AnimatableValue<PointF> animatableValue, AnimatableScaleValue animatableScaleValue, AnimatableFloatValue animatableFloatValue, AnimatableIntegerValue animatableIntegerValue) {
        this.anchorPoint = animatablePathValue;
        this.position = animatableValue;
        this.scale = animatableScaleValue;
        this.rotation = animatableFloatValue;
        this.opacity = animatableIntegerValue;
    }

     AnimatableTransform(AnimatablePathValue animatablePathValue, AnimatableValue animatableValue, AnimatableScaleValue animatableScaleValue, AnimatableFloatValue animatableFloatValue, AnimatableIntegerValue animatableIntegerValue, AnimatableTransform animatableTransform) {
        this(animatablePathValue, animatableValue, animatableScaleValue, animatableFloatValue, animatableIntegerValue);
    }

    public TransformKeyframeAnimation createAnimation() {
        return new TransformKeyframeAnimation(this);
    }

    AnimatablePathValue getAnchorPoint() {
        return this.anchorPoint;
    }

    AnimatableIntegerValue getOpacity() {
        return this.opacity;
    }

    AnimatableValue<PointF> getPosition() {
        return this.position;
    }

    AnimatableFloatValue getRotation() {
        return this.rotation;
    }

    AnimatableScaleValue getScale() {
        return this.scale;
    }
}
