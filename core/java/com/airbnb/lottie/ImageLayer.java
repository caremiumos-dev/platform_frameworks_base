package com.airbnb.lottie;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class ImageLayer extends BaseLayer {
    private final float density;
    private final Rect dst;
    private final Paint paint;
    private final Rect src;

    ImageLayer(LottieDrawable lottieDrawable, Layer layer, float f) {
        super(lottieDrawable, layer);
        this.paint = new Paint(3);
        this.src = new Rect();
        this.dst = new Rect();
        this.density = f;
    }

    @Nullable
    private Bitmap getBitmap() {
        return this.lottieDrawable.getImageAsset(this.layerModel.getRefId());
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public void drawLayer(@NonNull Canvas canvas, Matrix matrix, int i) {
        Bitmap bitmap = getBitmap();
        if (bitmap == null) {
            return;
        }
        this.paint.setAlpha(i);
        canvas.save();
        canvas.concat(matrix);
        this.src.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        this.dst.set(0, 0, (int) (bitmap.getWidth() * this.density), (int) (bitmap.getHeight() * this.density));
        canvas.drawBitmap(bitmap, this.src, this.dst, this.paint);
        canvas.restore();
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        super.getBounds(rectF, matrix);
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            rectF.set(rectF.left, rectF.top, Math.min(rectF.right, bitmap.getWidth()), Math.min(rectF.bottom, bitmap.getHeight()));
            this.boundsMatrix.mapRect(rectF);
        }
    }
}
