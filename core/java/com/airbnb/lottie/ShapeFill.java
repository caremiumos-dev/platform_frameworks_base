package com.airbnb.lottie;

import android.graphics.Path.FillType;
import android.support.annotation.Nullable;
import org.json.JSONObject;

class ShapeFill {

    @Nullable
    private final AnimatableColorValue color;
    private final boolean fillEnabled;
    private final FillType fillType;
    private final String name;

    @Nullable
    private final AnimatableIntegerValue opacity;

    static class Factory {
        private Factory() {
        }

        static ShapeFill newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            ShapeFill shapeFill = null;
            String strOptString = jSONObject.optString("nm");
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("c");
            AnimatableColorValue animatableColorValueNewInstance = jSONObjectOptJSONObject != null ? AnimatableColorValue.Factory.newInstance(jSONObjectOptJSONObject, lottieComposition) : null;
            JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("o");
            return new ShapeFill(strOptString, jSONObject.optBoolean("fillEnabled"), jSONObject.optInt("r", 1) == 1 ? FillType.WINDING : FillType.EVEN_ODD, animatableColorValueNewInstance, jSONObjectOptJSONObject2 != null ? AnimatableIntegerValue.Factory.newInstance(jSONObjectOptJSONObject2, lottieComposition) : null, shapeFill);
        }
    }

    private ShapeFill(String str, boolean z, FillType fillType, @Nullable AnimatableColorValue animatableColorValue, @Nullable AnimatableIntegerValue animatableIntegerValue) {
        this.name = str;
        this.fillEnabled = z;
        this.fillType = fillType;
        this.color = animatableColorValue;
        this.opacity = animatableIntegerValue;
    }

     ShapeFill(String str, boolean z, FillType fillType, AnimatableColorValue animatableColorValue, AnimatableIntegerValue animatableIntegerValue, ShapeFill shapeFill) {
        this(str, z, fillType, animatableColorValue, animatableIntegerValue);
    }

    @Nullable
    AnimatableColorValue getColor() {
        return this.color;
    }

    FillType getFillType() {
        return this.fillType;
    }

    String getName() {
        return this.name;
    }

    @Nullable
    AnimatableIntegerValue getOpacity() {
        return this.opacity;
    }

    public String toString() {
        return "ShapeFill{color=" + (this.color == null ? "null" : Integer.toHexString(this.color.getInitialValue().intValue())) + ", fillEnabled=" + this.fillEnabled + ", opacity=" + (this.opacity == null ? "null" : this.opacity.getInitialValue()) + '}';
    }
}
