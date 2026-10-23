package com.airbnb.lottie;

import android.graphics.Color;
import org.json.JSONArray;

class ColorFactory implements AnimatableValue.Factory<Integer> {
    static final ColorFactory INSTANCE = new ColorFactory();

    ColorFactory() {
    }

    @Override
    public Integer valueFromObject(Object obj, float f) {
        JSONArray jSONArray = (JSONArray) obj;
        if (jSONArray.length() != 4) {
            return -16777216;
        }
        boolean z = true;
        for (int i = 0; i < jSONArray.length(); i++) {
            if (jSONArray.optDouble(i) > 1.0d) {
                z = false;
            }
        }
        float f2 = z ? 255.0f : 1.0f;
        return Integer.valueOf(Color.argb((int) (jSONArray.optDouble(3) * ((double) f2)), (int) (jSONArray.optDouble(0) * ((double) f2)), (int) (jSONArray.optDouble(1) * ((double) f2)), (int) (jSONArray.optDouble(2) * ((double) f2))));
    }
}
