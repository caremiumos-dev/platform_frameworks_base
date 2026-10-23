package com.airbnb.lottie;

import org.json.JSONArray;

class ScaleXY {
    private final float scaleX;
    private final float scaleY;

    static class Factory implements AnimatableValue.Factory<ScaleXY> {
        static final ScaleXY.Factory INSTANCE = new ScaleXY.Factory();

        private Factory() {
        }

        @Override
        public ScaleXY valueFromObject(Object obj, float f) {
            JSONArray jSONArray = (JSONArray) obj;
            return new ScaleXY((((float) jSONArray.optDouble(0, 1.0d)) / 100.0f) * f, (((float) jSONArray.optDouble(1, 1.0d)) / 100.0f) * f);
        }
    }

    ScaleXY() {
        this(1.0f, 1.0f);
    }

    ScaleXY(float f, float f2) {
        this.scaleX = f;
        this.scaleY = f2;
    }

    float getScaleX() {
        return this.scaleX;
    }

    float getScaleY() {
        return this.scaleY;
    }

    public String toString() {
        return getScaleX() + "x" + getScaleY();
    }
}
