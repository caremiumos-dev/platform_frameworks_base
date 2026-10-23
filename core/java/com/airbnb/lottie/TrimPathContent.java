package com.airbnb.lottie;

import java.util.ArrayList;
import java.util.List;

class TrimPathContent implements Content, BaseKeyframeAnimation.AnimationListener {
    private final BaseKeyframeAnimation<?, Float> endAnimation;
    private final List<BaseKeyframeAnimation.AnimationListener> listeners = new ArrayList();
    private String name;
    private final BaseKeyframeAnimation<?, Float> offsetAnimation;
    private final BaseKeyframeAnimation<?, Float> startAnimation;
    private final ShapeTrimPath.Type type;

    TrimPathContent(BaseLayer baseLayer, ShapeTrimPath shapeTrimPath) {
        this.name = shapeTrimPath.getName();
        this.type = shapeTrimPath.getType();
        this.startAnimation = shapeTrimPath.getStart().createAnimation();
        this.endAnimation = shapeTrimPath.getEnd().createAnimation();
        this.offsetAnimation = shapeTrimPath.getOffset().createAnimation();
        baseLayer.addAnimation(this.startAnimation);
        baseLayer.addAnimation(this.endAnimation);
        baseLayer.addAnimation(this.offsetAnimation);
        this.startAnimation.addUpdateListener(this);
        this.endAnimation.addUpdateListener(this);
        this.offsetAnimation.addUpdateListener(this);
    }

    void addListener(BaseKeyframeAnimation.AnimationListener animationListener) {
        this.listeners.add(animationListener);
    }

    public BaseKeyframeAnimation<?, Float> getEnd() {
        return this.endAnimation;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public BaseKeyframeAnimation<?, Float> getOffset() {
        return this.offsetAnimation;
    }

    public BaseKeyframeAnimation<?, Float> getStart() {
        return this.startAnimation;
    }

    ShapeTrimPath.Type getType() {
        return this.type;
    }

    @Override
    public void onValueChanged() {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.listeners.size()) {
                return;
            }
            this.listeners.get(i2).onValueChanged();
            i = i2 + 1;
        }
    }

    @Override
    public void setContents(List<Content> list, List<Content> list2) {
    }
}
