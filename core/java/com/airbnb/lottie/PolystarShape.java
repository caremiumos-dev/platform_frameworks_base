package com.airbnb.lottie;

import android.graphics.PointF;
import org.json.JSONObject;

class PolystarShape {
    private final AnimatableFloatValue innerRadius;
    private final AnimatableFloatValue innerRoundedness;
    private final String name;
    private final AnimatableFloatValue outerRadius;
    private final AnimatableFloatValue outerRoundedness;
    private final AnimatableFloatValue points;
    private final AnimatableValue<PointF> position;
    private final AnimatableFloatValue rotation;
    private final PolystarShape.Type type;

    static class Factory {
        private Factory() {
        }

        static PolystarShape newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            AnimatableFloatValue animatableFloatValueNewInstance;
            AnimatableFloatValue animatableFloatValueNewInstance2;
            PolystarShape polystarShape = null;
            String strOptString = jSONObject.optString("nm");
            PolystarShape.Type typeForValue = PolystarShape.Type.forValue(jSONObject.optInt("sy"));
            AnimatableFloatValue animatableFloatValueNewInstance3 = AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("pt"), lottieComposition, false);
            AnimatableValue<PointF> animatableValueCreateAnimatablePathOrSplitDimensionPath = AnimatablePathValue.createAnimatablePathOrSplitDimensionPath(jSONObject.optJSONObject("p"), lottieComposition);
            AnimatableFloatValue animatableFloatValueNewInstance4 = AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("r"), lottieComposition, false);
            AnimatableFloatValue animatableFloatValueNewInstance5 = AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("or"), lottieComposition);
            AnimatableFloatValue animatableFloatValueNewInstance6 = AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("os"), lottieComposition, false);
            if (typeForValue == PolystarShape.Type.Star) {
                animatableFloatValueNewInstance2 = AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("ir"), lottieComposition);
                animatableFloatValueNewInstance = AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("is"), lottieComposition, false);
            } else {
                animatableFloatValueNewInstance = null;
                animatableFloatValueNewInstance2 = null;
            }
            return new PolystarShape(strOptString, typeForValue, animatableFloatValueNewInstance3, animatableValueCreateAnimatablePathOrSplitDimensionPath, animatableFloatValueNewInstance4, animatableFloatValueNewInstance2, animatableFloatValueNewInstance5, animatableFloatValueNewInstance, animatableFloatValueNewInstance6, polystarShape);
        }
    }

    enum Type {
        Star(1),
        Polygon(2);

        private final int value;

        Type(int i) {
            this.value = i;
        }

        static PolystarShape.Type forValue(int i) {
            for (PolystarShape.Type type : valuesCustom()) {
                if (type.value == i) {
                    return type;
                }
            }
            return null;
        }

        public static PolystarShape.Type[] valuesCustom() {
            return values();
        }
    }

    private PolystarShape(String str, PolystarShape.Type type, AnimatableFloatValue animatableFloatValue, AnimatableValue<PointF> animatableValue, AnimatableFloatValue animatableFloatValue2, AnimatableFloatValue animatableFloatValue3, AnimatableFloatValue animatableFloatValue4, AnimatableFloatValue animatableFloatValue5, AnimatableFloatValue animatableFloatValue6) {
        this.name = str;
        this.type = type;
        this.points = animatableFloatValue;
        this.position = animatableValue;
        this.rotation = animatableFloatValue2;
        this.innerRadius = animatableFloatValue3;
        this.outerRadius = animatableFloatValue4;
        this.innerRoundedness = animatableFloatValue5;
        this.outerRoundedness = animatableFloatValue6;
    }

     PolystarShape(String str, PolystarShape.Type type, AnimatableFloatValue animatableFloatValue, AnimatableValue animatableValue, AnimatableFloatValue animatableFloatValue2, AnimatableFloatValue animatableFloatValue3, AnimatableFloatValue animatableFloatValue4, AnimatableFloatValue animatableFloatValue5, AnimatableFloatValue animatableFloatValue6, PolystarShape polystarShape) {
        this(str, type, animatableFloatValue, animatableValue, animatableFloatValue2, animatableFloatValue3, animatableFloatValue4, animatableFloatValue5, animatableFloatValue6);
    }

    AnimatableFloatValue getInnerRadius() {
        return this.innerRadius;
    }

    AnimatableFloatValue getInnerRoundedness() {
        return this.innerRoundedness;
    }

    String getName() {
        return this.name;
    }

    AnimatableFloatValue getOuterRadius() {
        return this.outerRadius;
    }

    AnimatableFloatValue getOuterRoundedness() {
        return this.outerRoundedness;
    }

    AnimatableFloatValue getPoints() {
        return this.points;
    }

    AnimatableValue<PointF> getPosition() {
        return this.position;
    }

    AnimatableFloatValue getRotation() {
        return this.rotation;
    }

    PolystarShape.Type getType() {
        return this.type;
    }
}
