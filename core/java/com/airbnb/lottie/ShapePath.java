package com.airbnb.lottie;

import org.json.JSONObject;

class ShapePath {
    private final int index;
    private final String name;
    private final AnimatableShapeValue shapePath;

    static class Factory {
        private Factory() {
        }

        static ShapePath newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            return new ShapePath(jSONObject.optString("nm"), jSONObject.optInt("ind"), AnimatableShapeValue.Factory.newInstance(jSONObject.optJSONObject("ks"), lottieComposition), null);
        }
    }

    private ShapePath(String str, int i, AnimatableShapeValue animatableShapeValue) {
        this.name = str;
        this.index = i;
        this.shapePath = animatableShapeValue;
    }

     ShapePath(String str, int i, AnimatableShapeValue animatableShapeValue, ShapePath shapePath) {
        this(str, i, animatableShapeValue);
    }

    public String getName() {
        return this.name;
    }

    AnimatableShapeValue getShapePath() {
        return this.shapePath;
    }

    public String toString() {
        return "ShapePath{name=" + this.name + ", index=" + this.index + ", hasAnimation=" + this.shapePath.hasAnimation() + '}';
    }
}
