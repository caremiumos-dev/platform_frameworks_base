package com.airbnb.lottie;

import android.graphics.PointF;
import org.json.JSONArray;
import org.json.JSONObject;

class PointFFactory implements AnimatableValue.Factory<PointF> {
    static final PointFFactory INSTANCE = new PointFFactory();

    private PointFFactory() {
    }

    @Override
    public PointF valueFromObject(Object obj, float f) {
        if (obj instanceof JSONArray) {
            return JsonUtils.pointFromJsonArray((JSONArray) obj, f);
        }
        if (obj instanceof JSONObject) {
            return JsonUtils.pointFromJsonObject((JSONObject) obj, f);
        }
        throw new IllegalArgumentException("Unable to parse point from " + obj);
    }
}
