package com.airbnb.lottie;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import java.util.List;

class PathKeyframeAnimation extends KeyframeAnimation<PointF> {
    private PathMeasure pathMeasure;
    private PathKeyframe pathMeasureKeyframe;
    private final PointF point;
    private final float[] pos;

    PathKeyframeAnimation(List<? extends Keyframe<PointF>> list) {
        super(list);
        this.point = new PointF();
        this.pos = new float[2];
    }

    @Override
    public PointF getValue(Keyframe<PointF> keyframe, float f) {
        PathKeyframe pathKeyframe = (PathKeyframe) keyframe;
        Path path = pathKeyframe.getPath();
        if (path == null) {
            return keyframe.startValue;
        }
        if (this.pathMeasureKeyframe != pathKeyframe) {
            this.pathMeasure = new PathMeasure(path, false);
            this.pathMeasureKeyframe = pathKeyframe;
        }
        this.pathMeasure.getPosTan(this.pathMeasure.getLength() * f, this.pos, null);
        this.point.set(this.pos[0], this.pos[1]);
        return this.point;
    }

}
