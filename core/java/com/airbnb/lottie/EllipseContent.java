package com.airbnb.lottie;

import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import java.util.List;

class EllipseContent implements PathContent, BaseKeyframeAnimation.AnimationListener {
    private static final float ELLIPSE_CONTROL_POINT_PERCENTAGE = 0.55228f;
    private boolean isPathValid;
    private final LottieDrawable lottieDrawable;
    private final String name;
    private final Path path = new Path();
    private final BaseKeyframeAnimation<?, PointF> positionAnimation;
    private final BaseKeyframeAnimation<?, PointF> sizeAnimation;

    @Nullable
    private TrimPathContent trimPath;

    EllipseContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, CircleShape circleShape) {
        this.name = circleShape.getName();
        this.lottieDrawable = lottieDrawable;
        this.sizeAnimation = circleShape.getSize().createAnimation();
        this.positionAnimation = circleShape.getPosition().createAnimation();
        baseLayer.addAnimation(this.sizeAnimation);
        baseLayer.addAnimation(this.positionAnimation);
        this.sizeAnimation.addUpdateListener(this);
        this.positionAnimation.addUpdateListener(this);
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
        float f3 = f * ELLIPSE_CONTROL_POINT_PERCENTAGE;
        float f4 = f2 * ELLIPSE_CONTROL_POINT_PERCENTAGE;
        this.path.reset();
        this.path.moveTo(0.0f, -f2);
        this.path.cubicTo(0.0f + f3, -f2, f, 0.0f - f4, f, 0.0f);
        this.path.cubicTo(f, 0.0f + f4, 0.0f + f3, f2, 0.0f, f2);
        this.path.cubicTo(0.0f - f3, f2, -f, 0.0f + f4, -f, 0.0f);
        this.path.cubicTo(-f, 0.0f - f4, 0.0f - f3, -f2, 0.0f, -f2);
        PointF value2 = this.positionAnimation.getValue();
        this.path.offset(value2.x, value2.y);
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
