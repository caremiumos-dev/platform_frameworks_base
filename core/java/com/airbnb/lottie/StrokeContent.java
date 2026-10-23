package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.support.annotation.Nullable;

class StrokeContent extends BaseStrokeContent {
    private final KeyframeAnimation<Integer> colorAnimation;
    private final String name;

    StrokeContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, ShapeStroke shapeStroke) {
        super(lottieDrawable, baseLayer, shapeStroke.getCapType().toPaintCap(), shapeStroke.getJoinType().toPaintJoin(), shapeStroke.getOpacity(), shapeStroke.getWidth(), shapeStroke.getLineDashPattern(), shapeStroke.getDashOffset());
        this.name = shapeStroke.getName();
        this.colorAnimation = shapeStroke.getColor().createAnimation();
        this.colorAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.colorAnimation);
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix, int i) {
        this.paint.setColor(((Integer) this.colorAnimation.getValue()).intValue());
        super.draw(canvas, matrix, i);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
