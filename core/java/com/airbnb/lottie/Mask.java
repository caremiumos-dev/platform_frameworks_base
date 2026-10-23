package com.airbnb.lottie;

import org.json.JSONObject;

class Mask {
    private final Mask.MaskMode maskMode;
    private final AnimatableShapeValue maskPath;

    static class Factory {
        private Factory() {
        }

        static Mask newMask(JSONObject jSONObject, LottieComposition lottieComposition) {
            Mask.MaskMode maskMode;
            String strOptString = jSONObject.optString("mode");
            if (strOptString.equals("a")) {
                maskMode = Mask.MaskMode.MaskModeAdd;
            } else if (strOptString.equals("s")) {
                maskMode = Mask.MaskMode.MaskModeSubtract;
            } else {
                maskMode = strOptString.equals("i") ? Mask.MaskMode.MaskModeIntersect : Mask.MaskMode.MaskModeUnknown;
            }
            return new Mask(maskMode, AnimatableShapeValue.Factory.newInstance(jSONObject.optJSONObject("pt"), lottieComposition), null);
        }
    }

    enum MaskMode {
        MaskModeAdd,
        MaskModeSubtract,
        MaskModeIntersect,
        MaskModeUnknown;

        public static Mask.MaskMode[] valuesCustom() {
            return values();
        }
    }

    private Mask(Mask.MaskMode maskMode, AnimatableShapeValue animatableShapeValue) {
        this.maskMode = maskMode;
        this.maskPath = animatableShapeValue;
    }

     Mask(Mask.MaskMode maskMode, AnimatableShapeValue animatableShapeValue, Mask mask) {
        this(maskMode, animatableShapeValue);
    }

    Mask.MaskMode getMaskMode() {
        return this.maskMode;
    }

    AnimatableShapeValue getMaskPath() {
        return this.maskPath;
    }
}
