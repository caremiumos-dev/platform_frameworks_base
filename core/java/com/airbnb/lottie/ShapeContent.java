package com.airbnb.lottie;

import android.graphics.Path;
import android.graphics.Path.FillType;
import android.support.annotation.Nullable;
import java.util.List;

class ShapeContent implements PathContent, BaseKeyframeAnimation.AnimationListener {
    private boolean isPathValid;
    private final LottieDrawable lottieDrawable;
    private final String name;
    private final Path path = new Path();
    private final BaseKeyframeAnimation<?, Path> shapeAnimation;

    @Nullable
    private TrimPathContent trimPath;

    ShapeContent(LottieDrawable lottieDrawable, BaseLayer baseLayer, ShapePath shapePath) {
        this.name = shapePath.getName();
        this.lottieDrawable = lottieDrawable;
        this.shapeAnimation = shapePath.getShapePath().createAnimation();
        baseLayer.addAnimation(this.shapeAnimation);
        this.shapeAnimation.addUpdateListener(this);
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
        this.path.set(this.shapeAnimation.getValue());
        this.path.setFillType(FillType.EVEN_ODD);
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
