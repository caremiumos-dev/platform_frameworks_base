package com.airbnb.lottie;

import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.provider.Settings.Global;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View.BaseSavedState;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class LottieAnimationView extends ImageView {
    private static final String TAG = LottieAnimationView.class.getSimpleName();
    private static final Map<String, LottieComposition> strongRefCache = new HashMap();
    private static final Map<String, WeakReference<LottieComposition>> weakRefCache = new HashMap();
    private String animationName;
    private boolean autoPlay;

    @Nullable
    private LottieComposition composition;

    @Nullable
    private Cancellable compositionLoader;
    private LottieAnimationView.CacheStrategy defaultCacheStrategy;
    private final OnCompositionLoadedListener loadedListener;
    private final LottieDrawable lottieDrawable;
    private boolean useHardwareLayer;
    private boolean wasAnimatingWhenDetached;

    public enum CacheStrategy {
        None,
        Weak,
        Strong;

        public static LottieAnimationView.CacheStrategy[] valuesCustom() {
            return values();
        }
    }

    private static class SavedState extends BaseSavedState {
        public static final Creator<LottieAnimationView.SavedState> CREATOR = new Creator<LottieAnimationView.SavedState>() {

            @Override
            public LottieAnimationView.SavedState createFromParcel(Parcel parcel) {
                return new LottieAnimationView.SavedState(parcel, null);
            }

            @Override
            public LottieAnimationView.SavedState[] newArray(int i) {
                return new LottieAnimationView.SavedState[i];
            }
        };
        String animationName;
        String imageAssetsFolder;
        boolean isAnimating;
        boolean isLooping;
        float progress;

        private SavedState(Parcel parcel) {
            super(parcel);
            this.animationName = parcel.readString();
            this.progress = parcel.readFloat();
            this.isAnimating = parcel.readInt() == 1;
            this.isLooping = parcel.readInt() == 1;
            this.imageAssetsFolder = parcel.readString();
        }

         SavedState(Parcel parcel, LottieAnimationView.SavedState savedState) {
            this(parcel);
        }

        SavedState(Parcelable parcelable) {
            super(parcelable);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeString(this.animationName);
            parcel.writeFloat(this.progress);
            parcel.writeInt(this.isAnimating ? 1 : 0);
            parcel.writeInt(this.isLooping ? 1 : 0);
            parcel.writeString(this.imageAssetsFolder);
        }
    }

    public LottieAnimationView(Context context) {
        super(context);
        this.loadedListener = new OnCompositionLoadedListener() {
            @Override
            public void onCompositionLoaded(@Nullable LottieComposition lottieComposition) {
                if (lottieComposition != null) {
                    LottieAnimationView.this.setComposition(lottieComposition);
                }
                LottieAnimationView.this.compositionLoader = null;
            }
        };
        this.lottieDrawable = new LottieDrawable();
        this.wasAnimatingWhenDetached = false;
        this.autoPlay = false;
        this.useHardwareLayer = false;
        init(null);
    }

    public LottieAnimationView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.loadedListener = new OnCompositionLoadedListener() {
            @Override
            public void onCompositionLoaded(@Nullable LottieComposition lottieComposition) {
                if (lottieComposition != null) {
                    LottieAnimationView.this.setComposition(lottieComposition);
                }
                LottieAnimationView.this.compositionLoader = null;
            }
        };
        this.lottieDrawable = new LottieDrawable();
        this.wasAnimatingWhenDetached = false;
        this.autoPlay = false;
        this.useHardwareLayer = false;
        init(attributeSet);
    }

    public LottieAnimationView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.loadedListener = new OnCompositionLoadedListener() {
            @Override
            public void onCompositionLoaded(@Nullable LottieComposition lottieComposition) {
                if (lottieComposition != null) {
                    LottieAnimationView.this.setComposition(lottieComposition);
                }
                LottieAnimationView.this.compositionLoader = null;
            }
        };
        this.lottieDrawable = new LottieDrawable();
        this.wasAnimatingWhenDetached = false;
        this.autoPlay = false;
        this.useHardwareLayer = false;
        init(attributeSet);
    }

    private void cancelLoaderTask() {
        if (this.compositionLoader != null) {
            this.compositionLoader.cancel();
            this.compositionLoader = null;
        }
    }

    private void enableOrDisableHardwareLayer() {
        setLayerType(this.useHardwareLayer && this.lottieDrawable.isAnimating() ? 2 : 1, null);
    }

    private void init(@Nullable AttributeSet attributeSet) {
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.LottieAnimationView);
        this.defaultCacheStrategy = LottieAnimationView.CacheStrategy.valuesCustom()[typedArrayObtainStyledAttributes.getInt(R.styleable.LottieAnimationView_lottie_cacheStrategy, LottieAnimationView.CacheStrategy.None.ordinal())];
        String string = typedArrayObtainStyledAttributes.getString(R.styleable.LottieAnimationView_lottie_fileName);
        if (!isInEditMode() && string != null) {
            setAnimation(string);
        }
        if (typedArrayObtainStyledAttributes.getBoolean(R.styleable.LottieAnimationView_lottie_autoPlay, false)) {
            this.lottieDrawable.playAnimation();
            this.autoPlay = true;
        }
        this.lottieDrawable.loop(typedArrayObtainStyledAttributes.getBoolean(R.styleable.LottieAnimationView_lottie_loop, false));
        setImageAssetsFolder(typedArrayObtainStyledAttributes.getString(R.styleable.LottieAnimationView_lottie_imageAssetsFolder));
        setProgress(typedArrayObtainStyledAttributes.getFloat(R.styleable.LottieAnimationView_lottie_progress, 0.0f));
        enableMergePathsForKitKatAndAbove(typedArrayObtainStyledAttributes.getBoolean(R.styleable.LottieAnimationView_lottie_enableMergePathsForKitKatAndAbove, false));
        if (typedArrayObtainStyledAttributes.hasValue(R.styleable.LottieAnimationView_lottie_colorFilter)) {
            addColorFilter(new SimpleColorFilter(typedArrayObtainStyledAttributes.getColor(R.styleable.LottieAnimationView_lottie_colorFilter, 0)));
        }
        if (typedArrayObtainStyledAttributes.hasValue(R.styleable.LottieAnimationView_lottie_scale)) {
            this.lottieDrawable.setScale(typedArrayObtainStyledAttributes.getFloat(R.styleable.LottieAnimationView_lottie_scale, 1.0f));
        }
        typedArrayObtainStyledAttributes.recycle();
        if (Build.VERSION.SDK_INT >= 17 && Global.getFloat(getContext().getContentResolver(), "animator_duration_scale", 1.0f) == 0.0f) {
            this.lottieDrawable.systemAnimationsAreDisabled();
        }
        enableOrDisableHardwareLayer();
    }

    public void addAnimatorListener(AnimatorListener animatorListener) {
        this.lottieDrawable.addAnimatorListener(animatorListener);
    }

    public void addAnimatorUpdateListener(AnimatorUpdateListener animatorUpdateListener) {
        this.lottieDrawable.addAnimatorUpdateListener(animatorUpdateListener);
    }

    public void addColorFilter(@Nullable ColorFilter colorFilter) {
        this.lottieDrawable.addColorFilter(colorFilter);
    }

    public void addColorFilterToContent(String str, String str2, @Nullable ColorFilter colorFilter) {
        this.lottieDrawable.addColorFilterToContent(str, str2, colorFilter);
    }

    public void addColorFilterToLayer(String str, @Nullable ColorFilter colorFilter) {
        this.lottieDrawable.addColorFilterToLayer(str, colorFilter);
    }

    public void cancelAnimation() {
        this.lottieDrawable.cancelAnimation();
        enableOrDisableHardwareLayer();
    }

    public void clearColorFilters() {
        this.lottieDrawable.clearColorFilters();
    }

    public void enableMergePathsForKitKatAndAbove(boolean z) {
        this.lottieDrawable.enableMergePathsForKitKatAndAbove(z);
    }

    public long getDuration() {
        if (this.composition != null) {
            return this.composition.getDuration();
        }
        return 0L;
    }

    @Nullable
    public String getImageAssetsFolder() {
        return this.lottieDrawable.getImageAssetsFolder();
    }

    @FloatRange(from = 0.0f, to = 1.0f)
    public float getProgress() {
        return this.lottieDrawable.getProgress();
    }

    public float getScale() {
        return this.lottieDrawable.getScale();
    }

    public boolean hasMasks() {
        return this.lottieDrawable.hasMasks();
    }

    public boolean hasMatte() {
        return this.lottieDrawable.hasMatte();
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (getDrawable() == this.lottieDrawable) {
            super.invalidateDrawable(this.lottieDrawable);
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    public boolean isAnimating() {
        return this.lottieDrawable.isAnimating();
    }

    public void loop(boolean z) {
        this.lottieDrawable.loop(z);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.autoPlay && this.wasAnimatingWhenDetached) {
            playAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (isAnimating()) {
            cancelAnimation();
            this.wasAnimatingWhenDetached = true;
        }
        recycleBitmaps();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof LottieAnimationView.SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        LottieAnimationView.SavedState savedState = (LottieAnimationView.SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.animationName = savedState.animationName;
        if (!TextUtils.isEmpty(this.animationName)) {
            setAnimation(this.animationName);
        }
        setProgress(savedState.progress);
        loop(savedState.isLooping);
        if (savedState.isAnimating) {
            playAnimation();
        }
        this.lottieDrawable.setImagesAssetsFolder(savedState.imageAssetsFolder);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        LottieAnimationView.SavedState savedState = new LottieAnimationView.SavedState(super.onSaveInstanceState());
        savedState.animationName = this.animationName;
        savedState.progress = this.lottieDrawable.getProgress();
        savedState.isAnimating = this.lottieDrawable.isAnimating();
        savedState.isLooping = this.lottieDrawable.isLooping();
        savedState.imageAssetsFolder = this.lottieDrawable.getImageAssetsFolder();
        return savedState;
    }

    public void pauseAnimation() {
        float progress = getProgress();
        this.lottieDrawable.cancelAnimation();
        setProgress(progress);
        enableOrDisableHardwareLayer();
    }

    public void playAnimation() {
        this.lottieDrawable.playAnimation();
        enableOrDisableHardwareLayer();
    }

    @VisibleForTesting
    void recycleBitmaps() {
        if (this.lottieDrawable != null) {
            this.lottieDrawable.recycleBitmaps();
        }
    }

    public void removeAnimatorListener(AnimatorListener animatorListener) {
        this.lottieDrawable.removeAnimatorListener(animatorListener);
    }

    public void removeUpdateListener(AnimatorUpdateListener animatorUpdateListener) {
        this.lottieDrawable.removeAnimatorUpdateListener(animatorUpdateListener);
    }

    public void resumeAnimation() {
        this.lottieDrawable.resumeAnimation();
        enableOrDisableHardwareLayer();
    }

    public void resumeReverseAnimation() {
        this.lottieDrawable.resumeReverseAnimation();
        enableOrDisableHardwareLayer();
    }

    public void reverseAnimation() {
        this.lottieDrawable.reverseAnimation();
        enableOrDisableHardwareLayer();
    }

    public void setAnimation(String str) {
        setAnimation(str, this.defaultCacheStrategy);
    }

    public void setAnimation(final String str, final LottieAnimationView.CacheStrategy cacheStrategy) {
        this.animationName = str;
        if (weakRefCache.containsKey(str)) {
            WeakReference<LottieComposition> weakReference = weakRefCache.get(str);
            if (weakReference.get() != null) {
                setComposition(weakReference.get());
                return;
            }
        } else if (strongRefCache.containsKey(str)) {
            setComposition(strongRefCache.get(str));
            return;
        }
        this.animationName = str;
        this.lottieDrawable.cancelAnimation();
        cancelLoaderTask();
        this.compositionLoader = LottieComposition.Factory.fromAssetFileName(getContext(), str, new OnCompositionLoadedListener() {
            @Override
            public void onCompositionLoaded(LottieComposition lottieComposition) {
                if (cacheStrategy == LottieAnimationView.CacheStrategy.Strong) {
                    LottieAnimationView.strongRefCache.put(str, lottieComposition);
                } else if (cacheStrategy == LottieAnimationView.CacheStrategy.Weak) {
                    LottieAnimationView.weakRefCache.put(str, new WeakReference(lottieComposition));
                }
                LottieAnimationView.this.setComposition(lottieComposition);
            }
        });
    }

    public void setAnimation(JSONObject jSONObject) {
        cancelLoaderTask();
        this.compositionLoader = LottieComposition.Factory.fromJson(getResources(), jSONObject, this.loadedListener);
    }

    public void setComposition(@NonNull LottieComposition lottieComposition) {
        this.lottieDrawable.setCallback(this);
        if (this.lottieDrawable.setComposition(lottieComposition)) {
            int screenWidth = Utils.getScreenWidth(getContext());
            int screenHeight = Utils.getScreenHeight(getContext());
            int iWidth = lottieComposition.getBounds().width();
            int iHeight = lottieComposition.getBounds().height();
            if (iWidth > screenWidth || iHeight > screenHeight) {
                setScale(Math.min(Math.min(screenWidth / iWidth, screenHeight / iHeight), this.lottieDrawable.getScale()));
                Log.w("LOTTIE", String.format("Composition larger than the screen %dx%d vs %dx%d. Scaling down.", Integer.valueOf(iWidth), Integer.valueOf(iHeight), Integer.valueOf(screenWidth), Integer.valueOf(screenHeight)));
            }
            setImageDrawable(null);
            setImageDrawable(this.lottieDrawable);
            this.composition = lottieComposition;
            requestLayout();
        }
    }

    public void setImageAssetDelegate(ImageAssetDelegate imageAssetDelegate) {
        this.lottieDrawable.setImageAssetDelegate(imageAssetDelegate);
    }

    public void setImageAssetsFolder(String str) {
        this.lottieDrawable.setImagesAssetsFolder(str);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable != this.lottieDrawable) {
            recycleBitmaps();
        }
        super.setImageDrawable(drawable);
    }

    @Override
    public void setImageResource(int i) {
        super.setImageResource(i);
        recycleBitmaps();
    }

    public void setProgress(@FloatRange(from = 0.0f, to = 1.0f) float f) {
        this.lottieDrawable.setProgress(f);
    }

    public void setScale(float f) {
        this.lottieDrawable.setScale(f);
        if (getDrawable() == this.lottieDrawable) {
            setImageDrawable(null);
            setImageDrawable(this.lottieDrawable);
        }
    }

    public void setSpeed(float f) {
        this.lottieDrawable.setSpeed(f);
    }

    @Nullable
    public Bitmap updateBitmap(String str, @Nullable Bitmap bitmap) {
        return this.lottieDrawable.updateBitmap(str, bitmap);
    }

    public void useExperimentalHardwareAcceleration() {
        useExperimentalHardwareAcceleration(true);
    }

    public void useExperimentalHardwareAcceleration(boolean z) {
        this.useHardwareLayer = z;
        enableOrDisableHardwareLayer();
    }
}
