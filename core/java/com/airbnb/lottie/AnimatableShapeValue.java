package com.airbnb.lottie;

import android.graphics.Path;
import java.util.List;
import org.json.JSONObject;

class AnimatableShapeValue extends BaseAnimatableValue<ShapeData, Path> {
    private final Path convertTypePath;

    static final class Factory {
        private Factory() {
        }

        static AnimatableShapeValue newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            AnimatableValueParser.Result json = AnimatableValueParser.newInstance(jSONObject, lottieComposition.getDpScale(), lottieComposition, ShapeData.Factory.INSTANCE).parseJson();
            return new AnimatableShapeValue(json.keyframes, (ShapeData) json.initialValue, null);
        }
    }

    private AnimatableShapeValue(List<Keyframe<ShapeData>> list, ShapeData shapeData) {
        super(list, shapeData);
        this.convertTypePath = new Path();
    }

     AnimatableShapeValue(List list, ShapeData shapeData, AnimatableShapeValue animatableShapeValue) {
        this(list, shapeData);
    }

    @Override
    public Path convertType(ShapeData shapeData) {
        this.convertTypePath.reset();
        MiscUtils.getPathFromData(shapeData, this.convertTypePath);
        return this.convertTypePath;
    }

    @Override
    public BaseKeyframeAnimation<?, Path> createAnimation() {
        return !hasAnimation() ? new StaticKeyframeAnimation(convertType((ShapeData) this.initialValue)) : new ShapeKeyframeAnimation(this.keyframes);
    }
}
