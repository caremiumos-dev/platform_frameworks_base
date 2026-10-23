package com.airbnb.lottie;

import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class GradientStroke {
    private final ShapeStroke.LineCapType capType;

    @Nullable
    private final AnimatableFloatValue dashOffset;
    private final AnimatablePointValue endPoint;
    private final AnimatableGradientColorValue gradientColor;
    private final GradientType gradientType;
    private final ShapeStroke.LineJoinType joinType;
    private final List<AnimatableFloatValue> lineDashPattern;
    private final String name;
    private final AnimatableIntegerValue opacity;
    private final AnimatablePointValue startPoint;
    private final AnimatableFloatValue width;

    static class Factory {
        private Factory() {
        }

        static GradientStroke newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            String strOptString = jSONObject.optString("nm");
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("g");
            if (jSONObjectOptJSONObject != null && jSONObjectOptJSONObject.has("k")) {
                jSONObjectOptJSONObject = jSONObjectOptJSONObject.optJSONObject("k");
            }
            AnimatableGradientColorValue animatableGradientColorValueNewInstance = jSONObjectOptJSONObject != null ? AnimatableGradientColorValue.Factory.newInstance(jSONObjectOptJSONObject, lottieComposition) : null;
            JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("o");
            AnimatableIntegerValue animatableIntegerValueNewInstance = jSONObjectOptJSONObject2 != null ? AnimatableIntegerValue.Factory.newInstance(jSONObjectOptJSONObject2, lottieComposition) : null;
            GradientType gradientType = jSONObject.optInt("t", 1) == 1 ? GradientType.Linear : GradientType.Radial;
            JSONObject jSONObjectOptJSONObject3 = jSONObject.optJSONObject("s");
            AnimatablePointValue animatablePointValueNewInstance = jSONObjectOptJSONObject3 != null ? AnimatablePointValue.Factory.newInstance(jSONObjectOptJSONObject3, lottieComposition) : null;
            JSONObject jSONObjectOptJSONObject4 = jSONObject.optJSONObject("e");
            AnimatablePointValue animatablePointValueNewInstance2 = jSONObjectOptJSONObject4 != null ? AnimatablePointValue.Factory.newInstance(jSONObjectOptJSONObject4, lottieComposition) : null;
            AnimatableFloatValue animatableFloatValueNewInstance = AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("w"), lottieComposition);
            ShapeStroke.LineCapType lineCapType = ShapeStroke.LineCapType.valuesCustom()[jSONObject.optInt("lc") - 1];
            ShapeStroke.LineJoinType lineJoinType = ShapeStroke.LineJoinType.valuesCustom()[jSONObject.optInt("lj") - 1];
            AnimatableFloatValue animatableFloatValueNewInstance2 = null;
            ArrayList arrayList = new ArrayList();
            if (jSONObject.has("d")) {
                JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("d");
                for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                    JSONObject jSONObjectOptJSONObject5 = jSONArrayOptJSONArray.optJSONObject(i);
                    String strOptString2 = jSONObjectOptJSONObject5.optString("n");
                    if (strOptString2.equals("o")) {
                        animatableFloatValueNewInstance2 = AnimatableFloatValue.Factory.newInstance(jSONObjectOptJSONObject5.optJSONObject("v"), lottieComposition);
                    } else if (strOptString2.equals("d") || strOptString2.equals("g")) {
                        arrayList.add(AnimatableFloatValue.Factory.newInstance(jSONObjectOptJSONObject5.optJSONObject("v"), lottieComposition));
                    }
                }
                if (arrayList.size() == 1) {
                    arrayList.add((AnimatableFloatValue) arrayList.get(0));
                }
            }
            return new GradientStroke(strOptString, gradientType, animatableGradientColorValueNewInstance, animatableIntegerValueNewInstance, animatablePointValueNewInstance, animatablePointValueNewInstance2, animatableFloatValueNewInstance, lineCapType, lineJoinType, arrayList, animatableFloatValueNewInstance2, null);
        }
    }

    private GradientStroke(String str, GradientType gradientType, AnimatableGradientColorValue animatableGradientColorValue, AnimatableIntegerValue animatableIntegerValue, AnimatablePointValue animatablePointValue, AnimatablePointValue animatablePointValue2, AnimatableFloatValue animatableFloatValue, ShapeStroke.LineCapType lineCapType, ShapeStroke.LineJoinType lineJoinType, List<AnimatableFloatValue> list, @Nullable AnimatableFloatValue animatableFloatValue2) {
        this.name = str;
        this.gradientType = gradientType;
        this.gradientColor = animatableGradientColorValue;
        this.opacity = animatableIntegerValue;
        this.startPoint = animatablePointValue;
        this.endPoint = animatablePointValue2;
        this.width = animatableFloatValue;
        this.capType = lineCapType;
        this.joinType = lineJoinType;
        this.lineDashPattern = list;
        this.dashOffset = animatableFloatValue2;
    }

     GradientStroke(String str, GradientType gradientType, AnimatableGradientColorValue animatableGradientColorValue, AnimatableIntegerValue animatableIntegerValue, AnimatablePointValue animatablePointValue, AnimatablePointValue animatablePointValue2, AnimatableFloatValue animatableFloatValue, ShapeStroke.LineCapType lineCapType, ShapeStroke.LineJoinType lineJoinType, List list, AnimatableFloatValue animatableFloatValue2, GradientStroke gradientStroke) {
        this(str, gradientType, animatableGradientColorValue, animatableIntegerValue, animatablePointValue, animatablePointValue2, animatableFloatValue, lineCapType, lineJoinType, list, animatableFloatValue2);
    }

    ShapeStroke.LineCapType getCapType() {
        return this.capType;
    }

    @Nullable
    AnimatableFloatValue getDashOffset() {
        return this.dashOffset;
    }

    AnimatablePointValue getEndPoint() {
        return this.endPoint;
    }

    AnimatableGradientColorValue getGradientColor() {
        return this.gradientColor;
    }

    GradientType getGradientType() {
        return this.gradientType;
    }

    ShapeStroke.LineJoinType getJoinType() {
        return this.joinType;
    }

    List<AnimatableFloatValue> getLineDashPattern() {
        return this.lineDashPattern;
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

    AnimatableFloatValue getWidth() {
        return this.width;
    }
}
