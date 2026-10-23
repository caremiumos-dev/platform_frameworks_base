package com.airbnb.lottie;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.io.Closeable;

final class Utils {
    private static DisplayMetrics displayMetrics;
    private static final PathMeasure pathMeasure = new PathMeasure();
    private static final Path tempPath = new Path();
    private static final Path tempPath2 = new Path();
    private static final float[] points = new float[4];
    private static final float SQRT_2 = (float) Math.sqrt(2.0d);

    private Utils() {
    }

    static void applyTrimPathIfNeeded(Path path, float f, float f2, float f3) {
        pathMeasure.setPath(path, false);
        float length = pathMeasure.getLength();
        if ((f == 1.0f && f2 == 0.0f) || length == 0.0f || Math.abs((f2 - f) - 1.0f) < 0.01d) {
            return;
        }
        float f4 = length * f;
        float f5 = length * f2;
        float fMin = Math.min(f4, f5);
        float fMax = Math.max(f4, f5);
        float f6 = f3 * length;
        float fFloorMod = fMin + f6;
        float fFloorMod2 = fMax + f6;
        if (fFloorMod >= length && fFloorMod2 >= length) {
            fFloorMod = MiscUtils.floorMod(fFloorMod, length);
            fFloorMod2 = MiscUtils.floorMod(fFloorMod2, length);
        }
        if (fFloorMod < 0.0f) {
            fFloorMod = MiscUtils.floorMod(fFloorMod, length);
        }
        if (fFloorMod2 < 0.0f) {
            fFloorMod2 = MiscUtils.floorMod(fFloorMod2, length);
        }
        if (fFloorMod == fFloorMod2) {
            path.reset();
            return;
        }
        if (fFloorMod >= fFloorMod2) {
            fFloorMod -= length;
        }
        tempPath.reset();
        pathMeasure.getSegment(fFloorMod, fFloorMod2, tempPath, true);
        if (fFloorMod2 > length) {
            tempPath2.reset();
            pathMeasure.getSegment(0.0f, fFloorMod2 % length, tempPath2, true);
            tempPath.addPath(tempPath2);
        } else if (fFloorMod < 0.0f) {
            tempPath2.reset();
            pathMeasure.getSegment(fFloorMod + length, length, tempPath2, true);
            tempPath.addPath(tempPath2);
        }
        path.set(tempPath);
    }

    static void applyTrimPathIfNeeded(Path path, @Nullable TrimPathContent trimPathContent) {
        if (trimPathContent == null) {
            return;
        }
        applyTrimPathIfNeeded(path, trimPathContent.getStart().getValue().floatValue() / 100.0f, trimPathContent.getEnd().getValue().floatValue() / 100.0f, trimPathContent.getOffset().getValue().floatValue() / 360.0f);
    }

    static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e2) {
            }
        }
    }

    static Path createPath(PointF pointF, PointF pointF2, PointF pointF3, PointF pointF4) {
        Path path = new Path();
        path.moveTo(pointF.x, pointF.y);
        if (pointF3 == null || pointF4 == null || (pointF3.length() == 0.0f && pointF4.length() == 0.0f)) {
            path.lineTo(pointF2.x, pointF2.y);
        } else {
            path.cubicTo(pointF.x + pointF3.x, pointF.y + pointF3.y, pointF2.x + pointF4.x, pointF2.y + pointF4.y, pointF2.x, pointF2.y);
        }
        return path;
    }

    static float getScale(Matrix matrix) {
        points[0] = 0.0f;
        points[1] = 0.0f;
        points[2] = SQRT_2;
        points[3] = SQRT_2;
        matrix.mapPoints(points);
        return ((float) Math.hypot(points[2] - points[0], points[3] - points[1])) / 2.0f;
    }

    static int getScreenHeight(Context context) {
        if (displayMetrics == null) {
            displayMetrics = new DisplayMetrics();
        }
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    static int getScreenWidth(Context context) {
        if (displayMetrics == null) {
            displayMetrics = new DisplayMetrics();
        }
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }
}
