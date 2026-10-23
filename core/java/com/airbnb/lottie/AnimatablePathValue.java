package com.airbnb.lottie;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class AnimatablePathValue implements AnimatableValue<PointF> {
    private PointF initialPoint;
    private final List<PathKeyframe> keyframes;

    private static class ValueFactory implements AnimatableValue.Factory<PointF> {
        private static final AnimatableValue.Factory<PointF> INSTANCE = new AnimatablePathValue.ValueFactory();

        private ValueFactory() {
        }

        @Override
        public PointF valueFromObject(Object obj, float f) {
            return JsonUtils.pointFromJsonArray((JSONArray) obj, f);
        }
    }

    AnimatablePathValue() {
        this.keyframes = new ArrayList();
        this.initialPoint = new PointF(0.0f, 0.0f);
    }

    AnimatablePathValue(Object obj, LottieComposition lottieComposition) {
        this.keyframes = new ArrayList();
        if (!hasKeyframes(obj)) {
            this.initialPoint = JsonUtils.pointFromJsonArray((JSONArray) obj, lottieComposition.getDpScale());
            return;
        }
        JSONArray jSONArray = (JSONArray) obj;
        int length = jSONArray.length();
        for (int i = 0; i < length; i++) {
            this.keyframes.add(PathKeyframe.Factory.newInstance(jSONArray.optJSONObject(i), lottieComposition, AnimatablePathValue.ValueFactory.INSTANCE));
        }
        Keyframe.setEndFrames(this.keyframes);
    }

    static AnimatableValue<PointF> createAnimatablePathOrSplitDimensionPath(JSONObject jSONObject, LottieComposition lottieComposition) {
        return jSONObject.has("k") ? new AnimatablePathValue(jSONObject.opt("k"), lottieComposition) : new AnimatableSplitDimensionPathValue(AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("x"), lottieComposition), AnimatableFloatValue.Factory.newInstance(jSONObject.optJSONObject("y"), lottieComposition));
    }

    private boolean hasKeyframes(Object obj) {
        if (!(obj instanceof JSONArray)) {
            return false;
        }
        Object objOpt = ((JSONArray) obj).opt(0);
        if (objOpt instanceof JSONObject) {
            return ((JSONObject) objOpt).has("t");
        }
        return false;
    }

    @Override
    public BaseKeyframeAnimation<?, PointF> createAnimation() {
        return !hasAnimation() ? new StaticKeyframeAnimation(this.initialPoint) : new PathKeyframeAnimation(this.keyframes);
    }

    @Override
    public boolean hasAnimation() {
        return !this.keyframes.isEmpty();
    }

    public String toString() {
        return "initialPoint=" + this.initialPoint;
    }
}
