package com.airbnb.lottie;

import android.support.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class AnimatableValueParser<T> {
    private final LottieComposition composition;

    @Nullable
    private final JSONObject json;
    private final float scale;
    private final AnimatableValue.Factory<T> valueFactory;

    static class Result<T> {

        @Nullable
        final T initialValue;
        final List<Keyframe<T>> keyframes;

        Result(List<Keyframe<T>> list, @Nullable T t) {
            this.keyframes = list;
            this.initialValue = t;
        }
    }

    private AnimatableValueParser(@Nullable JSONObject jSONObject, float f, LottieComposition lottieComposition, AnimatableValue.Factory<T> factory) {
        this.json = jSONObject;
        this.scale = f;
        this.composition = lottieComposition;
        this.valueFactory = factory;
    }

    private static boolean hasKeyframes(Object obj) {
        if (!(obj instanceof JSONArray)) {
            return false;
        }
        Object objOpt = ((JSONArray) obj).opt(0);
        if (objOpt instanceof JSONObject) {
            return ((JSONObject) objOpt).has("t");
        }
        return false;
    }

    static <T> AnimatableValueParser<T> newInstance(@Nullable JSONObject jSONObject, float f, LottieComposition lottieComposition, AnimatableValue.Factory<T> factory) {
        return new AnimatableValueParser<>(jSONObject, f, lottieComposition, factory);
    }

    @Nullable
    private T parseInitialValue(List<Keyframe<T>> list) {
        if (this.json != null) {
            return !list.isEmpty() ? list.get(0).startValue : this.valueFactory.valueFromObject(this.json.opt("k"), this.scale);
        }
        return null;
    }

    private List<Keyframe<T>> parseKeyframes() {
        if (this.json == null) {
            return Collections.emptyList();
        }
        Object objOpt = this.json.opt("k");
        return hasKeyframes(objOpt) ? Keyframe.Factory.parseKeyframes((JSONArray) objOpt, this.composition, this.scale, this.valueFactory) : Collections.emptyList();
    }

    AnimatableValueParser.Result<T> parseJson() {
        List<Keyframe<T>> keyframes = parseKeyframes();
        return new AnimatableValueParser.Result<>(keyframes, parseInitialValue(keyframes));
    }
}
