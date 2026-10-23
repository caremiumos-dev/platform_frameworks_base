package com.airbnb.lottie;

import org.json.JSONObject;

class ShapeTrimPath {
    private final AnimatableFloatValue end;
    private final String name;
    private final AnimatableFloatValue offset;
    private final AnimatableFloatValue start;
    private final ShapeTrimPath.Type type;

    static class Factory {
        private Factory() {
        }

        static ShapeTrimPath newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            return new ShapeTrimPath(jSONObject.optString("nm"), ShapeTrimPath.Type.forId(jSONObject.optInt("m", 1)), AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("s"), lottieComposition, false), AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("e"), lottieComposition, false), AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("o"), lottieComposition, false), null);
        }
    }

    enum Type {
        Simultaneously,
        Individually;

        static ShapeTrimPath.Type forId(int i) {
            switch (i) {
                case 1:
                    return Simultaneously;
                case 2:
                    return Individually;
                default:
                    throw new IllegalArgumentException("Unknown trim path type " + i);
            }
        }

        public static ShapeTrimPath.Type[] valuesCustom() {
            return values();
        }
    }

    private ShapeTrimPath(String str, ShapeTrimPath.Type type, AnimatableFloatValue animatableFloatValue, AnimatableFloatValue animatableFloatValue2, AnimatableFloatValue animatableFloatValue3) {
        this.name = str;
        this.type = type;
        this.start = animatableFloatValue;
        this.end = animatableFloatValue2;
        this.offset = animatableFloatValue3;
    }

     ShapeTrimPath(String str, ShapeTrimPath.Type type, AnimatableFloatValue animatableFloatValue, AnimatableFloatValue animatableFloatValue2, AnimatableFloatValue animatableFloatValue3, ShapeTrimPath shapeTrimPath) {
        this(str, type, animatableFloatValue, animatableFloatValue2, animatableFloatValue3);
    }

    AnimatableFloatValue getEnd() {
        return this.end;
    }

    String getName() {
        return this.name;
    }

    AnimatableFloatValue getOffset() {
        return this.offset;
    }

    AnimatableFloatValue getStart() {
        return this.start;
    }

    ShapeTrimPath.Type getType() {
        return this.type;
    }

    public String toString() {
        return "Trim Path: {start: " + this.start + ", end: " + this.end + ", offset: " + this.offset + "}";
    }
}
