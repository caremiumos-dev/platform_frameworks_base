package com.airbnb.lottie;

import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.view.animation.Interpolator;
import org.json.JSONArray;
import org.json.JSONObject;

class PathKeyframe extends Keyframe<PointF> {

    @Nullable
    private Path path;

    static class Factory {
        private Factory() {
        }

        static PathKeyframe newInstance(JSONObject jSONObject, LottieComposition lottieComposition, AnimatableValue.Factory<PointF> factory) {
            PointF pointFPointFromJsonArray;
            PointF pointF;
            PathKeyframe pathKeyframe = null;
            Keyframe keyframeNewInstance = Keyframe.Factory.newInstance(jSONObject, lottieComposition, lottieComposition.getDpScale(), factory);
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("ti");
            JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("to");
            if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray2 == null) {
                pointFPointFromJsonArray = null;
                pointF = null;
            } else {
                PointF pointFPointFromJsonArray2 = JsonUtils.pointFromJsonArray(jSONArrayOptJSONArray2, lottieComposition.getDpScale());
                pointFPointFromJsonArray = JsonUtils.pointFromJsonArray(jSONArrayOptJSONArray, lottieComposition.getDpScale());
                pointF = pointFPointFromJsonArray2;
            }
            PathKeyframe pathKeyframe2 = new PathKeyframe(lottieComposition, (PointF) keyframeNewInstance.startValue, (PointF) keyframeNewInstance.endValue, keyframeNewInstance.interpolator, keyframeNewInstance.startFrame, keyframeNewInstance.endFrame, pathKeyframe);
            boolean zEquals = (keyframeNewInstance.endValue == null || keyframeNewInstance.startValue == null) ? false : ((PointF) keyframeNewInstance.startValue).equals(keyframeNewInstance.endValue);
            if (pathKeyframe2.endValue != null && (!zEquals)) {
                pathKeyframe2.path = Utils.createPath((PointF) keyframeNewInstance.startValue, (PointF) keyframeNewInstance.endValue, pointF, pointFPointFromJsonArray);
            }
            return pathKeyframe2;
        }
    }

    private PathKeyframe(LottieComposition lottieComposition, @Nullable PointF pointF, @Nullable PointF pointF2, @Nullable Interpolator interpolator, float f, @Nullable Float f2) {
        super(lottieComposition, pointF, pointF2, interpolator, f, f2);
    }

     PathKeyframe(LottieComposition lottieComposition, PointF pointF, PointF pointF2, Interpolator interpolator, float f, Float f2, PathKeyframe pathKeyframe) {
        this(lottieComposition, pointF, pointF2, interpolator, f, f2);
    }

    @Nullable
    Path getPath() {
        return this.path;
    }
}
