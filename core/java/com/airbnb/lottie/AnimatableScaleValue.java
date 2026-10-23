package com.airbnb.lottie;

import java.util.List;
import org.json.JSONObject;

class AnimatableScaleValue extends BaseAnimatableValue<ScaleXY, ScaleXY> {

    static final class Factory {
        private Factory() {
        }

        static AnimatableScaleValue newInstance() {
            return new AnimatableScaleValue(null);
        }

        static AnimatableScaleValue newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            AnimatableValueParser.Result json = AnimatableValueParser.newInstance(jSONObject, 1.0f, lottieComposition, ScaleXY.Factory.INSTANCE).parseJson();
            return new AnimatableScaleValue(json.keyframes, (ScaleXY) json.initialValue);
        }
    }

    private AnimatableScaleValue() {
        super(new ScaleXY());
    }

     AnimatableScaleValue(AnimatableScaleValue animatableScaleValue) {
        this();
    }

    AnimatableScaleValue(List<Keyframe<ScaleXY>> list, ScaleXY scaleXY) {
        super(list, scaleXY);
    }

    @Override
    public KeyframeAnimation<ScaleXY> createAnimation() {
        return !hasAnimation() ? new StaticKeyframeAnimation((ScaleXY) this.initialValue) : new ScaleKeyframeAnimation(this.keyframes);
    }
}
