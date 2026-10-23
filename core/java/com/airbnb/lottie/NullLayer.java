package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

class NullLayer extends BaseLayer {
    NullLayer(LottieDrawable lottieDrawable, Layer layer) {
        super(lottieDrawable, layer);
    }

    @Override
    void drawLayer(Canvas canvas, Matrix matrix, int i) {
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        super.getBounds(rectF, matrix);
        rectF.set(0.0f, 0.0f, 0.0f, 0.0f);
    }
}
