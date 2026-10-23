package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import java.util.List;

public class GradientStrokeContent extends BaseStrokeContent {
    private static final int CACHE_STEPS_MS = 32;
    private final RectF boundsRect;
    private final int cacheSteps;
    private final KeyframeAnimation<GradientColor> colorAnimation;
    private final KeyframeAnimation<PointF> endPointAnimation;
    private final LongSparseArray<LinearGradient> linearGradientCache;
    private final String name;
    private final LongSparseArray<RadialGradient> radialGradientCache;
    private final KeyframeAnimation<PointF> startPointAnimation;
    private final GradientType type;

    GradientStrokeContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, GradientStroke gradientStroke) {
        super(lottieDrawable, baseLayer, gradientStroke.getCapType().toPaintCap(), gradientStroke.getJoinType().toPaintJoin(), gradientStroke.getOpacity(), gradientStroke.getWidth(), gradientStroke.getLineDashPattern(), gradientStroke.getDashOffset());
        this.linearGradientCache = new LongSparseArray<>();
        this.radialGradientCache = new LongSparseArray<>();
        this.boundsRect = new RectF();
        this.name = gradientStroke.getName();
        this.type = gradientStroke.getGradientType();
        this.cacheSteps = (int) (lottieDrawable.getComposition().getDuration() / 32);
        this.colorAnimation = gradientStroke.getGradientColor().createAnimation();
        this.colorAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.colorAnimation);
        this.startPointAnimation = gradientStroke.getStartPoint().createAnimation();
        this.startPointAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.startPointAnimation);
        this.endPointAnimation = gradientStroke.getEndPoint().createAnimation();
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
        LinearGradient linearGradient2 = new LinearGradient((int) (this.boundsRect.left + (this.boundsRect.width() / 2.0f) + pointF.x), (int) (pointF.y + this.boundsRect.top + (this.boundsRect.height() / 2.0f)), (int) (this.boundsRect.left + (this.boundsRect.width() / 2.0f) + pointF2.x), (int) (this.boundsRect.top + (this.boundsRect.height() / 2.0f) + pointF2.y), gradientColor.getColors(), gradientColor.getPositions(), TileMode.CLAMP);
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
        int iWidth = (int) (this.boundsRect.left + (this.boundsRect.width() / 2.0f) + pointF.x);
        int iHeight = (int) (pointF.y + this.boundsRect.top + (this.boundsRect.height() / 2.0f));
        RadialGradient radialGradient2 = new RadialGradient(iWidth, iHeight, (float) Math.hypot(((int) ((this.boundsRect.left + (this.boundsRect.width() / 2.0f)) + pointF2.x)) - iWidth, ((int) (pointF2.y + (this.boundsRect.top + (this.boundsRect.height() / 2.0f)))) - iHeight), colors, positions, TileMode.CLAMP);
        this.radialGradientCache.put(gradientHash, radialGradient2);
        return radialGradient2;
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix, int i) {
        getBounds(this.boundsRect, matrix);
        if (this.type == GradientType.Linear) {
            this.paint.setShader(getLinearGradient());
        } else {
            this.paint.setShader(getRadialGradient());
        }
        super.draw(canvas, matrix, i);
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        super.getBounds(rectF, matrix);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void onValueChanged() {
        super.onValueChanged();
    }

    @Override
    public void setContents(List list, List list2) {
        super.setContents(list, list2);
    }
}
