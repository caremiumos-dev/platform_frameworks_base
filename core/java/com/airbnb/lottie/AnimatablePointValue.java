package com.airbnb.lottie;

import android.graphics.PointF;
import java.util.List;
import org.json.JSONObject;

class AnimatablePointValue extends BaseAnimatableValue<PointF, PointF> {

    static final class Factory {
        private Factory() {
        }

        static AnimatablePointValue newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            AnimatableValueParser.Result json = AnimatableValueParser.newInstance(jSONObject, lottieComposition.getDpScale(), lottieComposition, PointFFactory.INSTANCE).parseJson();
            return new AnimatablePointValue(json.keyframes, (PointF) json.initialValue, null);
        }
    }

    private AnimatablePointValue(List<Keyframe<PointF>> list, PointF pointF) {
        super(list, pointF);
    }

     AnimatablePointValue(List list, PointF pointF, AnimatablePointValue animatablePointValue) {
        this(list, pointF);
    }

    @Override
    public KeyframeAnimation<PointF> createAnimation() {
        return !hasAnimation() ? new StaticKeyframeAnimation((PointF) this.initialValue) : new PointKeyframeAnimation(this.keyframes);
    }
}
