package com.airbnb.lottie;

import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.os.Build;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.HashSet;
import java.util.Set;

public class LottieDrawable extends Drawable implements Callback {
    private static final String TAG = LottieDrawable.class.getSimpleName();
    private LottieComposition composition;

    @Nullable
    private CompositionLayer compositionLayer;
    private boolean enableMergePaths;

    @Nullable
    private ImageAssetBitmapManager imageAssetBitmapManager;

    @Nullable
    private ImageAssetDelegate imageAssetDelegate;

    @Nullable
    private String imageAssetsFolder;
    private boolean playAnimationWhenCompositionAdded;
    private boolean reverseAnimationWhenCompositionAdded;
    private boolean systemAnimationsAreDisabled;
    private final Matrix matrix = new Matrix();
    private final ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
    private float speed = 1.0f;
    private float progress = 0.0f;
    private float scale = 1.0f;
    private final Set<LottieDrawable.ColorFilterData> colorFilterData = new HashSet();
    private int alpha = 255;

    private static class ColorFilterData {

        @Nullable
        final ColorFilter colorFilter;

        @Nullable
        final String contentName;
        final String layerName;

        ColorFilterData(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
            this.layerName = str;
            this.contentName = str2;
            this.colorFilter = colorFilter;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LottieDrawable.ColorFilterData)) {
                return false;
            }
            LottieDrawable.ColorFilterData colorFilterData = (LottieDrawable.ColorFilterData) obj;
            return hashCode() == colorFilterData.hashCode() && this.colorFilter == colorFilterData.colorFilter;
        }

        public int hashCode() {
            int iHashCode = this.layerName != null ? this.layerName.hashCode() * 527 : 17;
            return this.contentName != null ? iHashCode * 31 * this.contentName.hashCode() : iHashCode;
        }
    }

    public LottieDrawable() {
        this.animator.setRepeatCount(0);
        this.animator.setInterpolator(new LinearInterpolator());
        this.animator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (!LottieDrawable.this.systemAnimationsAreDisabled) {
                    LottieDrawable.this.setProgress(((Float) valueAnimator.getAnimatedValue()).floatValue());
                } else {
                    LottieDrawable.this.animator.cancel();
                    LottieDrawable.this.setProgress(1.0f);
                }
            }
        });
    }

    private void addColorFilterInternal(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
        LottieDrawable.ColorFilterData colorFilterData = new LottieDrawable.ColorFilterData(str, str2, colorFilter);
        if (colorFilter == null && this.colorFilterData.contains(colorFilterData)) {
            this.colorFilterData.remove(colorFilterData);
        } else {
            this.colorFilterData.add(new LottieDrawable.ColorFilterData(str, str2, colorFilter));
        }
        if (this.compositionLayer == null) {
            return;
        }
        this.compositionLayer.addColorFilter(str, str2, colorFilter);
    }

    private void applyColorFilters() {
        if (this.compositionLayer == null) {
            return;
        }
        for (LottieDrawable.ColorFilterData colorFilterData : this.colorFilterData) {
            this.compositionLayer.addColorFilter(colorFilterData.layerName, colorFilterData.contentName, colorFilterData.colorFilter);
        }
    }

    private void buildCompositionLayer() {
        this.compositionLayer = new CompositionLayer(this, Layer.Factory.newInstance(this.composition), this.composition.getLayers(), this.composition);
    }

    private void clearComposition() {
        recycleBitmaps();
        this.compositionLayer = null;
        this.imageAssetBitmapManager = null;
        invalidateSelf();
    }

    @Nullable
    private Context getContext() {
        Callback callback = getCallback();
        if (callback != null && (callback instanceof View)) {
            return ((View) callback).getContext();
        }
        return null;
    }

    private ImageAssetBitmapManager getImageAssetBitmapManager() {
        if (getCallback() == null) {
            return null;
        }
        if (this.imageAssetBitmapManager != null && (!this.imageAssetBitmapManager.hasSameContext(getContext()))) {
            this.imageAssetBitmapManager.recycleBitmaps();
            this.imageAssetBitmapManager = null;
        }
        if (this.imageAssetBitmapManager == null) {
            this.imageAssetBitmapManager = new ImageAssetBitmapManager(getCallback(), this.imageAssetsFolder, this.imageAssetDelegate, this.composition.getImages());
        }
        return this.imageAssetBitmapManager;
    }

    private float getMaxScale(@NonNull Canvas canvas) {
        return Math.min(canvas.getWidth() / this.composition.getBounds().width(), canvas.getHeight() / this.composition.getBounds().height());
    }

    private void playAnimation(boolean z) {
        if (this.compositionLayer == null) {
            this.playAnimationWhenCompositionAdded = true;
            this.reverseAnimationWhenCompositionAdded = false;
            return;
        }
        long duration = z ? (long) (this.progress * this.animator.getDuration()) : 0L;
        this.animator.start();
        if (z) {
            this.animator.setCurrentPlayTime(duration);
        }
    }

    private void reverseAnimation(boolean z) {
        if (this.compositionLayer == null) {
            this.playAnimationWhenCompositionAdded = false;
            this.reverseAnimationWhenCompositionAdded = true;
        } else {
            if (z) {
                this.animator.setCurrentPlayTime((long) (this.progress * this.animator.getDuration()));
            }
            this.animator.reverse();
        }
    }

    private void updateBounds() {
        if (this.composition == null) {
            return;
        }
        setBounds(0, 0, (int) (this.composition.getBounds().width() * this.scale), (int) (this.composition.getBounds().height() * this.scale));
    }

    public void addAnimatorListener(AnimatorListener animatorListener) {
        this.animator.addListener(animatorListener);
    }

    public void addAnimatorUpdateListener(AnimatorUpdateListener animatorUpdateListener) {
        this.animator.addUpdateListener(animatorUpdateListener);
    }

    public void addColorFilter(ColorFilter colorFilter) {
        addColorFilterInternal(null, null, colorFilter);
    }

    public void addColorFilterToContent(String str, String str2, @Nullable ColorFilter colorFilter) {
        addColorFilterInternal(str, str2, colorFilter);
    }

    public void addColorFilterToLayer(String str, @Nullable ColorFilter colorFilter) {
        addColorFilterInternal(str, null, colorFilter);
    }

    public void cancelAnimation() {
        this.playAnimationWhenCompositionAdded = false;
        this.reverseAnimationWhenCompositionAdded = false;
        this.animator.cancel();
    }

    public void clearColorFilters() {
        this.colorFilterData.clear();
        addColorFilterInternal(null, null, null);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (this.compositionLayer == null) {
            return;
        }
        float fMin = this.scale;
        if (this.compositionLayer.hasMatte()) {
            fMin = Math.min(this.scale, getMaxScale(canvas));
        }
        this.matrix.reset();
        this.matrix.preScale(fMin, fMin);
        this.compositionLayer.draw(canvas, this.matrix, this.alpha);
    }

    public void enableMergePathsForKitKatAndAbove(boolean z) {
        if (Build.VERSION.SDK_INT < 19) {
            Log.w(TAG, "Merge paths are not supported pre-Kit Kat.");
            return;
        }
        this.enableMergePaths = z;
        if (this.composition != null) {
            buildCompositionLayer();
        }
    }

    boolean enableMergePathsForKitKatAndAbove() {
        return this.enableMergePaths;
    }

    @Override
    public int getAlpha() {
        return this.alpha;
    }

    public LottieComposition getComposition() {
        return this.composition;
    }

    @Nullable
    Bitmap getImageAsset(String str) {
        ImageAssetBitmapManager imageAssetBitmapManager = getImageAssetBitmapManager();
        if (imageAssetBitmapManager != null) {
            return imageAssetBitmapManager.bitmapForId(str);
        }
        return null;
    }

    @Nullable
    public String getImageAssetsFolder() {
        return this.imageAssetsFolder;
    }

    @Override
    public int getIntrinsicHeight() {
        if (this.composition == null) {
            return -1;
        }
        return (int) (this.composition.getBounds().height() * this.scale);
    }

    @Override
    public int getIntrinsicWidth() {
        if (this.composition == null) {
            return -1;
        }
        return (int) (this.composition.getBounds().width() * this.scale);
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    public float getProgress() {
        return this.progress;
    }

    public float getScale() {
        return this.scale;
    }

    public boolean hasMasks() {
        if (this.compositionLayer != null) {
            return this.compositionLayer.hasMasks();
        }
        return false;
    }

    public boolean hasMatte() {
        if (this.compositionLayer != null) {
            return this.compositionLayer.hasMatte();
        }
        return false;
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.invalidateDrawable(this);
    }

    @Override
    public void invalidateSelf() {
        Callback callback = getCallback();
        if (callback != null) {
            callback.invalidateDrawable(this);
        }
    }

    public boolean isAnimating() {
        return this.animator.isRunning();
    }

    public boolean isLooping() {
        return this.animator.getRepeatCount() == -1;
    }

    public void loop(boolean z) {
        this.animator.setRepeatCount(z ? -1 : 0);
    }

    public void playAnimation() {
        boolean z = false;
        if (this.progress > 0.0d && this.progress < 1.0d) {
            z = true;
        }
        playAnimation(z);
    }

    public void recycleBitmaps() {
        if (this.imageAssetBitmapManager != null) {
            this.imageAssetBitmapManager.recycleBitmaps();
        }
    }

    public void removeAnimatorListener(AnimatorListener animatorListener) {
        this.animator.removeListener(animatorListener);
    }

    public void removeAnimatorUpdateListener(AnimatorUpdateListener animatorUpdateListener) {
        this.animator.removeUpdateListener(animatorUpdateListener);
    }

    public void resumeAnimation() {
        playAnimation(true);
    }

    public void resumeReverseAnimation() {
        reverseAnimation(true);
    }

    public void reverseAnimation() {
        boolean z = false;
        if (this.progress > 0.0d && this.progress < 1.0d) {
            z = true;
        }
        reverseAnimation(z);
    }

    @Override
    public void scheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable, long j) {
        Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.scheduleDrawable(this, runnable, j);
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int i) {
        this.alpha = i;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    public boolean setComposition(LottieComposition lottieComposition) {
        if (this.composition == lottieComposition) {
            return false;
        }
        clearComposition();
        this.composition = lottieComposition;
        setSpeed(this.speed);
        updateBounds();
        buildCompositionLayer();
        applyColorFilters();
        setProgress(this.progress);
        if (this.playAnimationWhenCompositionAdded) {
            this.playAnimationWhenCompositionAdded = false;
            playAnimation();
        }
        if (!this.reverseAnimationWhenCompositionAdded) {
            return true;
        }
        this.reverseAnimationWhenCompositionAdded = false;
        reverseAnimation();
        return true;
    }

    public void setImageAssetDelegate(ImageAssetDelegate imageAssetDelegate) {
        this.imageAssetDelegate = imageAssetDelegate;
        if (this.imageAssetBitmapManager != null) {
            this.imageAssetBitmapManager.setAssetDelegate(imageAssetDelegate);
        }
    }

    public void setImagesAssetsFolder(@Nullable String str) {
        this.imageAssetsFolder = str;
    }

    public void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float f) {
        this.progress = f;
        if (this.compositionLayer != null) {
            this.compositionLayer.setProgress(f);
        }
    }

    public void setScale(float f) {
        this.scale = f;
        updateBounds();
    }

    public void setSpeed(float f) {
        this.speed = f;
        if (f < 0.0f) {
            this.animator.setFloatValues(1.0f, 0.0f);
        } else {
            this.animator.setFloatValues(0.0f, 1.0f);
        }
        if (this.composition != null) {
            this.animator.setDuration((long) (this.composition.getDuration() / Math.abs(f)));
        }
    }

    void systemAnimationsAreDisabled() {
        this.systemAnimationsAreDisabled = true;
    }

    @Override
    public void unscheduleDrawable(@NonNull Drawable drawable, @NonNull Runnable runnable) {
        Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.unscheduleDrawable(this, runnable);
    }

    @Nullable
    public Bitmap updateBitmap(String str, @Nullable Bitmap bitmap) {
        ImageAssetBitmapManager imageAssetBitmapManager = getImageAssetBitmapManager();
        if (imageAssetBitmapManager == null) {
            Log.w("LOTTIE", "Cannot update bitmap. Most likely the drawable is not added to a View which prevents Lottie from getting a Context.");
            return null;
        }
        Bitmap bitmapUpdateBitmap = imageAssetBitmapManager.updateBitmap(str, bitmap);
        invalidateSelf();
        return bitmapUpdateBitmap;
    }
}
