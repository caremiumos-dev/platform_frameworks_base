package com.airbnb.lottie;

import android.util.Log;
import java.util.List;
import org.json.JSONObject;

class AnimatableFloatValue extends BaseAnimatableValue<Float, Float> {

    static final class Factory {
        private Factory() {
        }

        static AnimatableFloatValue newInstance() {
            return new AnimatableFloatValue(null);
        }

        static AnimatableFloatValue newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            return newInstance(jSONObject, lottieComposition, true);
        }

        static AnimatableFloatValue newInstance(JSONObject jSONObject, LottieComposition lottieComposition, boolean z) {
            float dpScale = z ? lottieComposition.getDpScale() : 1.0f;
            if (jSONObject.has("x")) {
                Log.w("LOTTIE", "Animation has expressions which are not supported.");
            }
            AnimatableValueParser.Result json = AnimatableValueParser.newInstance(jSONObject, dpScale, lottieComposition, AnimatableFloatValue.ValueFactory.INSTANCE).parseJson();
            return new AnimatableFloatValue(json.keyframes, (Float) json.initialValue, null);
        }
    }

    private static class ValueFactory implements AnimatableValue.Factory<Float> {
        static final AnimatableFloatValue.ValueFactory INSTANCE = new AnimatableFloatValue.ValueFactory();

        private ValueFactory() {
        }

        @Override
        public Float valueFromObject(Object obj, float f) {
            return Float.valueOf(JsonUtils.valueFromObject(obj) * f);
        }
    }

    private AnimatableFloatValue() {
        super(Float.valueOf(0.0f));
    }

     AnimatableFloatValue(AnimatableFloatValue animatableFloatValue) {
        this();
    }

    private AnimatableFloatValue(List<Keyframe<Float>> list, Float f) {
        super(list, f);
    }

     AnimatableFloatValue(List list, Float f, AnimatableFloatValue animatableFloatValue) {
        this(list, f);
    }

    @Override
    public KeyframeAnimation<Float> createAnimation() {
        return !hasAnimation() ? new StaticKeyframeAnimation((Float) this.initialValue) : new FloatKeyframeAnimation(this.keyframes);
    }

    @Override
    public Float getInitialValue() {
        return (Float) this.initialValue;
    }
}
