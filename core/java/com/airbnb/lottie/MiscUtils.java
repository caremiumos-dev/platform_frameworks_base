package com.airbnb.lottie;

import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.FloatRange;

class MiscUtils {
    MiscUtils() {
    }

    static PointF addPoints(PointF pointF, PointF pointF2) {
        return new PointF(pointF.x + pointF2.x, pointF.y + pointF2.y);
    }

    static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    private static int floorDiv(int i, int i2) {
        int i3 = i / i2;
        return ((i ^ i2) >= 0 || i3 * i2 == i) ? i3 : i3 - 1;
    }

    static int floorMod(float f, float f2) {
        return floorMod((int) f, (int) f2);
    }

    static int floorMod(int i, int i2) {
        return i - (floorDiv(i, i2) * i2);
    }

    static void getPathFromData(ShapeData shapeData, Path path) {
        path.reset();
        PointF initialPoint = shapeData.getInitialPoint();
        path.moveTo(initialPoint.x, initialPoint.y);
        PointF pointF = new PointF(initialPoint.x, initialPoint.y);
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= shapeData.getCurves().size()) {
                break;
            }
            CubicCurveData cubicCurveData = shapeData.getCurves().get(i2);
            PointF controlPoint1 = cubicCurveData.getControlPoint1();
            PointF controlPoint2 = cubicCurveData.getControlPoint2();
            PointF vertex = cubicCurveData.getVertex();
            if (controlPoint1.equals(pointF) && controlPoint2.equals(vertex)) {
                path.lineTo(vertex.x, vertex.y);
            } else {
                path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, vertex.x, vertex.y);
            }
            pointF.set(vertex.x, vertex.y);
            i = i2 + 1;
        }
        if (shapeData.isClosed()) {
            path.close();
        }
    }

    static double lerp(double d, double d2, @FloatRange(from = 0.0f, to = 1.0f) double d3) {
        return ((d2 - d) * d3) + d;
    }

    static float lerp(float f, float f2, @FloatRange(from = 0.0f, to = 1.0f) float f3) {
        return ((f2 - f) * f3) + f;
    }

    static int lerp(int i, int i2, @FloatRange(from = 0.0f, to = 1.0f) float f) {
        return (int) (i + ((i2 - i) * f));
    }
}
