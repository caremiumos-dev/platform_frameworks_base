package com.airbnb.lottie;

import android.graphics.PointF;

class CubicCurveData {
    private final PointF controlPoint1;
    private final PointF controlPoint2;
    private final PointF vertex;

    CubicCurveData() {
        this.controlPoint1 = new PointF();
        this.controlPoint2 = new PointF();
        this.vertex = new PointF();
    }

    CubicCurveData(PointF pointF, PointF pointF2, PointF pointF3) {
        this.controlPoint1 = pointF;
        this.controlPoint2 = pointF2;
        this.vertex = pointF3;
    }

    PointF getControlPoint1() {
        return this.controlPoint1;
    }

    PointF getControlPoint2() {
        return this.controlPoint2;
    }

    PointF getVertex() {
        return this.vertex;
    }

    void setControlPoint1(float f, float f2) {
        this.controlPoint1.set(f, f2);
    }

    void setControlPoint2(float f, float f2) {
        this.controlPoint2.set(f, f2);
    }

    void setVertex(float f, float f2) {
        this.vertex.set(f, f2);
    }
}
