package com.airbnb.lottie;

import java.util.List;
import org.json.JSONObject;

class AnimatableColorValue extends BaseAnimatableValue<Integer, Integer> {

    static final class Factory {
        private Factory() {
        }

        static AnimatableColorValue newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            AnimatableValueParser.Result json = AnimatableValueParser.newInstance(jSONObject, 1.0f, lottieComposition, ColorFactory.INSTANCE).parseJson();
            return new AnimatableColorValue(json.keyframes, (Integer) json.initialValue, null);
        }
    }

    private AnimatableColorValue(List<Keyframe<Integer>> list, Integer num) {
        super(list, num);
    }

     AnimatableColorValue(List list, Integer num, AnimatableColorValue animatableColorValue) {
        this(list, num);
    }

    @Override
    public KeyframeAnimation<Integer> createAnimation() {
        return !hasAnimation() ? new StaticKeyframeAnimation((Integer) this.initialValue) : new ColorKeyframeAnimation(this.keyframes);
    }

    @Override
    public String toString() {
        return "AnimatableColorValue{initialValue=" + this.initialValue + '}';
    }
}
