package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

abstract class BaseStrokeContent implements DrawingContent, BaseKeyframeAnimation.AnimationListener {
    private final List<BaseKeyframeAnimation<?, Float>> dashPatternAnimations;

    @Nullable
    private final BaseKeyframeAnimation<?, Float> dashPatternOffsetAnimation;
    private final float[] dashPatternValues;
    private final LottieDrawable lottieDrawable;
    private final BaseKeyframeAnimation<?, Integer> opacityAnimation;
    private final BaseKeyframeAnimation<?, Float> widthAnimation;
    private final PathMeasure pm = new PathMeasure();
    private final Path path = new Path();
    private final Path trimPathPath = new Path();
    private final RectF rect = new RectF();
    private final List<BaseStrokeContent.PathGroup> pathGroups = new ArrayList();
    final Paint paint = new Paint(1);

    private static final class PathGroup {
        private final List<PathContent> paths;

        @Nullable
        private final TrimPathContent trimPath;

        private PathGroup(@Nullable TrimPathContent trimPathContent) {
            this.paths = new ArrayList();
            this.trimPath = trimPathContent;
        }

         PathGroup(TrimPathContent trimPathContent, BaseStrokeContent.PathGroup pathGroup) {
            this(trimPathContent);
        }
    }

    BaseStrokeContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, Cap cap, Join join, AnimatableIntegerValue animatableIntegerValue, AnimatableFloatValue animatableFloatValue, List<AnimatableFloatValue> list, AnimatableFloatValue animatableFloatValue2) {
        this.lottieDrawable = lottieDrawable;
        this.paint.setStyle(Style.STROKE);
        this.paint.setStrokeCap(cap);
        this.paint.setStrokeJoin(join);
        this.opacityAnimation = animatableIntegerValue.createAnimation();
        this.widthAnimation = animatableFloatValue.createAnimation();
        if (animatableFloatValue2 == null) {
            this.dashPatternOffsetAnimation = null;
        } else {
            this.dashPatternOffsetAnimation = animatableFloatValue2.createAnimation();
        }
        this.dashPatternAnimations = new ArrayList(list.size());
        this.dashPatternValues = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            this.dashPatternAnimations.add(list.get(i).createAnimation());
        }
        baseLayer.addAnimation(this.opacityAnimation);
        baseLayer.addAnimation(this.widthAnimation);
        for (int i2 = 0; i2 < this.dashPatternAnimations.size(); i2++) {
            baseLayer.addAnimation(this.dashPatternAnimations.get(i2));
        }
        if (this.dashPatternOffsetAnimation != null) {
            baseLayer.addAnimation(this.dashPatternOffsetAnimation);
        }
        this.opacityAnimation.addUpdateListener(this);
        this.widthAnimation.addUpdateListener(this);
        for (int i3 = 0; i3 < list.size(); i3++) {
            this.dashPatternAnimations.get(i3).addUpdateListener(this);
        }
        if (this.dashPatternOffsetAnimation != null) {
            this.dashPatternOffsetAnimation.addUpdateListener(this);
        }
    }

    private void applyDashPatternIfNeeded(Matrix matrix) {
        int i = 0;
        if (this.dashPatternAnimations.isEmpty()) {
            return;
        }
        float scale = Utils.getScale(matrix);
        while (true) {
            int i2 = i;
            if (i2 >= this.dashPatternAnimations.size()) {
                break;
            }
            this.dashPatternValues[i2] = this.dashPatternAnimations.get(i2).getValue().floatValue();
            if (i2 % 2 == 0) {
                if (this.dashPatternValues[i2] < 1.0f) {
                    this.dashPatternValues[i2] = 1.0f;
                }
            } else if (this.dashPatternValues[i2] < 0.1f) {
                this.dashPatternValues[i2] = 0.1f;
            }
            float[] fArr = this.dashPatternValues;
            fArr[i2] = fArr[i2] * scale;
            i = i2 + 1;
        }
        this.paint.setPathEffect(new DashPathEffect(this.dashPatternValues, this.dashPatternOffsetAnimation == null ? 0.0f : this.dashPatternOffsetAnimation.getValue().floatValue()));
    }

    private void applyTrimPath(Canvas canvas, BaseStrokeContent.PathGroup pathGroup, Matrix matrix) {
        float f;
        if (pathGroup.trimPath == null) {
            return;
        }
        this.path.reset();
        for (int size = pathGroup.paths.size() - 1; size >= 0; size--) {
            this.path.addPath(((PathContent) pathGroup.paths.get(size)).getPath(), matrix);
        }
        this.pm.setPath(this.path, false);
        float length = this.pm.getLength();
        while (true) {
            f = length;
            if (!this.pm.nextContour()) {
                break;
            } else {
                length = this.pm.getLength() + f;
            }
        }
        float fFloatValue = (pathGroup.trimPath.getOffset().getValue().floatValue() * f) / 360.0f;
        float fFloatValue2 = ((pathGroup.trimPath.getStart().getValue().floatValue() * f) / 100.0f) + fFloatValue;
        float fFloatValue3 = ((pathGroup.trimPath.getEnd().getValue().floatValue() * f) / 100.0f) + fFloatValue;
        int size2 = pathGroup.paths.size() - 1;
        float f2 = 0.0f;
        while (size2 >= 0) {
            this.trimPathPath.set(((PathContent) pathGroup.paths.get(size2)).getPath());
            this.trimPathPath.transform(matrix);
            this.pm.setPath(this.trimPathPath, false);
            float length2 = this.pm.getLength();
            if (fFloatValue3 > f && fFloatValue3 - f < f2 + length2 && f2 < fFloatValue3 - f) {
                Utils.applyTrimPathIfNeeded(this.trimPathPath, fFloatValue2 > f ? (fFloatValue2 - f) / length2 : 0.0f, Math.min((fFloatValue3 - f) / length2, 1.0f), 0.0f);
                canvas.drawPath(this.trimPathPath, this.paint);
            } else if (f2 + length2 >= fFloatValue2 && f2 <= fFloatValue3) {
                if (f2 + length2 > fFloatValue3 || fFloatValue2 >= f2) {
                    Utils.applyTrimPathIfNeeded(this.trimPathPath, fFloatValue2 < f2 ? 0.0f : (fFloatValue2 - f2) / length2, fFloatValue3 > f2 + length2 ? 1.0f : (fFloatValue3 - f2) / length2, 0.0f);
                    canvas.drawPath(this.trimPathPath, this.paint);
                } else {
                    canvas.drawPath(this.trimPathPath, this.paint);
                }
            }
            size2--;
            f2 += length2;
        }
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix, int i) {
        this.paint.setAlpha((int) (((this.opacityAnimation.getValue().intValue() * (i / 255.0f)) / 100.0f) * 255.0f));
        this.paint.setStrokeWidth(this.widthAnimation.getValue().floatValue() * Utils.getScale(matrix));
        if (this.paint.getStrokeWidth() <= 0.0f) {
            return;
        }
        applyDashPatternIfNeeded(matrix);
        for (int i2 = 0; i2 < this.pathGroups.size(); i2++) {
            BaseStrokeContent.PathGroup pathGroup = this.pathGroups.get(i2);
            if (pathGroup.trimPath != null) {
                applyTrimPath(canvas, pathGroup, matrix);
            } else {
                this.path.reset();
                for (int size = pathGroup.paths.size() - 1; size >= 0; size--) {
                    this.path.addPath(((PathContent) pathGroup.paths.get(size)).getPath(), matrix);
                }
                canvas.drawPath(this.path, this.paint);
            }
        }
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        this.path.reset();
        for (int i = 0; i < this.pathGroups.size(); i++) {
            BaseStrokeContent.PathGroup pathGroup = this.pathGroups.get(i);
            for (int i2 = 0; i2 < pathGroup.paths.size(); i2++) {
                this.path.addPath(((PathContent) pathGroup.paths.get(i2)).getPath(), matrix);
            }
        }
        this.path.computeBounds(this.rect, false);
        float fFloatValue = this.widthAnimation.getValue().floatValue();
        this.rect.set(this.rect.left - (fFloatValue / 2.0f), this.rect.top - (fFloatValue / 2.0f), this.rect.right + (fFloatValue / 2.0f), (fFloatValue / 2.0f) + this.rect.bottom);
        rectF.set(this.rect);
        rectF.set(rectF.left - 1.0f, rectF.top - 1.0f, rectF.right + 1.0f, rectF.bottom + 1.0f);
    }

    @Override
    public void onValueChanged() {
        this.lottieDrawable.invalidateSelf();
    }

    @Override
    public void setContents(List<Content> list, List<Content> list2) {
        BaseStrokeContent.PathGroup pathGroup;
        BaseStrokeContent.PathGroup pathGroup2 = null;
        int size = list.size() - 1;
        TrimPathContent trimPathContent = null;
        while (size >= 0) {
            Content content = list.get(size);
            size--;
            trimPathContent = ((content instanceof TrimPathContent) && ((TrimPathContent) content).getType() == ShapeTrimPath.Type.Individually) ? (TrimPathContent) content : trimPathContent;
        }
        if (trimPathContent != null) {
            trimPathContent.addListener(this);
        }
        int size2 = list2.size() - 1;
        BaseStrokeContent.PathGroup pathGroup3 = null;
        while (size2 >= 0) {
            Content content2 = list2.get(size2);
            if ((content2 instanceof TrimPathContent) && ((TrimPathContent) content2).getType() == ShapeTrimPath.Type.Individually) {
                if (pathGroup3 != null) {
                    this.pathGroups.add(pathGroup3);
                }
                BaseStrokeContent.PathGroup pathGroup4 = new BaseStrokeContent.PathGroup((TrimPathContent) content2, pathGroup2);
                ((TrimPathContent) content2).addListener(this);
                pathGroup = pathGroup4;
            } else if (content2 instanceof PathContent) {
                pathGroup = pathGroup3 == null ? new BaseStrokeContent.PathGroup(trimPathContent, pathGroup2) : pathGroup3;
                pathGroup.paths.add((PathContent) content2);
            } else {
                pathGroup = pathGroup3;
            }
            size2--;
            pathGroup3 = pathGroup;
        }
        if (pathGroup3 != null) {
            this.pathGroups.add(pathGroup3);
        }
    }
}
