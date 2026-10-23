package com.airbnb.lottie;

import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import java.util.List;

class PolystarContent implements PathContent, BaseKeyframeAnimation.AnimationListener {

    private static int[] f16comairbnblottiePolystarShape$TypeSwitchesValues = null;
    private static final float POLYGON_MAGIC_NUMBER = 0.25f;
    private static final float POLYSTAR_MAGIC_NUMBER = 0.47829f;

    @Nullable
    private final BaseKeyframeAnimation<?, Float> innerRadiusAnimation;

    @Nullable
    private final BaseKeyframeAnimation<?, Float> innerRoundednessAnimation;
    private boolean isPathValid;
    private final LottieDrawable lottieDrawable;
    private final String name;
    private final BaseKeyframeAnimation<?, Float> outerRadiusAnimation;
    private final BaseKeyframeAnimation<?, Float> outerRoundednessAnimation;
    private final Path path = new Path();
    private final BaseKeyframeAnimation<?, Float> pointsAnimation;
    private final BaseKeyframeAnimation<?, PointF> positionAnimation;
    private final BaseKeyframeAnimation<?, Float> rotationAnimation;

    @Nullable
    private TrimPathContent trimPath;
    private final PolystarShape.Type type;

    private static int[] m287getcomairbnblottiePolystarShape$TypeSwitchesValues() {
        if (f16comairbnblottiePolystarShape$TypeSwitchesValues != null) {
            return f16comairbnblottiePolystarShape$TypeSwitchesValues;
        }
        int[] iArr = new int[PolystarShape.Type.valuesCustom().length];
        try {
            iArr[PolystarShape.Type.Polygon.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[PolystarShape.Type.Star.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        f16comairbnblottiePolystarShape$TypeSwitchesValues = iArr;
        return iArr;
    }

    PolystarContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, PolystarShape polystarShape) {
        this.lottieDrawable = lottieDrawable;
        this.name = polystarShape.getName();
        this.type = polystarShape.getType();
        this.pointsAnimation = polystarShape.getPoints().createAnimation();
        this.positionAnimation = polystarShape.getPosition().createAnimation();
        this.rotationAnimation = polystarShape.getRotation().createAnimation();
        this.outerRadiusAnimation = polystarShape.getOuterRadius().createAnimation();
        this.outerRoundednessAnimation = polystarShape.getOuterRoundedness().createAnimation();
        if (this.type == PolystarShape.Type.Star) {
            this.innerRadiusAnimation = polystarShape.getInnerRadius().createAnimation();
            this.innerRoundednessAnimation = polystarShape.getInnerRoundedness().createAnimation();
        } else {
            this.innerRadiusAnimation = null;
            this.innerRoundednessAnimation = null;
        }
        baseLayer.addAnimation(this.pointsAnimation);
        baseLayer.addAnimation(this.positionAnimation);
        baseLayer.addAnimation(this.rotationAnimation);
        baseLayer.addAnimation(this.outerRadiusAnimation);
        baseLayer.addAnimation(this.outerRoundednessAnimation);
        if (this.type == PolystarShape.Type.Star) {
            baseLayer.addAnimation(this.innerRadiusAnimation);
            baseLayer.addAnimation(this.innerRoundednessAnimation);
        }
        this.pointsAnimation.addUpdateListener(this);
        this.positionAnimation.addUpdateListener(this);
        this.rotationAnimation.addUpdateListener(this);
        this.outerRadiusAnimation.addUpdateListener(this);
        this.outerRoundednessAnimation.addUpdateListener(this);
        if (this.type == PolystarShape.Type.Star) {
            this.outerRadiusAnimation.addUpdateListener(this);
            this.outerRoundednessAnimation.addUpdateListener(this);
        }
    }

    private void createPolygonPath() {
        int iFloor = (int) Math.floor(this.pointsAnimation.getValue().floatValue());
        double radians = Math.toRadians(((double) (this.rotationAnimation == null ? 0.0f : this.rotationAnimation.getValue().floatValue())) - 90.0d);
        float f = (float) (6.283185307179586d / ((double) iFloor));
        float fFloatValue = this.outerRoundednessAnimation.getValue().floatValue() / 100.0f;
        float fFloatValue2 = this.outerRadiusAnimation.getValue().floatValue();
        float fCos = (float) (((double) fFloatValue2) * Math.cos(radians));
        float fSin = (float) (((double) fFloatValue2) * Math.sin(radians));
        this.path.moveTo(fCos, fSin);
        double d = radians + ((double) f);
        double dCeil = Math.ceil(iFloor);
        int i = 0;
        while (true) {
            int i2 = i;
            float f2 = fCos;
            double d2 = d;
            float f3 = fSin;
            if (i2 >= dCeil) {
                PointF value = this.positionAnimation.getValue();
                this.path.offset(value.x, value.y);
                this.path.close();
                return;
            }
            fCos = (float) (((double) fFloatValue2) * Math.cos(d2));
            fSin = (float) (((double) fFloatValue2) * Math.sin(d2));
            if (fFloatValue != 0.0f) {
                float fAtan2 = (float) (Math.atan2(f3, f2) - 1.5707963267948966d);
                float fCos2 = (float) Math.cos(fAtan2);
                float fSin2 = (float) Math.sin(fAtan2);
                float fAtan3 = (float) (Math.atan2(fSin, fCos) - 1.5707963267948966d);
                this.path.cubicTo(f2 - (fCos2 * ((fFloatValue2 * fFloatValue) * POLYGON_MAGIC_NUMBER)), f3 - (((fFloatValue2 * fFloatValue) * POLYGON_MAGIC_NUMBER) * fSin2), fCos + (((float) Math.cos(fAtan3)) * fFloatValue2 * fFloatValue * POLYGON_MAGIC_NUMBER), (((float) Math.sin(fAtan3)) * fFloatValue2 * fFloatValue * POLYGON_MAGIC_NUMBER) + fSin, fCos, fSin);
            } else {
                this.path.lineTo(fCos, fSin);
            }
            d = d2 + ((double) f);
            i = i2 + 1;
        }
    }

    private void createStarPath() {
        float fCos;
        float fSin;
        double d;
        float f;
        float f2;
        float f3;
        float f4;
        float fFloatValue = this.pointsAnimation.getValue().floatValue();
        double radians = Math.toRadians(((double) (this.rotationAnimation == null ? 0.0f : this.rotationAnimation.getValue().floatValue())) - 90.0d);
        float f5 = (float) (6.283185307179586d / ((double) fFloatValue));
        float f6 = f5 / 2.0f;
        float f7 = fFloatValue - ((int) fFloatValue);
        double d2 = f7 != 0.0f ? radians + ((double) ((1.0f - f7) * f6)) : radians;
        float fFloatValue2 = this.outerRadiusAnimation.getValue().floatValue();
        float fFloatValue3 = this.innerRadiusAnimation.getValue().floatValue();
        float fFloatValue4 = this.innerRoundednessAnimation != null ? this.innerRoundednessAnimation.getValue().floatValue() / 100.0f : 0.0f;
        float fFloatValue5 = this.outerRoundednessAnimation != null ? this.outerRoundednessAnimation.getValue().floatValue() / 100.0f : 0.0f;
        if (f7 != 0.0f) {
            float f8 = fFloatValue3 + ((fFloatValue2 - fFloatValue3) * f7);
            fCos = (float) (((double) f8) * Math.cos(d2));
            fSin = (float) (((double) f8) * Math.sin(d2));
            this.path.moveTo(fCos, fSin);
            d = d2 + ((double) ((f5 * f7) / 2.0f));
            f = f8;
        } else {
            fCos = (float) (((double) fFloatValue2) * Math.cos(d2));
            fSin = (float) (((double) fFloatValue2) * Math.sin(d2));
            this.path.moveTo(fCos, fSin);
            d = d2 + ((double) f6);
            f = 0.0f;
        }
        double dCeil = Math.ceil(fFloatValue) * 2.0d;
        int i = 0;
        boolean z = false;
        float f9 = fSin;
        float f10 = fCos;
        while (true) {
            double d3 = d;
            if (i >= dCeil) {
                PointF value = this.positionAnimation.getValue();
                this.path.offset(value.x, value.y);
                this.path.close();
                return;
            }
            float f11 = z ? fFloatValue2 : fFloatValue3;
            float f12 = (f == 0.0f || ((double) i) != dCeil - 2.0d) ? f6 : (f5 * f7) / 2.0f;
            if (f != 0.0f && i == dCeil - 1.0d) {
                f11 = f;
            }
            float fCos2 = (float) (((double) f11) * Math.cos(d3));
            float fSin2 = (float) (((double) f11) * Math.sin(d3));
            if (fFloatValue4 == 0.0f && fFloatValue5 == 0.0f) {
                this.path.lineTo(fCos2, fSin2);
            } else {
                float fAtan2 = (float) (Math.atan2(f9, f10) - 1.5707963267948966d);
                float fCos3 = (float) Math.cos(fAtan2);
                float fSin3 = (float) Math.sin(fAtan2);
                float fAtan3 = (float) (Math.atan2(fSin2, fCos2) - 1.5707963267948966d);
                float fCos4 = (float) Math.cos(fAtan3);
                float fSin4 = (float) Math.sin(fAtan3);
                float f13 = z ? fFloatValue4 : fFloatValue5;
                float f14 = z ? fFloatValue5 : fFloatValue4;
                float f15 = z ? fFloatValue3 : fFloatValue2;
                float f16 = z ? fFloatValue2 : fFloatValue3;
                float f17 = fCos3 * f15 * f13 * POLYSTAR_MAGIC_NUMBER;
                float f18 = f15 * f13 * POLYSTAR_MAGIC_NUMBER * fSin3;
                float f19 = f16 * f14 * POLYSTAR_MAGIC_NUMBER * fCos4;
                float f20 = f16 * f14 * POLYSTAR_MAGIC_NUMBER * fSin4;
                if (f7 == 0.0f) {
                    f2 = f20;
                    f3 = f17;
                    f4 = f19;
                } else if (i == 0) {
                    f18 *= f7;
                    f2 = f20;
                    f3 = f17 * f7;
                    f4 = f19;
                } else if (i == dCeil - 1.0d) {
                    float f21 = f19 * f7;
                    f2 = f20 * f7;
                    f3 = f17;
                    f4 = f21;
                } else {
                    f2 = f20;
                    f3 = f17;
                    f4 = f19;
                }
                this.path.cubicTo(f10 - f3, f9 - f18, f4 + fCos2, f2 + fSin2, fCos2, fSin2);
            }
            d = d3 + ((double) f12);
            i++;
            z = !z;
            f9 = fSin2;
            f10 = fCos2;
        }
    }

    private void invalidate() {
        this.isPathValid = false;
        this.lottieDrawable.invalidateSelf();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Path getPath() {
        if (this.isPathValid) {
            return this.path;
        }
        this.path.reset();
        switch (m287getcomairbnblottiePolystarShape$TypeSwitchesValues()[this.type.ordinal()]) {
            case 1:
                createPolygonPath();
                break;
            case 2:
                createStarPath();
                break;
        }
        this.path.close();
        Utils.applyTrimPathIfNeeded(this.path, this.trimPath);
        this.isPathValid = true;
        return this.path;
    }

    @Override
    public void onValueChanged() {
        invalidate();
    }

    @Override
    public void setContents(List<Content> list, List<Content> list2) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= list.size()) {
                return;
            }
            Content content = list.get(i2);
            if ((content instanceof TrimPathContent) && ((TrimPathContent) content).getType() == ShapeTrimPath.Type.Simultaneously) {
                this.trimPath = (TrimPathContent) content;
                this.trimPath.addListener(this);
            }
            i = i2 + 1;
        }
    }
}
