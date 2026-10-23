package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import java.util.ArrayList;
import java.util.List;

class CompositionLayer extends BaseLayer {

    private static int[] f14comairbnblottieLayer$MatteTypeSwitchesValues = null;

    @Nullable
    private Boolean hasMasks;

    @Nullable
    private Boolean hasMatte;
    private final List<BaseLayer> layers;
    private final RectF newClipRect;
    private final Rect originalClipRect;
    private final RectF rect;

    private static int[] m273getcomairbnblottieLayer$MatteTypeSwitchesValues() {
        if (f14comairbnblottieLayer$MatteTypeSwitchesValues != null) {
            return f14comairbnblottieLayer$MatteTypeSwitchesValues;
        }
        int[] iArr = new int[Layer.MatteType.valuesCustom().length];
        try {
            iArr[Layer.MatteType.Add.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Layer.MatteType.Invert.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Layer.MatteType.None.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Layer.MatteType.Unknown.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f14comairbnblottieLayer$MatteTypeSwitchesValues = iArr;
        return iArr;
    }

    CompositionLayer(LottieDrawable lottieDrawable, Layer layer, List<Layer> list, LottieComposition lottieComposition) {
        super(lottieDrawable, layer);
        BaseLayer baseLayer = null;
        this.layers = new ArrayList();
        this.rect = new RectF();
        this.originalClipRect = new Rect();
        this.newClipRect = new RectF();
        LongSparseArray longSparseArray = new LongSparseArray(lottieComposition.getLayers().size());
        int size = list.size() - 1;
        BaseLayer baseLayer2 = null;
        while (size >= 0) {
            Layer layer2 = list.get(size);
            BaseLayer baseLayerForModel = BaseLayer.forModel(layer2, lottieDrawable, lottieComposition);
            if (baseLayerForModel != null) {
                longSparseArray.put(baseLayerForModel.getLayerModel().getId(), baseLayerForModel);
                if (baseLayer2 == null) {
                    this.layers.add(0, baseLayerForModel);
                    switch (m273getcomairbnblottieLayer$MatteTypeSwitchesValues()[layer2.getMatteType().ordinal()]) {
                        case 1:
                        case 2:
                            baseLayer = baseLayerForModel;
                            break;
                        default:
                            baseLayer = baseLayer2;
                            break;
                    }
                } else {
                    baseLayer2.setMatteLayer(baseLayerForModel);
                    baseLayer = null;
                }
            } else {
                baseLayer = baseLayer2;
            }
            size--;
            baseLayer2 = baseLayer;
        }
        for (int i = 0; i < longSparseArray.size(); i++) {
            BaseLayer baseLayer3 = (BaseLayer) longSparseArray.get(longSparseArray.keyAt(i));
            BaseLayer baseLayer4 = (BaseLayer) longSparseArray.get(baseLayer3.getLayerModel().getParentId());
            if (baseLayer4 != null) {
                baseLayer3.setParentLayer(baseLayer4);
            }
        }
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.layers.size()) {
                return;
            }
            BaseLayer baseLayer = this.layers.get(i2);
            String name = baseLayer.getLayerModel().getName();
            if (str == null) {
                baseLayer.addColorFilter(null, null, colorFilter);
            } else if (name.equals(str)) {
                baseLayer.addColorFilter(str, str2, colorFilter);
            }
            i = i2 + 1;
        }
    }

    @Override
    void drawLayer(Canvas canvas, Matrix matrix, int i) {
        canvas.getClipBounds(this.originalClipRect);
        this.newClipRect.set(0.0f, 0.0f, this.layerModel.getPreCompWidth(), this.layerModel.getPreCompHeight());
        matrix.mapRect(this.newClipRect);
        for (int size = this.layers.size() - 1; size >= 0; size--) {
            if (this.newClipRect.isEmpty() ? true : canvas.clipRect(this.newClipRect)) {
                this.layers.get(size).draw(canvas, matrix, i);
            }
        }
        if (this.originalClipRect.isEmpty()) {
            return;
        }
        canvas.clipRect(this.originalClipRect, Op.REPLACE);
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        super.getBounds(rectF, matrix);
        this.rect.set(0.0f, 0.0f, 0.0f, 0.0f);
        for (int size = this.layers.size() - 1; size >= 0; size--) {
            this.layers.get(size).getBounds(this.rect, this.boundsMatrix);
            if (rectF.isEmpty()) {
                rectF.set(this.rect);
            } else {
                rectF.set(Math.min(rectF.left, this.rect.left), Math.min(rectF.top, this.rect.top), Math.max(rectF.right, this.rect.right), Math.max(rectF.bottom, this.rect.bottom));
            }
        }
    }

    boolean hasMasks() {
        if (this.hasMasks == null) {
            for (int size = this.layers.size() - 1; size >= 0; size--) {
                BaseLayer baseLayer = this.layers.get(size);
                if ((baseLayer instanceof ShapeLayer) && baseLayer.hasMasksOnThisLayer()) {
                    this.hasMasks = true;
                    return true;
                }
            }
            this.hasMasks = false;
        }
        return this.hasMasks.booleanValue();
    }

    boolean hasMatte() {
        if (this.hasMatte == null) {
            if (hasMatteOnThisLayer()) {
                this.hasMatte = true;
                return true;
            }
            for (int size = this.layers.size() - 1; size >= 0; size--) {
                if (this.layers.get(size).hasMatteOnThisLayer()) {
                    this.hasMatte = true;
                    return true;
                }
            }
            this.hasMatte = false;
        }
        return this.hasMatte.booleanValue();
    }

    @Override
    public void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float f) {
        super.setProgress(f);
        float startProgress = f - this.layerModel.getStartProgress();
        for (int size = this.layers.size() - 1; size >= 0; size--) {
            this.layers.get(size).setProgress(startProgress);
        }
    }
}
