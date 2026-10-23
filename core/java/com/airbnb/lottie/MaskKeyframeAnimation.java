package com.airbnb.lottie;

import android.graphics.Path;
import java.util.ArrayList;
import java.util.List;

class MaskKeyframeAnimation {
    private final List<BaseKeyframeAnimation<?, Path>> maskAnimations;
    private final List<Mask> masks;

    MaskKeyframeAnimation(List<Mask> list) {
        this.masks = list;
        this.maskAnimations = new ArrayList(list.size());
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= list.size()) {
                return;
            }
            this.maskAnimations.add(list.get(i2).getMaskPath().createAnimation());
            i = i2 + 1;
        }
    }

    List<BaseKeyframeAnimation<?, Path>> getMaskAnimations() {
        return this.maskAnimations;
    }

    List<Mask> getMasks() {
        return this.masks;
    }
}
