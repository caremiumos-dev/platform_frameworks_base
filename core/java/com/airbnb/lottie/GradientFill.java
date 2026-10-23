package com.airbnb.lottie;

import android.graphics.Path.FillType;
import android.support.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

class GradientFill {
    private final AnimatablePointValue endPoint;
    private final FillType fillType;
    private final AnimatableGradientColorValue gradientColor;
    private final GradientType gradientType;

    @Nullable
    private final AnimatableFloatValue highlightAngle;

    @Nullable
    private final AnimatableFloatValue highlightLength;
    private final String name;
    private final AnimatableIntegerValue opacity;
    private final AnimatablePointValue startPoint;

    static class Factory {
        private Factory() {
        }

        static GradientFill newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            AnimatableFloatValue highlightLength = null;
            AnimatableFloatValue highlightAngle = null;
            String strOptString = jSONObject.optString("nm");
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("g");
            if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.has("k")) {
                int iOptInt = jSONObjectOptJSONObject.optInt("p");
                jSONObjectOptJSONObject = jSONObjectOptJSONObject.optJSONObject("k");
                try {
                    jSONObjectOptJSONObject.put("p", iOptInt);
                } catch (JSONException e) {
                }
            }
            AnimatableGradientColorValue animatableGradientColorValueNewInstance = jSONObjectOptJSONObject != null ? AnimatableGradientColorValue.Factory.newInstance(jSONObjectOptJSONObject, lottieComposition) : null;
            JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("o");
            AnimatableIntegerValue animatableIntegerValueNewInstance = jSONObjectOptJSONObject2 != null ? AnimatableIntegerValue.Factory.newInstance(jSONObjectOptJSONObject2, lottieComposition) : null;
            FillType fillType = jSONObject.optInt("r", 1) == 1 ? FillType.WINDING : FillType.EVEN_ODD;
            GradientType gradientType = jSONObject.optInt("t", 1) == 1 ? GradientType.Linear : GradientType.Radial;
            JSONObject jSONObjectOptJSONObject3 = jSONObject.optJSONObject("s");
            AnimatablePointValue animatablePointValueNewInstance = jSONObjectOptJSONObject3 != null ? AnimatablePointValue.Factory.newInstance(jSONObjectOptJSONObject3, lottieComposition) : null;
            JSONObject jSONObjectOptJSONObject4 = jSONObject.optJSONObject("e");
            JSONObject highlightLengthJson = jSONObject.optJSONObject("h");
            if (highlightLengthJson != null) {
                highlightLength = AnimatableFloatValue.Factory.newInstance(highlightLengthJson, lottieComposition);
            }
            JSONObject highlightAngleJson = jSONObject.optJSONObject("a");
            if (highlightAngleJson != null) {
                highlightAngle = AnimatableFloatValue.Factory.newInstance(highlightAngleJson, lottieComposition);
            }
            return new GradientFill(strOptString, gradientType, fillType, animatableGradientColorValueNewInstance, animatableIntegerValueNewInstance, animatablePointValueNewInstance, jSONObjectOptJSONObject4 != null ? AnimatablePointValue.Factory.newInstance(jSONObjectOptJSONObject4, lottieComposition) : null, highlightLength, highlightAngle);
        }
    }

    private GradientFill(String str, GradientType gradientType, FillType fillType, AnimatableGradientColorValue animatableGradientColorValue, AnimatableIntegerValue animatableIntegerValue, AnimatablePointValue animatablePointValue, AnimatablePointValue animatablePointValue2, AnimatableFloatValue animatableFloatValue, AnimatableFloatValue animatableFloatValue2) {
        this.gradientType = gradientType;
        this.fillType = fillType;
        this.gradientColor = animatableGradientColorValue;
        this.opacity = animatableIntegerValue;
        this.startPoint = animatablePointValue;
        this.endPoint = animatablePointValue2;
        this.name = str;
        this.highlightLength = animatableFloatValue;
        this.highlightAngle = animatableFloatValue2;
    }

     GradientFill(String str, GradientType gradientType, FillType fillType, AnimatableGradientColorValue animatableGradientColorValue, AnimatableIntegerValue animatableIntegerValue, AnimatablePointValue animatablePointValue, AnimatablePointValue animatablePointValue2, AnimatableFloatValue animatableFloatValue, AnimatableFloatValue animatableFloatValue2, GradientFill gradientFill) {
        this(str, gradientType, fillType, animatableGradientColorValue, animatableIntegerValue, animatablePointValue, animatablePointValue2, animatableFloatValue, animatableFloatValue2);
    }

    AnimatablePointValue getEndPoint() {
        return this.endPoint;
    }

    FillType getFillType() {
        return this.fillType;
    }

    AnimatableGradientColorValue getGradientColor() {
        return this.gradientColor;
    }

    GradientType getGradientType() {
        return this.gradientType;
    }

    @Nullable
    AnimatableFloatValue getHighlightAngle() {
        return this.highlightAngle;
    }

    @Nullable
    AnimatableFloatValue getHighlightLength() {
        return this.highlightLength;
    }

    String getName() {
        return this.name;
    }

    AnimatableIntegerValue getOpacity() {
        return this.opacity;
    }

    AnimatablePointValue getStartPoint() {
        return this.startPoint;
    }
}
