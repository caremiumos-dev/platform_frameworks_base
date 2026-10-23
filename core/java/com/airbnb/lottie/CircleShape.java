package com.airbnb.lottie;

import android.graphics.PointF;
import org.json.JSONObject;

class CircleShape {
    private final String name;
    private final AnimatableValue<PointF> position;
    private final AnimatablePointValue size;

    static class Factory {
        private Factory() {
        }

        static CircleShape newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            return new CircleShape(jSONObject.optString("nm"), AnimatablePathValue.createAnimatablePathOrSplitDimensionPath(jSONObject.optJSONObject("p"), lottieComposition), AnimatablePointValue.Factory.newInstance(jSONObject.optJSONObject("s"), lottieComposition), null);
        }
    }

    private CircleShape(String str, AnimatableValue<PointF> animatableValue, AnimatablePointValue animatablePointValue) {
        this.name = str;
        this.position = animatableValue;
        this.size = animatablePointValue;
    }

     CircleShape(String str, AnimatableValue animatableValue, AnimatablePointValue animatablePointValue, CircleShape circleShape) {
        this(str, animatableValue, animatablePointValue);
    }

    String getName() {
        return this.name;
    }

    public AnimatableValue<PointF> getPosition() {
        return this.position;
    }

    public AnimatablePointValue getSize() {
        return this.size;
    }
}
