package com.airbnb.lottie;

import android.support.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

class ShapeGroup {
    private final List<Object> items;
    private final String name;

    static class Factory {
        private Factory() {
        }

        public static ShapeGroup newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("it");
            String strOptString = jSONObject.optString("nm");
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                Object objShapeItemWithJson = ShapeGroup.shapeItemWithJson(jSONArrayOptJSONArray.optJSONObject(i), lottieComposition);
                if (objShapeItemWithJson != null) {
                    arrayList.add(objShapeItemWithJson);
                }
            }
            return new ShapeGroup(strOptString, arrayList);
        }
    }

    ShapeGroup(String str, List<Object> list) {
        this.name = str;
        this.items = list;
    }

    @Nullable
    static Object shapeItemWithJson(JSONObject jSONObject, LottieComposition lottieComposition) {
        String strOptString = jSONObject.optString("ty");
        if (strOptString.equals("gr")) {
            return ShapeGroup.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("st")) {
            return ShapeStroke.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("gs")) {
            return GradientStroke.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("fl")) {
            return ShapeFill.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("gf")) {
            return GradientFill.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("tr")) {
            return AnimatableTransform.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("sh")) {
            return ShapePath.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("el")) {
            return CircleShape.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("rc")) {
            return RectangleShape.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("tm")) {
            return ShapeTrimPath.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("sr")) {
            return PolystarShape.Factory.newInstance(jSONObject, lottieComposition);
        }
        if (strOptString.equals("mm")) {
            return MergePaths.Factory.newInstance(jSONObject);
        }
        Log.w("LOTTIE", "Unknown shape type " + strOptString);
        return null;
    }

    List<Object> getItems() {
        return this.items;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return "ShapeGroup{name='" + this.name + "' Shapes: " + Arrays.toString(this.items.toArray()) + '}';
    }
}
