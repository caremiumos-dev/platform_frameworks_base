package com.airbnb.lottie;

import android.graphics.PointF;
import java.util.List;

class PointKeyframeAnimation extends KeyframeAnimation<PointF> {
    private final PointF point;

    PointKeyframeAnimation(List<Keyframe<PointF>> list) {
        super(list);
        this.point = new PointF();
    }

    @Override
    public PointF getValue(Keyframe<PointF> keyframe, float f) {
        if (keyframe.startValue == null || keyframe.endValue == null) {
            throw new IllegalStateException("Missing values for keyframe.");
        }
        PointF pointF = keyframe.startValue;
        PointF pointF2 = keyframe.endValue;
        this.point.set(pointF.x + ((pointF2.x - pointF.x) * f), ((pointF2.y - pointF.y) * f) + pointF.y);
        return this.point;
    }

}
