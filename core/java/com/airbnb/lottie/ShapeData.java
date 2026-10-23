package com.airbnb.lottie;

import android.graphics.PointF;
import android.support.annotation.FloatRange;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class ShapeData {
    private boolean closed;
    private final List<CubicCurveData> curves;
    private PointF initialPoint;

    static class Factory implements AnimatableValue.Factory<ShapeData> {
        static final ShapeData.Factory INSTANCE = new ShapeData.Factory();

        private Factory() {
        }

        private static PointF vertexAtIndex(int i, JSONArray jSONArray) {
            if (i >= jSONArray.length()) {
                throw new IllegalArgumentException("Invalid index " + i + ". There are only " + jSONArray.length() + " points.");
            }
            JSONArray jSONArrayOptJSONArray = jSONArray.optJSONArray(i);
            Object objOpt = jSONArrayOptJSONArray.opt(0);
            Object objOpt2 = jSONArrayOptJSONArray.opt(1);
            return new PointF(objOpt instanceof Double ? new Float(((Double) objOpt).doubleValue()).floatValue() : ((Integer) objOpt).intValue(), objOpt2 instanceof Double ? new Float(((Double) objOpt2).doubleValue()).floatValue() : ((Integer) objOpt2).intValue());
        }

        @Override
        public ShapeData valueFromObject(Object obj, float f) {
            JSONObject jSONObject;
            if (obj instanceof JSONArray) {
                Object objOpt = ((JSONArray) obj).opt(0);
                if ((objOpt instanceof JSONObject) && ((JSONObject) objOpt).has("v")) {
                    jSONObject = (JSONObject) objOpt;
                } else {
                    jSONObject = null;
                }
            } else if ((obj instanceof JSONObject) && ((JSONObject) obj).has("v")) {
                jSONObject = (JSONObject) obj;
            } else {
                jSONObject = null;
            }
            if (jSONObject == null) {
                return null;
            }
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("v");
            JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("i");
            JSONArray jSONArrayOptJSONArray3 = jSONObject.optJSONArray("o");
            boolean zOptBoolean = jSONObject.optBoolean("c", false);
            if (jSONArrayOptJSONArray == null || jSONArrayOptJSONArray2 == null || jSONArrayOptJSONArray3 == null || jSONArrayOptJSONArray.length() != jSONArrayOptJSONArray2.length() || jSONArrayOptJSONArray.length() != jSONArrayOptJSONArray3.length()) {
                throw new IllegalStateException("Unable to process points array or tangents. " + jSONObject);
            }
            if (jSONArrayOptJSONArray.length() == 0) {
                return new ShapeData(new PointF(), false, Collections.emptyList(), null);
            }
            int length = jSONArrayOptJSONArray.length();
            PointF pointFVertexAtIndex = vertexAtIndex(0, jSONArrayOptJSONArray);
            pointFVertexAtIndex.x *= f;
            pointFVertexAtIndex.y *= f;
            ArrayList arrayList = new ArrayList(length);
            for (int i = 1; i < length; i++) {
                PointF pointFVertexAtIndex2 = vertexAtIndex(i, jSONArrayOptJSONArray);
                PointF pointFVertexAtIndex3 = vertexAtIndex(i - 1, jSONArrayOptJSONArray);
                PointF pointFVertexAtIndex4 = vertexAtIndex(i - 1, jSONArrayOptJSONArray3);
                PointF pointFVertexAtIndex5 = vertexAtIndex(i, jSONArrayOptJSONArray2);
                PointF pointFAddPoints = MiscUtils.addPoints(pointFVertexAtIndex3, pointFVertexAtIndex4);
                PointF pointFAddPoints2 = MiscUtils.addPoints(pointFVertexAtIndex2, pointFVertexAtIndex5);
                pointFAddPoints.x *= f;
                pointFAddPoints.y *= f;
                pointFAddPoints2.x *= f;
                pointFAddPoints2.y *= f;
                pointFVertexAtIndex2.x *= f;
                pointFVertexAtIndex2.y *= f;
                arrayList.add(new CubicCurveData(pointFAddPoints, pointFAddPoints2, pointFVertexAtIndex2));
            }
            if (zOptBoolean) {
                PointF pointFVertexAtIndex6 = vertexAtIndex(0, jSONArrayOptJSONArray);
                PointF pointFVertexAtIndex7 = vertexAtIndex(length - 1, jSONArrayOptJSONArray);
                PointF pointFVertexAtIndex8 = vertexAtIndex(length - 1, jSONArrayOptJSONArray3);
                PointF pointFVertexAtIndex9 = vertexAtIndex(0, jSONArrayOptJSONArray2);
                PointF pointFAddPoints3 = MiscUtils.addPoints(pointFVertexAtIndex7, pointFVertexAtIndex8);
                PointF pointFAddPoints4 = MiscUtils.addPoints(pointFVertexAtIndex6, pointFVertexAtIndex9);
                if (f != 1.0f) {
                    pointFAddPoints3.x *= f;
                    pointFAddPoints3.y *= f;
                    pointFAddPoints4.x *= f;
                    pointFAddPoints4.y *= f;
                    pointFVertexAtIndex6.x *= f;
                    pointFVertexAtIndex6.y *= f;
                }
                arrayList.add(new CubicCurveData(pointFAddPoints3, pointFAddPoints4, pointFVertexAtIndex6));
            }
            return new ShapeData(pointFVertexAtIndex, zOptBoolean, arrayList, null);
        }
    }

    ShapeData() {
        this.curves = new ArrayList();
    }

    private ShapeData(PointF pointF, boolean z, List<CubicCurveData> list) {
        this.curves = new ArrayList();
        this.initialPoint = pointF;
        this.closed = z;
        this.curves.addAll(list);
    }

     ShapeData(PointF pointF, boolean z, List list, ShapeData shapeData) {
        this(pointF, z, list);
    }

    private void setInitialPoint(float f, float f2) {
        if (this.initialPoint == null) {
            this.initialPoint = new PointF();
        }
        this.initialPoint.set(f, f2);
    }

    List<CubicCurveData> getCurves() {
        return this.curves;
    }

    PointF getInitialPoint() {
        return this.initialPoint;
    }

    void interpolateBetween(ShapeData shapeData, ShapeData shapeData2, @FloatRange(from = 0.0f, to = 1.0f) float f) {
        if (this.initialPoint == null) {
            this.initialPoint = new PointF();
        }
        this.closed = !shapeData.isClosed() ? shapeData2.isClosed() : true;
        if (!this.curves.isEmpty() && this.curves.size() != shapeData.getCurves().size() && this.curves.size() != shapeData2.getCurves().size()) {
            throw new IllegalStateException("Curves must have the same number of control points. This: " + getCurves().size() + "\tShape 1: " + shapeData.getCurves().size() + "\tShape 2: " + shapeData2.getCurves().size());
        }
        if (this.curves.isEmpty()) {
            for (int size = shapeData.getCurves().size() - 1; size >= 0; size--) {
                this.curves.add(new CubicCurveData());
            }
        }
        PointF initialPoint = shapeData.getInitialPoint();
        PointF initialPoint2 = shapeData2.getInitialPoint();
        setInitialPoint(MiscUtils.lerp(initialPoint.x, initialPoint2.x, f), MiscUtils.lerp(initialPoint.y, initialPoint2.y, f));
        for (int size2 = this.curves.size() - 1; size2 >= 0; size2--) {
            CubicCurveData cubicCurveData = shapeData.getCurves().get(size2);
            CubicCurveData cubicCurveData2 = shapeData2.getCurves().get(size2);
            PointF controlPoint1 = cubicCurveData.getControlPoint1();
            PointF controlPoint2 = cubicCurveData.getControlPoint2();
            PointF vertex = cubicCurveData.getVertex();
            PointF controlPoint3 = cubicCurveData2.getControlPoint1();
            PointF controlPoint4 = cubicCurveData2.getControlPoint2();
            PointF vertex2 = cubicCurveData2.getVertex();
            this.curves.get(size2).setControlPoint1(MiscUtils.lerp(controlPoint1.x, controlPoint3.x, f), MiscUtils.lerp(controlPoint1.y, controlPoint3.y, f));
            this.curves.get(size2).setControlPoint2(MiscUtils.lerp(controlPoint2.x, controlPoint4.x, f), MiscUtils.lerp(controlPoint2.y, controlPoint4.y, f));
            this.curves.get(size2).setVertex(MiscUtils.lerp(vertex.x, vertex2.x, f), MiscUtils.lerp(vertex.y, vertex2.y, f));
        }
    }

    boolean isClosed() {
        return this.closed;
    }

    public String toString() {
        return "ShapeData{numCurves=" + this.curves.size() + "closed=" + this.closed + '}';
    }
}
