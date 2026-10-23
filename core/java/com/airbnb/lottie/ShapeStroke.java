package com.airbnb.lottie;

import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class ShapeStroke {
    private final ShapeStroke.LineCapType capType;
    private final AnimatableColorValue color;
    private final ShapeStroke.LineJoinType joinType;
    private final List<AnimatableFloatValue> lineDashPattern;
    private final String name;

    @Nullable
    private final AnimatableFloatValue offset;
    private final AnimatableIntegerValue opacity;
    private final AnimatableFloatValue width;

    static class Factory {
        private Factory() {
        }

        static ShapeStroke newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            String strOptString = jSONObject.optString("nm");
            ArrayList arrayList = new ArrayList();
            AnimatableColorValue animatableColorValueNewInstance = AnimatableColorValue.Factory.newInstance(jSONObject.optJSONObject("c"), lottieComposition);
            AnimatableFloatValue animatableFloatValueNewInstance = AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("w"), lottieComposition);
            AnimatableIntegerValue animatableIntegerValueNewInstance = AnimatableIntegerValue.Factory.newInstance(jSONObject.optJSONObject("o"), lottieComposition);
            ShapeStroke.LineCapType lineCapType = ShapeStroke.LineCapType.valuesCustom()[jSONObject.optInt("lc") - 1];
            ShapeStroke.LineJoinType lineJoinType = ShapeStroke.LineJoinType.valuesCustom()[jSONObject.optInt("lj") - 1];
            AnimatableFloatValue animatableFloatValueNewInstance2 = null;
            if (jSONObject.has("d")) {
                JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("d");
                for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                    JSONObject jSONObjectOptJSONObject = jSONArrayOptJSONArray.optJSONObject(i);
                    String strOptString2 = jSONObjectOptJSONObject.optString("n");
                    if (strOptString2.equals("o")) {
                        animatableFloatValueNewInstance2 = AnimatableFloatValue.Factory.newInstance(jSONObjectOptJSONObject.optJSONObject("v"), lottieComposition);
                    } else if (strOptString2.equals("d") || strOptString2.equals("g")) {
                        arrayList.add(AnimatableFloatValue.Factory.newInstance(jSONObjectOptJSONObject.optJSONObject("v"), lottieComposition));
                    }
                }
                if (arrayList.size() == 1) {
                    arrayList.add((AnimatableFloatValue) arrayList.get(0));
                }
            }
            return new ShapeStroke(strOptString, animatableFloatValueNewInstance2, arrayList, animatableColorValueNewInstance, animatableIntegerValueNewInstance, animatableFloatValueNewInstance, lineCapType, lineJoinType, null);
        }
    }

    enum LineCapType {
        Butt,
        Round,
        Unknown;

        private static int[] f17comairbnblottieShapeStroke$LineCapTypeSwitchesValues = null;

        private static int[] m289getcomairbnblottieShapeStroke$LineCapTypeSwitchesValues() {
            if (f17comairbnblottieShapeStroke$LineCapTypeSwitchesValues != null) {
                return f17comairbnblottieShapeStroke$LineCapTypeSwitchesValues;
            }
            int[] iArr = new int[valuesCustom().length];
            try {
                iArr[Butt.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Round.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Unknown.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f17comairbnblottieShapeStroke$LineCapTypeSwitchesValues = iArr;
            return iArr;
        }

        public static ShapeStroke.LineCapType[] valuesCustom() {
            return values();
        }

        Cap toPaintCap() {
            switch (m289getcomairbnblottieShapeStroke$LineCapTypeSwitchesValues()[ordinal()]) {
                case 1:
                    return Cap.BUTT;
                case 2:
                    return Cap.ROUND;
                default:
                    return Cap.SQUARE;
            }
        }
    }

    enum LineJoinType {
        Miter,
        Round,
        Bevel;

        private static int[] f18comairbnblottieShapeStroke$LineJoinTypeSwitchesValues = null;

        private static int[] m290getcomairbnblottieShapeStroke$LineJoinTypeSwitchesValues() {
            if (f18comairbnblottieShapeStroke$LineJoinTypeSwitchesValues != null) {
                return f18comairbnblottieShapeStroke$LineJoinTypeSwitchesValues;
            }
            int[] iArr = new int[valuesCustom().length];
            try {
                iArr[Bevel.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Miter.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Round.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f18comairbnblottieShapeStroke$LineJoinTypeSwitchesValues = iArr;
            return iArr;
        }

        public static ShapeStroke.LineJoinType[] valuesCustom() {
            return values();
        }

        Join toPaintJoin() {
            switch (m290getcomairbnblottieShapeStroke$LineJoinTypeSwitchesValues()[ordinal()]) {
                case 1:
                    return Join.BEVEL;
                case 2:
                    return Join.MITER;
                case 3:
                    return Join.ROUND;
                default:
                    return null;
            }
        }
    }

    private ShapeStroke(String str, @Nullable AnimatableFloatValue animatableFloatValue, List<AnimatableFloatValue> list, AnimatableColorValue animatableColorValue, AnimatableIntegerValue animatableIntegerValue, AnimatableFloatValue animatableFloatValue2, ShapeStroke.LineCapType lineCapType, ShapeStroke.LineJoinType lineJoinType) {
        this.name = str;
        this.offset = animatableFloatValue;
        this.lineDashPattern = list;
        this.color = animatableColorValue;
        this.opacity = animatableIntegerValue;
        this.width = animatableFloatValue2;
        this.capType = lineCapType;
        this.joinType = lineJoinType;
    }

     ShapeStroke(String str, AnimatableFloatValue animatableFloatValue, List list, AnimatableColorValue animatableColorValue, AnimatableIntegerValue animatableIntegerValue, AnimatableFloatValue animatableFloatValue2, ShapeStroke.LineCapType lineCapType, ShapeStroke.LineJoinType lineJoinType, ShapeStroke shapeStroke) {
        this(str, animatableFloatValue, list, animatableColorValue, animatableIntegerValue, animatableFloatValue2, lineCapType, lineJoinType);
    }

    ShapeStroke.LineCapType getCapType() {
        return this.capType;
    }

    AnimatableColorValue getColor() {
        return this.color;
    }

    AnimatableFloatValue getDashOffset() {
        return this.offset;
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

    AnimatableFloatValue getWidth() {
        return this.width;
    }
}
