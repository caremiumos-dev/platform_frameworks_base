package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.CallSuper;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

abstract class BaseLayer implements DrawingContent, BaseKeyframeAnimation.AnimationListener {

    private static int[] f12comairbnblottieLayer$LayerTypeSwitchesValues = null;

    private static int[] f13comairbnblottieMask$MaskModeSwitchesValues = null;
    private static final int SAVE_FLAGS = 19;
    final Layer layerModel;
    final LottieDrawable lottieDrawable;

    @Nullable
    private MaskKeyframeAnimation mask;

    @Nullable
    private BaseLayer matteLayer;

    @Nullable
    private BaseLayer parentLayer;
    private List<BaseLayer> parentLayers;
    final TransformKeyframeAnimation transform;
    private final Path path = new Path();
    private final Matrix matrix = new Matrix();
    private final Paint contentPaint = new Paint(1);
    private final Paint maskPaint = new Paint(1);
    private final Paint mattePaint = new Paint(1);
    private final Paint clearPaint = new Paint();
    private final RectF rect = new RectF();
    private final RectF maskBoundsRect = new RectF();
    private final RectF matteBoundsRect = new RectF();
    private final RectF tempMaskBoundsRect = new RectF();
    final Matrix boundsMatrix = new Matrix();
    private final List<BaseKeyframeAnimation<?, ?>> animations = new ArrayList();
    private boolean visible = true;

