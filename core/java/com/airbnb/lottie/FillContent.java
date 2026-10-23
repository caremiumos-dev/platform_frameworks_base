package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class FillContent implements DrawingContent, BaseKeyframeAnimation.AnimationListener {
    private final KeyframeAnimation<Integer> colorAnimation;
    private final LottieDrawable lottieDrawable;
    private final String name;
    private final KeyframeAnimation<Integer> opacityAnimation;
    private final Path path = new Path();
    private final Paint paint = new Paint(1);
    private final List<PathContent> paths = new ArrayList();

    FillContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, ShapeFill shapeFill) {
        this.name = shapeFill.getName();
        this.lottieDrawable = lottieDrawable;
        if (shapeFill.getColor() == null || shapeFill.getOpacity() == null) {
            this.colorAnimation = null;
            this.opacityAnimation = null;
            return;
        }
        this.path.setFillType(shapeFill.getFillType());
        this.colorAnimation = shapeFill.getColor().createAnimation();
        this.colorAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.colorAnimation);
        this.opacityAnimation = shapeFill.getOpacity().createAnimation();
        this.opacityAnimation.addUpdateListener(this);
        baseLayer.addAnimation(this.opacityAnimation);
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix, int i) {
        this.paint.setColor(((Integer) this.colorAnimation.getValue()).intValue());
        this.paint.setAlpha((int) (((((Integer) this.opacityAnimation.getValue()).intValue() * (i / 255.0f)) / 100.0f) * 255.0f));
        this.path.reset();
        int i2 = 0;
        while (true) {
            int i3 = i2;
            if (i3 >= this.paths.size()) {
                canvas.drawPath(this.path, this.paint);
                return;
            } else {
                this.path.addPath(this.paths.get(i3).getPath(), matrix);
                i2 = i3 + 1;
            }
        }
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
