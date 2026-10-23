package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import java.util.ArrayList;
import java.util.List;

class GradientFillContent implements DrawingContent, BaseKeyframeAnimation.AnimationListener {
    private static final int CACHE_STEPS_MS = 32;
    private final int cacheSteps;
    private final KeyframeAnimation<GradientColor> colorAnimation;
    private final KeyframeAnimation<PointF> endPointAnimation;
    private final LottieDrawable lottieDrawable;
    private final String name;
    private final KeyframeAnimation<Integer> opacityAnimation;
    private final KeyframeAnimation<PointF> startPointAnimation;
    private final GradientType type;
    private final LongSparseArray<LinearGradient> linearGradientCache = new LongSparseArray<>();
    private final LongSparseArray<RadialGradient> radialGradientCache = new LongSparseArray<>();
    private final Matrix shaderMatrix = new Matrix();
    private final Path path = new Path();
    private final Paint paint = new Paint(1);
    private final RectF boundsRect = new RectF();
    private final List<PathContent> paths = new ArrayList();

    GradientFillContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, GradientFill gradientFill) {
        this.name = gradientFill.getName();
        this.lottieDrawable = lottieDrawable;
        this.type = gradientFill.getGradientType();
        this.path.setFillType(gradientFill.getFillType());
        this.cacheSteps = (int) (lottieDrawable.getComposition().getDuration() / 32);
        this.colorAnimation = gradientFill.getGradientColor().createAnimation();
        this.colorAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.colorAnimation);
        this.opacityAnimation = gradientFill.getOpacity().createAnimation();
        this.opacityAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.opacityAnimation);
        this.startPointAnimation = gradientFill.getStartPoint().createAnimation();
        this.startPointAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.startPointAnimation);
        this.endPointAnimation = gradientFill.getEndPoint().createAnimation();
        this.endPointAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.endPointAnimation);
    }

    private int getGradientHash() {
        int iRound = Math.round(this.startPointAnimation.getProgress() * this.cacheSteps);
        int iRound2 = Math.round(this.endPointAnimation.getProgress() * this.cacheSteps);
        int iRound3 = Math.round(this.colorAnimation.getProgress() * this.cacheSteps);
        int i = iRound != 0 ? iRound * 527 : 17;
        if (iRound2 != 0) {
            i = i * 31 * iRound2;
        }
        return iRound3 != 0 ? i * 31 * iRound3 : i;
    }

    private LinearGradient getLinearGradient() {
        int gradientHash = getGradientHash();
        LinearGradient linearGradient = this.linearGradientCache.get(gradientHash);
        if (linearGradient != null) {
            return linearGradient;
        }
        PointF pointF = (PointF) this.startPointAnimation.getValue();
        PointF pointF2 = (PointF) this.endPointAnimation.getValue();
        GradientColor gradientColor = (GradientColor) this.colorAnimation.getValue();
        LinearGradient linearGradient2 = new LinearGradient(pointF.x, pointF.y, pointF2.x, pointF2.y, gradientColor.getColors(), gradientColor.getPositions(), TileMode.CLAMP);
        this.linearGradientCache.put(gradientHash, linearGradient2);
        return linearGradient2;
    }

    private RadialGradient getRadialGradient() {
        int gradientHash = getGradientHash();
        RadialGradient radialGradient = this.radialGradientCache.get(gradientHash);
        if (radialGradient != null) {
            return radialGradient;
        }
        PointF pointF = (PointF) this.startPointAnimation.getValue();
        PointF pointF2 = (PointF) this.endPointAnimation.getValue();
        GradientColor gradientColor = (GradientColor) this.colorAnimation.getValue();
        int[] colors = gradientColor.getColors();
        float[] positions = gradientColor.getPositions();
        float f = pointF.x;
        float f2 = pointF.y;
        RadialGradient radialGradient2 = new RadialGradient(f, f2, (float) Math.hypot(pointF2.x - f, pointF2.y - f2), colors, positions, TileMode.CLAMP);
        this.radialGradientCache.put(gradientHash, radialGradient2);
        return radialGradient2;
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix, int i) {
        this.path.reset();
        for (int i2 = 0; i2 < this.paths.size(); i2++) {
            this.path.addPath(this.paths.get(i2).getPath(), matrix);
        }
        this.path.computeBounds(this.boundsRect, false);
        Shader linearGradient = this.type == GradientType.Linear ? getLinearGradient() : getRadialGradient();
        this.shaderMatrix.set(matrix);
        linearGradient.setLocalMatrix(this.shaderMatrix);
        this.paint.setShader(linearGradient);
        this.paint.setAlpha((int) (((((Integer) this.opacityAnimation.getValue()).intValue() * (i / 255.0f)) / 100.0f) * 255.0f));
        canvas.drawPath(this.path, this.paint);
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        this.path.reset();
        for (int i = 0; i < this.paths.size(); i++) {
            this.path.addPath(this.paths.get(i).getPath(), matrix);
        }
        this.path.computeBounds(rectF, false);
        rectF.set(rectF.left - 1.0f, rectF.top - 1.0f, rectF.right + 1.0f, rectF.bottom + 1.0f);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onValueChanged() {
        this.lottieDrawable.invalidateSelf();
    }

    @Override
    public void setContents(List<Content> list, List<Content> list2) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= list2.size()) {
                return;
            }
            Content content = list2.get(i2);
            if (content instanceof PathContent) {
                this.paths.add((PathContent) content);
            }
            i = i2 + 1;
        }
    }
}
