package com.airbnb.lottie;

import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

class Layer {
    private static final String TAG = Layer.class.getSimpleName();
    private final LottieComposition composition;
    private final List<Keyframe<Float>> inOutKeyframes;
    private final long layerId;
    private final String layerName;
    private final Layer.LayerType layerType;
    private final List<Mask> masks;
    private final Layer.MatteType matteType;
    private final long parentId;
    private final int preCompHeight;
    private final int preCompWidth;

    @Nullable
    private final String refId;
    private final List<Object> shapes;
    private final int solidColor;
    private final int solidHeight;
    private final int solidWidth;
    private final float startProgress;
    private final float timeStretch;
    private final AnimatableTransform transform;

    static class Factory {
        private Factory() {
        }

        static Layer newInstance(LottieComposition lottieComposition) {
            Rect bounds = lottieComposition.getBounds();
            return new Layer(Collections.emptyList(), lottieComposition, null, -1L, Layer.LayerType.PreComp, -1L, null, Collections.emptyList(), AnimatableTransform.Factory.newInstance(), 0, 0, 0, 0.0f, 0.0f, bounds.width(), bounds.height(), Collections.emptyList(), Layer.MatteType.None, null);
        }

        static Layer newInstance(JSONObject jSONObject, LottieComposition lottieComposition) {
            String strOptString = jSONObject.optString("nm");
            String strOptString2 = jSONObject.optString("refId");
            long jOptLong = jSONObject.optLong("ind");
            int iOptInt = 0;
            int iOptInt2 = 0;
            int color = 0;
            int iOptInt3 = 0;
            int iOptInt4 = 0;
            int iOptInt5 = jSONObject.optInt("ty", -1);
            Layer.LayerType layerType = iOptInt5 < Layer.LayerType.Unknown.ordinal() ? Layer.LayerType.valuesCustom()[iOptInt5] : Layer.LayerType.Unknown;
            long jOptLong2 = jSONObject.optLong("parent", -1L);
            if (layerType == Layer.LayerType.Solid) {
                iOptInt = (int) (jSONObject.optInt("sw") * lottieComposition.getDpScale());
                iOptInt2 = (int) (jSONObject.optInt("sh") * lottieComposition.getDpScale());
                color = Color.parseColor(jSONObject.optString("sc"));
            }
            AnimatableTransform animatableTransformNewInstance = AnimatableTransform.Factory.newInstance(jSONObject.optJSONObject("ks"), lottieComposition);
            Layer.MatteType matteType = Layer.MatteType.valuesCustom()[jSONObject.optInt("tt")];
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList2 = new ArrayList();
            ArrayList arrayList3 = new ArrayList();
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("masksProperties");
            if (jSONArrayOptJSONArray != null) {
                for (int i = 0; i < jSONArrayOptJSONArray.length(); i++) {
                    arrayList2.add(Mask.Factory.newMask(jSONArrayOptJSONArray.optJSONObject(i), lottieComposition));
                }
            }
            JSONArray jSONArrayOptJSONArray2 = jSONObject.optJSONArray("shapes");
            if (jSONArrayOptJSONArray2 != null) {
                for (int i2 = 0; i2 < jSONArrayOptJSONArray2.length(); i2++) {
                    Object objShapeItemWithJson = ShapeGroup.shapeItemWithJson(jSONArrayOptJSONArray2.optJSONObject(i2), lottieComposition);
                    if (objShapeItemWithJson != null) {
                        arrayList.add(objShapeItemWithJson);
                    }
                }
            }
            float fOptDouble = (float) jSONObject.optDouble("sr", 1.0d);
            float fOptDouble2 = ((float) jSONObject.optDouble("st")) / lottieComposition.getDurationFrames();
            if (layerType == Layer.LayerType.PreComp) {
                iOptInt3 = (int) (jSONObject.optInt("w") * lottieComposition.getDpScale());
                iOptInt4 = (int) (jSONObject.optInt("h") * lottieComposition.getDpScale());
            }
            float fOptLong = jSONObject.optLong("ip");
            float fOptLong2 = jSONObject.optLong("op");
            if (fOptLong > 0.0f) {
                arrayList3.add(new Keyframe(lottieComposition, Float.valueOf(0.0f), Float.valueOf(0.0f), null, 0.0f, Float.valueOf(fOptLong)));
            }
            if (fOptLong2 <= 0.0f) {
                fOptLong2 = lottieComposition.getEndFrame() + 1;
            }
            arrayList3.add(new Keyframe(lottieComposition, Float.valueOf(1.0f), Float.valueOf(1.0f), null, fOptLong, Float.valueOf(fOptLong2)));
            if (fOptLong2 <= lottieComposition.getDurationFrames()) {
                arrayList3.add(new Keyframe(lottieComposition, Float.valueOf(0.0f), Float.valueOf(0.0f), null, fOptLong2, Float.valueOf(lottieComposition.getEndFrame())));
            }
            return new Layer(arrayList, lottieComposition, strOptString, jOptLong, layerType, jOptLong2, strOptString2, arrayList2, animatableTransformNewInstance, iOptInt, iOptInt2, color, fOptDouble, fOptDouble2, iOptInt3, iOptInt4, arrayList3, matteType, null);
        }
    }

