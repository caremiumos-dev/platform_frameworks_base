package com.airbnb.lottie;

import android.graphics.PointF;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class Keyframe<T> {
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final float MAX_CP_VALUE = 100.0f;
    private final LottieComposition composition;

    @Nullable
    Float endFrame;

    @Nullable
    final T endValue;

    @Nullable
    final Interpolator interpolator;
    final float startFrame;

    @Nullable
    final T startValue;

    static class Factory {
        private Factory() {
        }

        static <T> Keyframe<T> newInstance(JSONObject jSONObject, LottieComposition lottieComposition, float f, AnimatableValue.Factory<T> factory) {
            Interpolator interpolatorCreate;
            T t;
            T t2;
            PointF pointFPointFromJsonObject;
            PointF pointFPointFromJsonObject2;
            float fOptDouble = 0.0f;
            if (jSONObject.has("t")) {
                fOptDouble = (float) jSONObject.optDouble("t", 0.0d);
                Object objOpt = jSONObject.opt("s");
                T tValueFromObject = objOpt != null ? factory.valueFromObject(objOpt, f) : null;
                Object objOpt2 = jSONObject.opt("e");
                T tValueFromObject2 = objOpt2 != null ? factory.valueFromObject(objOpt2, f) : null;
                JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject("o");
                JSONObject jSONObjectOptJSONObject2 = jSONObject.optJSONObject("i");
                if (jSONObjectOptJSONObject == null || jSONObjectOptJSONObject2 == null) {
                    pointFPointFromJsonObject = null;
                    pointFPointFromJsonObject2 = null;
                } else {
                    pointFPointFromJsonObject2 = JsonUtils.pointFromJsonObject(jSONObjectOptJSONObject, f);
                    pointFPointFromJsonObject = JsonUtils.pointFromJsonObject(jSONObjectOptJSONObject2, f);
                }
                if (jSONObject.optInt("h", 0) == 1) {
                    interpolatorCreate = Keyframe.LINEAR_INTERPOLATOR;
                    t = tValueFromObject;
                    t2 = tValueFromObject;
                } else if (pointFPointFromJsonObject2 != null) {
                    pointFPointFromJsonObject2.x = MiscUtils.clamp(pointFPointFromJsonObject2.x, -f, f);
                    pointFPointFromJsonObject2.y = MiscUtils.clamp(pointFPointFromJsonObject2.y, -100.0f, Keyframe.MAX_CP_VALUE);
                    pointFPointFromJsonObject.x = MiscUtils.clamp(pointFPointFromJsonObject.x, -f, f);
                    pointFPointFromJsonObject.y = MiscUtils.clamp(pointFPointFromJsonObject.y, -100.0f, Keyframe.MAX_CP_VALUE);
                    interpolatorCreate = PathInterpolatorCompat.create(pointFPointFromJsonObject2.x / f, pointFPointFromJsonObject2.y / f, pointFPointFromJsonObject.x / f, pointFPointFromJsonObject.y / f);
                    t = tValueFromObject2;
                    t2 = tValueFromObject;
                } else {
                    interpolatorCreate = Keyframe.LINEAR_INTERPOLATOR;
                    t = tValueFromObject2;
                    t2 = tValueFromObject;
                }
            } else {
                T tValueFromObject3 = factory.valueFromObject(jSONObject, f);
                interpolatorCreate = null;
                t = tValueFromObject3;
                t2 = tValueFromObject3;
            }
            return new Keyframe<>(lottieComposition, t2, t, interpolatorCreate, fOptDouble, null);
        }

        static <T> List<Keyframe<T>> parseKeyframes(JSONArray jSONArray, LottieComposition lottieComposition, float f, AnimatableValue.Factory<T> factory) {
            int length = jSONArray.length();
            if (length == 0) {
                return Collections.emptyList();
            }
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < length; i++) {
                arrayList.add(newInstance(jSONArray.optJSONObject(i), lottieComposition, f, factory));
            }
            Keyframe.setEndFrames(arrayList);
            return arrayList;
        }
    }

    public Keyframe(LottieComposition lottieComposition, @Nullable T t, @Nullable T t2, @Nullable Interpolator interpolator, float f, @Nullable Float f2) {
        this.composition = lottieComposition;
        this.startValue = t;
        this.endValue = t2;
        this.interpolator = interpolator;
        this.startFrame = f;
        this.endFrame = f2;
    }

    static void setEndFrames(List<? extends Keyframe<?>> list) {
        int size = list.size();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= size - 1) {
                break;
            }
            list.get(i2).endFrame = Float.valueOf(list.get(i2 + 1).startFrame);
            i = i2 + 1;
        }
        Keyframe<?> keyframe = list.get(size - 1);
        if (keyframe.startValue == null) {
            list.remove(keyframe);
        }
    }

    boolean containsProgress(@FloatRange(from = 0.0f, to = 1.0f) float f) {
        return f >= getStartProgress() && f <= getEndProgress();
    }

    @FloatRange(from = 0.0f, to = 1.0f)
    float getEndProgress() {
        if (this.endFrame == null) {
            return 1.0f;
        }
        return this.endFrame.floatValue() / this.composition.getDurationFrames();
    }

    @FloatRange(from = 0.0f, to = 1.0f)
    float getStartProgress() {
        return this.startFrame / this.composition.getDurationFrames();
    }

    boolean isStatic() {
        return this.interpolator == null;
    }

    public String toString() {
        return "Keyframe{startValue=" + this.startValue + ", endValue=" + this.endValue + ", startFrame=" + this.startFrame + ", endFrame=" + this.endFrame + ", interpolator=" + this.interpolator + '}';
    }
}
