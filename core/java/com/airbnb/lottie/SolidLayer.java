package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.support.annotation.Nullable;

class SolidLayer extends BaseLayer {
    private final Layer layerModel;
    private final Paint paint;
    private final RectF rect;

    SolidLayer(LottieDrawable lottieDrawable, Layer layer) {
        super(lottieDrawable, layer);
        this.rect = new RectF();
        this.paint = new Paint();
        this.layerModel = layer;
        this.paint.setAlpha(0);
        this.paint.setStyle(Style.FILL);
        this.paint.setColor(layer.getSolidColor());
    }

    private void updateRect(Matrix matrix) {
        this.rect.set(0.0f, 0.0f, this.layerModel.getSolidWidth(), this.layerModel.getSolidHeight());
        matrix.mapRect(this.rect);
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public void drawLayer(Canvas canvas, Matrix matrix, int i) {
        int iAlpha = Color.alpha(this.layerModel.getSolidColor());
        if (iAlpha == 0) {
            return;
        }
        int iIntValue = (int) (((this.transform.getOpacity().getValue().intValue() * (iAlpha / 255.0f)) / 100.0f) * 255.0f);
        this.paint.setAlpha(iIntValue);
        if (iIntValue > 0) {
            updateRect(matrix);
            canvas.drawRect(this.rect, this.paint);
        }
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        super.getBounds(rectF, matrix);
        updateRect(this.boundsMatrix);
        rectF.set(this.rect);
    }
}
