/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.policy.hall;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;

import com.airbnb.lottie.ImageAssetDelegate;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieImageAsset;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Hall animation view backed by a Lottie animation, used on devices that ship
 * a lottie close animation.
 *
 * @hide
 */
public class HallLottieView extends HallAnimationView {
    private static final String TAG = HallLottieView.class.getSimpleName();

    private final String IMAGE_FOLDER = "images/";
    private final String OPEN_JSON = "open.json";
    private final String CLOSE_JSON = "close.json";

    LottieAnimationView animationView;
    private ImageAssetDelegate assetDelegate;
    String imagePath;
    private Animator.AnimatorListener mListener;
    Animator.AnimatorListener notifyListener;

    public HallLottieView(Context context) {
        this(context, null);
    }

    public HallLottieView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HallLottieView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animator) {
                if (notifyListener != null) {
                    notifyListener.onAnimationCancel(animator);
                }
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                onLottieAnimationEnd(animator);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
                if (notifyListener != null) {
                    notifyListener.onAnimationStart(animator);
                }
            }
        };
        this.assetDelegate = new ImageAssetDelegate() {
            @Override
            public Bitmap fetchBitmap(LottieImageAsset asset) {
                return getBitmapFromFile(asset.getFileName());
            }
        };
        this.animationView = new LottieAnimationView(context);
        this.animationView.addAnimatorListener(this.mListener);
        this.animationView.setImageAssetDelegate(this.assetDelegate);
        addView(this.animationView);
        setBackgroundColor(0xff000000);
    }

    private Bitmap getBitmapFromFile(String name) {
        long start = SystemClock.uptimeMillis();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = true;
        options.inDensity = 160;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        File file = new File(this.imagePath, name);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
        if (bitmap == null) {
            Log.w(TAG, "decode bitmap failed:" + file.getPath());
        }
        long cost = SystemClock.uptimeMillis() - start;
        if (cost > 10) {
            Log.w(TAG, "load bitmap slow cost:" + cost + " name:" + name);
        }
        return bitmap;
    }

    @Override
    public void cancelAnimation() {
        this.animationView.cancelAnimation();
    }

    @Override
    public void disableAnimator(boolean disable) {
    }

    JSONObject getAnimation(boolean open) {
        try {
            return new JSONObject(readFile(new File(this.animPath, open ? OPEN_JSON : CLOSE_JSON)));
        } catch (JSONException e) {
            Log.w(TAG, "error get json", e);
            return null;
        }
    }

    @Override
    public long getDuration() {
        return this.animationView.getDuration();
    }

    @Override
    public boolean isAnimatorRunning() {
        return this.animationView.isAnimating();
    }

    @Override
    public void loadAnimation(String path) {
        this.animPath = path;
        this.imagePath = new File(this.animPath, IMAGE_FOLDER).getPath();
        setAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    protected void onLottieAnimationEnd(Animator animator) {
        if (this.notifyListener != null) {
            this.notifyListener.onAnimationEnd(animator);
        }
    }

    @Override
    public void playAnimation() {
        this.animationView.cancelAnimation();
        this.animationView.setProgress(0.0f);
        this.animationView.playAnimation();
    }

    @Override
    public void requestHideAnimation() {
    }

    @Override
    public void setAnimation() {
        JSONObject animation = getAnimation(false);
        if (animation != null) {
            this.animationView.setAnimation(animation);
        }
    }

    @Override
    public void setAnimatorListener(Animator.AnimatorListener listener) {
        this.notifyListener = listener;
    }

    @Override
    public void setOnDrawListener(OnDrawListener listener) {
    }

    @Override
    public void setTitle(String title) {
    }
}