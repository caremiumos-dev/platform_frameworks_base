package com.airbnb.lottie;

import android.graphics.PointF;
import org.json.JSONObject;

class RectangleShape {
    private final AnimatableFloatValue cornerRadius;
    private final String name;
    private final AnimatableValue<PointF> position;
    private final AnimatablePointValue size;

    static class Factory {
        private Factory() {
        }

        static RectangleShape newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            return new RectangleShape(jSONObject.optString("nm"), AnimatablePathValue.createAnimatablePathOrSplitDimensionPath(jSONObject.optJSONObject("p"), lottieComposition), AnimatablePointValue.Factory.newInstance(jSONObject.optJSONObject("s"), lottieComposition), AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("r"), lottieComposition), null);
        }
    }

    private RectangleShape(String str, AnimatableValue<PointF> animatableValue, AnimatablePointValue animatablePointValue, AnimatableFloatValue animatableFloatValue) {
        this.name = str;
        this.position = animatableValue;
        this.size = animatablePointValue;
        this.cornerRadius = animatableFloatValue;
    }

     RectangleShape(String str, AnimatableValue animatableValue, AnimatablePointValue animatablePointValue, AnimatableFloatValue animatableFloatValue, RectangleShape rectangleShape) {
        this(str, animatableValue, animatablePointValue, animatableFloatValue);
    }

    AnimatableFloatValue getCornerRadius() {
        return this.cornerRadius;
    }

    String getName() {
        return this.name;
    }

    AnimatableValue<PointF> getPosition() {
        return this.position;
    }

    AnimatablePointValue getSize() {
        return this.size;
    }

    public String toString() {
        return "RectangleShape{cornerRadius=" + this.cornerRadius.getInitialValue() + ", position=" + this.position + ", size=" + this.size + '}';
    }
}
