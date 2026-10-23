package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.Collections;

class ShapeLayer extends BaseLayer {
    private final ContentGroup contentGroup;

    ShapeLayer(LottieDrawable lottieDrawable, Layer layer) {
        super(lottieDrawable, layer);
        this.contentGroup = new ContentGroup(lottieDrawable, this, new ShapeGroup(layer.getName(), layer.getShapes()));
        this.contentGroup.setContents(Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
        this.contentGroup.addColorFilter(str, str2, colorFilter);
    }

    @Override
    void drawLayer(@NonNull Canvas canvas, Matrix matrix, int i) {
        this.contentGroup.draw(canvas, matrix, i);
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        super.getBounds(rectF, matrix);
        this.contentGroup.getBounds(rectF, this.boundsMatrix);
    }
}
