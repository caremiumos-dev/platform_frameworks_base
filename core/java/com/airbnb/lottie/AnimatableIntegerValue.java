package com.airbnb.lottie;

import android.util.Log;
import java.util.List;
import org.json.JSONObject;

class AnimatableIntegerValue extends BaseAnimatableValue<Integer, Integer> {

    static final class Factory {
        private Factory() {
        }

        static AnimatableIntegerValue newInstance() {
            return new AnimatableIntegerValue(null);
        }

        static AnimatableIntegerValue newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            if (jSONObject.has("x")) {
                Log.w("LOTTIE", "Animation has expressions which are not supported.");
            }
            AnimatableValueParser.Result json = AnimatableValueParser.newInstance(jSONObject, 1.0f, lottieComposition, AnimatableIntegerValue.ValueFactory.INSTANCE).parseJson();
            return new AnimatableIntegerValue(json.keyframes, (Integer) json.initialValue);
        }
    }

    private static class ValueFactory implements AnimatableValue.Factory<Integer> {
        private static final AnimatableIntegerValue.ValueFactory INSTANCE = new AnimatableIntegerValue.ValueFactory();

        private ValueFactory() {
        }

        @Override
        public Integer valueFromObject(Object obj, float f) {
            return Integer.valueOf(Math.round(JsonUtils.valueFromObject(obj) * f));
        }
    }

    private AnimatableIntegerValue() {
        super(100);
    }

     AnimatableIntegerValue(AnimatableIntegerValue animatableIntegerValue) {
        this();
    }

    AnimatableIntegerValue(List<Keyframe<Integer>> list, Integer num) {
        super(list, num);
    }

    @Override
    public KeyframeAnimation<Integer> createAnimation() {
        return !hasAnimation() ? new StaticKeyframeAnimation((Integer) this.initialValue) : new IntegerKeyframeAnimation(this.keyframes);
    }

    @Override
    public Integer getInitialValue() {
        return (Integer) this.initialValue;
    }
}
