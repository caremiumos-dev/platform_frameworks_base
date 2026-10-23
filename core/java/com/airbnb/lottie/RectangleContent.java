package com.airbnb.lottie;

import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import java.util.List;

class RectangleContent implements PathContent, BaseKeyframeAnimation.AnimationListener {
    private final BaseKeyframeAnimation<?, Float> cornerRadiusAnimation;
    private boolean isPathValid;
    private final LottieDrawable lottieDrawable;
    private final String name;
    private final BaseKeyframeAnimation<?, PointF> positionAnimation;
    private final BaseKeyframeAnimation<?, PointF> sizeAnimation;

    @Nullable
    private TrimPathContent trimPath;
    private final Path path = new Path();
    private final RectF rect = new RectF();

    RectangleContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, RectangleShape rectangleShape) {
        this.name = rectangleShape.getName();
        this.lottieDrawable = lottieDrawable;
        this.positionAnimation = rectangleShape.getPosition().createAnimation();
        this.sizeAnimation = rectangleShape.getSize().createAnimation();
        this.cornerRadiusAnimation = rectangleShape.getCornerRadius().createAnimation();
        baseLayer.addAnimation(this.positionAnimation);
        baseLayer.addAnimation(this.sizeAnimation);
        baseLayer.addAnimation(this.cornerRadiusAnimation);
        this.positionAnimation.addUpdateListener(this);
        this.sizeAnimation.addUpdateListener(this);
        this.cornerRadiusAnimation.addUpdateListener(this);
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
        PointF value = this.sizeAnimation.getValue();
        float f = value.x / 2.0f;
        float f2 = value.y / 2.0f;
        float fFloatValue = this.cornerRadiusAnimation == null ? 0.0f : this.cornerRadiusAnimation.getValue().floatValue();
        float fMin = Math.min(f, f2);
        if (fFloatValue <= fMin) {
            fMin = fFloatValue;
        }
        PointF value2 = this.positionAnimation.getValue();
        this.path.moveTo(value2.x + f, (value2.y - f2) + fMin);
        this.path.lineTo(value2.x + f, (value2.y + f2) - fMin);
        if (fMin > 0.0f) {
            this.rect.set((value2.x + f) - (2.0f * fMin), (value2.y + f2) - (2.0f * fMin), value2.x + f, value2.y + f2);
            this.path.arcTo(this.rect, 0.0f, 90.0f, false);
        }
        this.path.lineTo((value2.x - f) + fMin, value2.y + f2);
        if (fMin > 0.0f) {
            this.rect.set(value2.x - f, (value2.y + f2) - (2.0f * fMin), (value2.x - f) + (2.0f * fMin), value2.y + f2);
            this.path.arcTo(this.rect, 90.0f, 90.0f, false);
        }
        this.path.lineTo(value2.x - f, (value2.y - f2) + fMin);
        if (fMin > 0.0f) {
            this.rect.set(value2.x - f, value2.y - f2, (value2.x - f) + (2.0f * fMin), (value2.y - f2) + (2.0f * fMin));
            this.path.arcTo(this.rect, 180.0f, 90.0f, false);
        }
        this.path.lineTo((value2.x + f) - fMin, value2.y - f2);
        if (fMin > 0.0f) {
            this.rect.set((value2.x + f) - (2.0f * fMin), value2.y - f2, f + value2.x, (value2.y - f2) + (fMin * 2.0f));
            this.path.arcTo(this.rect, 270.0f, 90.0f, false);
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