    enum LayerType {
        PreComp,
        Solid,
        Image,
        Null,
        Shape,
        Text,
        Unknown;

        public static Layer.LayerType[] valuesCustom() {
            return values();
        }
    }

    enum MatteType {
        None,
        Add,
        Invert,
        Unknown;

        public static Layer.MatteType[] valuesCustom() {
            return values();
        }
    }

    private Layer(List<Object> list, LottieComposition lottieComposition, String str, long j, Layer.LayerType layerType, long j2, @Nullable String str2, List<Mask> list2, AnimatableTransform animatableTransform, int i, int i2, int i3, float f, float f2, int i4, int i5, List<Keyframe<Float>> list3, Layer.MatteType matteType) {
        this.shapes = list;
        this.composition = lottieComposition;
        this.layerName = str;
        this.layerId = j;
        this.layerType = layerType;
        this.parentId = j2;
        this.refId = str2;
        this.masks = list2;
        this.transform = animatableTransform;
        this.solidWidth = i;
        this.solidHeight = i2;
        this.solidColor = i3;
        this.timeStretch = f;
        this.startProgress = f2;
        this.preCompWidth = i4;
        this.preCompHeight = i5;
        this.inOutKeyframes = list3;
        this.matteType = matteType;
    }

     Layer(List list, LottieComposition lottieComposition, String str, long j, Layer.LayerType layerType, long j2, String str2, List list2, AnimatableTransform animatableTransform, int i, int i2, int i3, float f, float f2, int i4, int i5, List list3, Layer.MatteType matteType, Layer layer) {
        this(list, lottieComposition, str, j, layerType, j2, str2, list2, animatableTransform, i, i2, i3, f, f2, i4, i5, list3, matteType);
    }

    LottieComposition getComposition() {
        return this.composition;
    }

    long getId() {
        return this.layerId;
    }

    List<Keyframe<Float>> getInOutKeyframes() {
        return this.inOutKeyframes;
    }

    Layer.LayerType getLayerType() {
        return this.layerType;
    }

    List<Mask> getMasks() {
        return this.masks;
    }

    Layer.MatteType getMatteType() {
        return this.matteType;
    }

    String getName() {
        return this.layerName;
    }

    long getParentId() {
        return this.parentId;
    }

    int getPreCompHeight() {
        return this.preCompHeight;
    }

    int getPreCompWidth() {
        return this.preCompWidth;
    }

    @Nullable
    String getRefId() {
        return this.refId;
    }

    List<Object> getShapes() {
        return this.shapes;
    }

    int getSolidColor() {
        return this.solidColor;
    }

    int getSolidHeight() {
        return this.solidHeight;
    }

    int getSolidWidth() {
        return this.solidWidth;
    }

    float getStartProgress() {
        return this.startProgress;
    }

    float getTimeStretch() {
        return this.timeStretch;
    }

    AnimatableTransform getTransform() {
        return this.transform;
    }

    public String toString() {
        return toString("");
    }

    String toString(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append(str).append(getName()).append("\n");
        Layer layerLayerModelForId = this.composition.layerModelForId(getParentId());
        if (layerLayerModelForId != null) {
            sb.append("\t\tParents: ").append(layerLayerModelForId.getName());
            Layer layerLayerModelForId2 = this.composition.layerModelForId(layerLayerModelForId.getParentId());
            while (layerLayerModelForId2 != null) {
                sb.append("->").append(layerLayerModelForId2.getName());
                layerLayerModelForId2 = this.composition.layerModelForId(layerLayerModelForId2.getParentId());
            }
            sb.append(str).append("\n");
        }
        if (!getMasks().isEmpty()) {
            sb.append(str).append("\tMasks: ").append(getMasks().size()).append("\n");
        }
        if (getSolidWidth() != 0 && getSolidHeight() != 0) {
            sb.append(str).append("\tBackground: ").append(String.format(Locale.US, "%dx%d %X\n", Integer.valueOf(getSolidWidth()), Integer.valueOf(getSolidHeight()), Integer.valueOf(getSolidColor())));
        }
        if (!this.shapes.isEmpty()) {
            sb.append(str).append("\tShapes:\n");
            Iterator<Object> it = this.shapes.iterator();
            while (it.hasNext()) {
                sb.append(str).append("\t\t").append(it.next()).append("\n");
            }
        }
        return sb.toString();
    }
}
