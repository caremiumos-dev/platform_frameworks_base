package com.airbnb.lottie;

import android.graphics.Color;
import android.support.annotation.IntRange;
import android.util.Log;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class AnimatableGradientColorValue extends BaseAnimatableValue<GradientColor, GradientColor> {

    static final class Factory {
        private Factory() {
        }

        static AnimatableGradientColorValue newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            AnimatableValueParser.Result json = AnimatableValueParser.newInstance(jSONObject, 1.0f, lottieComposition, new AnimatableGradientColorValue.ValueFactory(jSONObject.optInt("p", jSONObject.optJSONArray("k").length() / 4), null)).parseJson();
            return new AnimatableGradientColorValue(json.keyframes, (GradientColor) json.initialValue, null);
        }
    }

    private static class ValueFactory implements AnimatableValue.Factory<GradientColor> {
        private final int colorPoints;

        private ValueFactory(int i) {
            this.colorPoints = i;
        }

         ValueFactory(int i, AnimatableGradientColorValue.ValueFactory valueFactory) {
            this(i);
        }

        private void addOpacityStopsToGradientIfNeeded(GradientColor gradientColor, JSONArray jSONArray) {
            int i = this.colorPoints * 4;
            if (jSONArray.length() <= i) {
                return;
            }
            int length = (jSONArray.length() - i) / 2;
            double[] dArr = new double[length];
            double[] dArr2 = new double[length];
            int i2 = 0;
            for (int i3 = i; i3 < jSONArray.length(); i3++) {
                if (i3 % 2 == 0) {
                    dArr[i2] = jSONArray.optDouble(i3);
                } else {
                    dArr2[i2] = jSONArray.optDouble(i3);
                    i2++;
                }
            }
            for (int i4 = 0; i4 < gradientColor.getSize(); i4++) {
                int i5 = gradientColor.getColors()[i4];
                gradientColor.getColors()[i4] = Color.argb(getOpacityAtPosition(gradientColor.getPositions()[i4], dArr, dArr2), Color.red(i5), Color.green(i5), Color.blue(i5));
            }
        }

        @IntRange(from = 0, to = 255)
        private int getOpacityAtPosition(double d, double[] dArr, double[] dArr2) {
            int i = 1;
            while (true) {
                int i2 = i;
                if (i2 >= dArr.length) {
                    return (int) (dArr2[dArr2.length - 1] * 255.0d);
                }
                double d2 = dArr[i2 - 1];
                double d3 = dArr[i2];
                if (dArr[i2] >= d) {
                    return (int) (MiscUtils.lerp(dArr2[i2 - 1], dArr2[i2], (d - d2) / (d3 - d2)) * 255.0d);
                }
                i = i2 + 1;
            }
        }

        @Override
        public GradientColor valueFromObject(Object obj, float f) {
            JSONArray jSONArray = (JSONArray) obj;
            float[] fArr = new float[this.colorPoints];
            int[] iArr = new int[this.colorPoints];
            GradientColor gradientColor = new GradientColor(fArr, iArr);
            if (jSONArray.length() != this.colorPoints * 4) {
                Log.w("LOTTIE", "Unexpected gradient length: " + jSONArray.length() + ". Expected " + (this.colorPoints * 4) + ". This may affect the appearance of the gradient. Make sure to save your After Effects file before exporting an animation with gradients.");
            }
            int i = 0;
            int i2 = 0;
            for (int i3 = 0; i3 < this.colorPoints * 4; i3++) {
                int i4 = i3 / 4;
                double dOptDouble = jSONArray.optDouble(i3);
                switch (i3 % 4) {
                    case 0:
                        fArr[i4] = (float) dOptDouble;
                        break;
                    case 1:
                        i2 = (int) (dOptDouble * 255.0d);
                        break;
                    case 2:
                        i = (int) (dOptDouble * 255.0d);
                        break;
                    case 3:
                        iArr[i4] = Color.argb(255, i2, i, (int) (dOptDouble * 255.0d));
                        break;
                }
            }
            addOpacityStopsToGradientIfNeeded(gradientColor, jSONArray);
            return gradientColor;
        }
    }

    private AnimatableGradientColorValue(List<Keyframe<GradientColor>> list, GradientColor gradientColor) {
        super(list, gradientColor);
    }

     AnimatableGradientColorValue(List list, GradientColor gradientColor, AnimatableGradientColorValue animatableGradientColorValue) {
        this(list, gradientColor);
    }

    @Override
    public KeyframeAnimation<GradientColor> createAnimation() {
        return !hasAnimation() ? new StaticKeyframeAnimation((GradientColor) this.initialValue) : new GradientColorKeyframeAnimation(this.keyframes);
    }
}