    private static int[] m268getcomairbnblottieLayer$LayerTypeSwitchesValues() {
        if (f12comairbnblottieLayer$LayerTypeSwitchesValues != null) {
            return f12comairbnblottieLayer$LayerTypeSwitchesValues;
        }
        int[] iArr = new int[Layer.LayerType.valuesCustom().length];
        try {
            iArr[Layer.LayerType.Image.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Layer.LayerType.Null.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Layer.LayerType.PreComp.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Layer.LayerType.Shape.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Layer.LayerType.Solid.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Layer.LayerType.Text.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[Layer.LayerType.Unknown.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        f12comairbnblottieLayer$LayerTypeSwitchesValues = iArr;
        return iArr;
    }

    private static int[] m269getcomairbnblottieMask$MaskModeSwitchesValues() {
        if (f13comairbnblottieMask$MaskModeSwitchesValues != null) {
            return f13comairbnblottieMask$MaskModeSwitchesValues;
        }
        int[] iArr = new int[Mask.MaskMode.valuesCustom().length];
        try {
            iArr[Mask.MaskMode.MaskModeAdd.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Mask.MaskMode.MaskModeIntersect.ordinal()] = 10;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Mask.MaskMode.MaskModeSubtract.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Mask.MaskMode.MaskModeUnknown.ordinal()] = 11;
        } catch (NoSuchFieldError e4) {
        }
        f13comairbnblottieMask$MaskModeSwitchesValues = iArr;
        return iArr;
    }

    BaseLayer(LottieDrawable lottieDrawable, Layer layer) {
        this.lottieDrawable = lottieDrawable;
        this.layerModel = layer;
        this.clearPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        this.maskPaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        if (layer.getMatteType() == Layer.MatteType.Invert) {
            this.mattePaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
        } else {
            this.mattePaint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        }
        this.transform = layer.getTransform().createAnimation();
        this.transform.addListener(this);
        this.transform.addAnimationsToLayer(this);
        if (layer.getMasks() != null && (!layer.getMasks().isEmpty())) {
            this.mask = new MaskKeyframeAnimation(layer.getMasks());
            Iterator<BaseKeyframeAnimation<?, Path>> it = this.mask.getMaskAnimations().iterator();
            while (it.hasNext()) {
                BaseKeyframeAnimation<?, ?> baseKeyframeAnimation = (BaseKeyframeAnimation) it.next();
                addAnimation(baseKeyframeAnimation);
                baseKeyframeAnimation.addUpdateListener(this);
            }
        }
        setupInOutAnimations();
    }

    private void applyMasks(Canvas canvas, Matrix matrix) {
        canvas.saveLayer(this.rect, this.maskPaint, 19);
        clearCanvas(canvas);
        int size = this.mask.getMasks().size();
        for (int i = 0; i < size; i++) {
            Mask mask = this.mask.getMasks().get(i);
            this.path.set(this.mask.getMaskAnimations().get(i).getValue());
            this.path.transform(matrix);
            switch (m269getcomairbnblottieMask$MaskModeSwitchesValues()[mask.getMaskMode().ordinal()]) {
                case 2:
                    this.path.setFillType(FillType.INVERSE_WINDING);
                    break;
                default:
                    this.path.setFillType(FillType.WINDING);
                    break;
            }
            canvas.drawPath(this.path, this.contentPaint);
        }
        canvas.restore();
    }

    private void buildParentLayerListIfNeeded() {
        if (this.parentLayers != null) {
            return;
        }
        if (this.parentLayer == null) {
            this.parentLayers = Collections.emptyList();
            return;
        }
        this.parentLayers = new ArrayList();
        for (BaseLayer baseLayer = this.parentLayer; baseLayer != null; baseLayer = baseLayer.parentLayer) {
            this.parentLayers.add(baseLayer);
        }
    }

    private void clearCanvas(Canvas canvas) {
        canvas.drawRect(this.rect.left - 1.0f, this.rect.top - 1.0f, this.rect.right + 1.0f, 1.0f + this.rect.bottom, this.clearPaint);
    }

    @Nullable
    static BaseLayer forModel(Layer layer, LottieDrawable lottieDrawable, LottieComposition lottieComposition) {
        switch (m268getcomairbnblottieLayer$LayerTypeSwitchesValues()[layer.getLayerType().ordinal()]) {
            case 1:
                return new ImageLayer(lottieDrawable, layer, lottieComposition.getDpScale());
            case 2:
                return new NullLayer(lottieDrawable, layer);
            case 3:
                return new CompositionLayer(lottieDrawable, layer, lottieComposition.getPrecomps(layer.getRefId()), lottieComposition);
            case 4:
                return new ShapeLayer(lottieDrawable, layer);
            case 5:
                return new SolidLayer(lottieDrawable, layer);
            default:
                Log.w("LOTTIE", "Unknown layer type " + layer.getLayerType());
                return null;
        }
    }

    private void intersectBoundsWithMask(RectF r10, Matrix r11) {

        throw new UnsupportedOperationException("Method not decompiled: com.airbnb.lottie.BaseLayer.intersectBoundsWithMask(android.graphics.RectF, android.graphics.Matrix):void");
    }

    private void intersectBoundsWithMatte(RectF rectF, Matrix matrix) {
        if (hasMatteOnThisLayer() && this.layerModel.getMatteType() != Layer.MatteType.Invert) {
            this.matteLayer.getBounds(this.matteBoundsRect, matrix);
            rectF.set(Math.max(rectF.left, this.matteBoundsRect.left), Math.max(rectF.top, this.matteBoundsRect.top), Math.min(rectF.right, this.matteBoundsRect.right), Math.min(rectF.bottom, this.matteBoundsRect.bottom));
        }
    }

    private void invalidateSelf() {
        this.lottieDrawable.invalidateSelf();
    }

    public void setVisible(boolean z) {
        if (z != this.visible) {
            this.visible = z;
            invalidateSelf();
        }
    }

    private void setupInOutAnimations() {
        if (this.layerModel.getInOutKeyframes().isEmpty()) {
            setVisible(true);
            return;
        }
        final FloatKeyframeAnimation floatKeyframeAnimation = new FloatKeyframeAnimation(this.layerModel.getInOutKeyframes());
        floatKeyframeAnimation.setIsDiscrete();
        floatKeyframeAnimation.addUpdateListener(new BaseKeyframeAnimation.AnimationListener() {
            @Override
            public void onValueChanged() {
                BaseLayer.this.setVisible(((Float) floatKeyframeAnimation.getValue()).floatValue() == 1.0f);
            }
        });
        setVisible(((Float) floatKeyframeAnimation.getValue()).floatValue() == 1.0f);
        addAnimation(floatKeyframeAnimation);
    }

    void addAnimation(BaseKeyframeAnimation<?, ?> baseKeyframeAnimation) {
        if (baseKeyframeAnimation instanceof StaticKeyframeAnimation) {
            return;
        }
        this.animations.add(baseKeyframeAnimation);
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix, int i) {
        if (this.visible) {
            buildParentLayerListIfNeeded();
            this.matrix.reset();
            this.matrix.set(matrix);
            for (int size = this.parentLayers.size() - 1; size >= 0; size--) {
                this.matrix.preConcat(this.parentLayers.get(size).transform.getMatrix());
            }
            int iIntValue = (int) (((this.transform.getOpacity().getValue().intValue() * (i / 255.0f)) / 100.0f) * 255.0f);
            if (!hasMatteOnThisLayer() && (!hasMasksOnThisLayer())) {
                this.matrix.preConcat(this.transform.getMatrix());
                drawLayer(canvas, this.matrix, iIntValue);
                return;
            }
            this.rect.set(0.0f, 0.0f, 0.0f, 0.0f);
            getBounds(this.rect, this.matrix);
            intersectBoundsWithMatte(this.rect, this.matrix);
            this.matrix.preConcat(this.transform.getMatrix());
            intersectBoundsWithMask(this.rect, this.matrix);
            this.rect.set(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight());
            canvas.saveLayer(this.rect, this.contentPaint, 31);
            clearCanvas(canvas);
            drawLayer(canvas, this.matrix, iIntValue);
            if (hasMasksOnThisLayer()) {
                applyMasks(canvas, this.matrix);
            }
            if (hasMatteOnThisLayer()) {
                canvas.saveLayer(this.rect, this.mattePaint, 19);
                clearCanvas(canvas);
                this.matteLayer.draw(canvas, matrix, iIntValue);
                canvas.restore();
            }
            canvas.restore();
        }
    }

    abstract void drawLayer(Canvas canvas, Matrix matrix, int i);

    @Override
    @CallSuper
    public void getBounds(RectF rectF, Matrix matrix) {
        this.boundsMatrix.set(matrix);
        this.boundsMatrix.preConcat(this.transform.getMatrix());
    }

    Layer getLayerModel() {
        return this.layerModel;
    }

    @Override
    public String getName() {
        return this.layerModel.getName();
    }

    boolean hasMasksOnThisLayer() {
        if (this.mask != null) {
            return !this.mask.getMaskAnimations().isEmpty();
        }
        return false;
    }

    boolean hasMatteOnThisLayer() {
        return this.matteLayer != null;
    }

    @Override
    public void onValueChanged() {
        invalidateSelf();
    }

    @Override
    public void setContents(List<Content> list, List<Content> list2) {
    }

    void setMatteLayer(@Nullable BaseLayer baseLayer) {
        this.matteLayer = baseLayer;
    }

    void setParentLayer(@Nullable BaseLayer baseLayer) {
        this.parentLayer = baseLayer;
    }

    void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float f) {
        if (this.matteLayer != null) {
            this.matteLayer.setProgress(f);
        }
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.animations.size()) {
                return;
            }
            this.animations.get(i2).setProgress(f);
            i = i2 + 1;
        }
    }
}
