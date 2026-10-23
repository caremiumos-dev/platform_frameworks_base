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
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Open animation for the hall, drawn on top of a Lottie animation with a
 * circular reveal and a title.
 *
 * @hide
 */
public class HallOpenLottieView extends HallLottieView {
    private static final String COLOR_NAME = "color";
    private static final String CONFIG_FILE = "config.json";
    private static final String SIZE_NAME = "textSize";
    private static final String TAG = "HallOpenAnimationView";

    private final long BG_SHOW_TIME = 300L;
    private final int DEFAULT_RADIUS = 255;
    private final int MIN_RADIUS = 20;

    private int mAlpha = 0;
    private Paint mBgPaint;
    private boolean mCanRunAnimator;
    private Path mClipPath;
    private int mClipRadius = DEFAULT_RADIUS;
    private int mColor = -1;
    private boolean mDisableAnimator;
    private boolean mDispatchOnDraw;
    private ValueAnimator mFadeAnimator;
    private ValueAnimator.AnimatorUpdateListener mFadeUpdateListener;
    private Animator.AnimatorListener mListener;
    private OnDrawListener mOnDrawListener;
    private Runnable mRunEndAnimation;
    private boolean mRunning;
    private Paint mTextPaint;
    private float mTextSize = 17.0f;
    private String mTitle;
    private TextView mTitleText;
    private int mTranslate;
    private ValueAnimator.AnimatorUpdateListener mUpdateListener;

    public HallOpenLottieView(Context context) {
        this(context, null);
    }

    public HallOpenLottieView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HallOpenLottieView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mRunEndAnimation = new Runnable() {
            @Override
            public void run() {
                mRunning = false;
                if (mCanRunAnimator) {
                    if (mDisableAnimator) {
                        HallOpenLottieView.super.onLottieAnimationEnd(null);
                    } else {
                        mFadeAnimator.start();
                    }
                    Log.d(TAG, "start end animator after delay end");
                }
            }
        };
        this.mFadeUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int value = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                if (value >= 0) {
                    if (value >= MIN_RADIUS) {
                        mClipRadius = value;
                    }
                    if (value < 242) {
                        mAlpha = 255 - value;
                    }
                } else {
                    mTranslate = -value;
                }
                invalidate();
            }
        };
        this.mListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animator) {
                mRunning = false;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mRunning = false;
                HallOpenLottieView.super.onLottieAnimationEnd(animator);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }
        };
        this.mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (animationView.getProgress() > 0.9f) {
                    mTitleText.setAlpha(animationView.getProgress());
                }
            }
        };
        this.animationView.addAnimatorUpdateListener(this.mUpdateListener);
        setBackgroundColor(0);
        this.mTextPaint = new Paint();
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setStyle(Paint.Style.FILL);
        this.mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        this.mClipPath = new Path();
        this.mBgPaint = new Paint();
        this.mBgPaint.setAntiAlias(true);
        this.mBgPaint.setColor(0xff000000);
        this.mFadeAnimator = ValueAnimator.ofInt(255, 0, -255);
        this.mFadeAnimator.setDuration(600L);
        this.mFadeAnimator.setInterpolator(new AccelerateInterpolator());
        this.mFadeAnimator.addListener(this.mListener);
        this.mFadeAnimator.addUpdateListener(this.mFadeUpdateListener);
        initTitleText();
    }

    private void clipCirclePath(Canvas canvas) {
        this.mClipPath.reset();
        this.mClipPath.addCircle(getWidth() / 2, getHeight() / 2, this.mClipRadius, Path.Direction.CW);
        canvas.clipPath(this.mClipPath);
    }

    private void drawBackgroundColor(Canvas canvas) {
        if (this.mTranslate > 0) {
            this.mBgPaint.setAlpha(((255 - this.mTranslate) * 7) / 10);
        } else {
            this.mBgPaint.setAlpha(178);
        }
        canvas.drawRect(0.0f, 0.0f, getWidth(), getHeight(), this.mBgPaint);
    }

    private void drawCoverCircle(Canvas canvas) {
        if (this.mAlpha > 0) {
            this.mTextPaint.setColor(0xFF151026);
            this.mTextPaint.setAlpha(this.mAlpha);
            this.mTitleText.setAlpha(1.0f - (this.mAlpha / 255.0f));
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, this.mClipRadius + 4, this.mTextPaint);
        }
    }

    private void getConfigFromJson() {
        try {
            JSONObject object = new JSONObject(readFile(new File(this.animPath, CONFIG_FILE)));
            this.mColor = object.getInt(COLOR_NAME);
            this.mTextSize = object.getInt(SIZE_NAME);
        } catch (JSONException e) {
            Log.w(TAG, "error get json", e);
        }
    }

    private void initTitleText() {
        this.mTitleText = new TextView(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -2);
        params.leftMargin = 36;
        params.rightMargin = 36;
        params.gravity = 17;
        this.mTitleText.setLayoutParams(params);
        this.mTitleText.setGravity(17);
        this.mTitleText.setAlpha(0.0f);
        addView(this.mTitleText);
    }

    private void setTextColor(int color) {
        this.mTitleText.setTextColor(color);
    }

    private void setTextSize(float size) {
        this.mTitleText.setTextSize(2, size);
    }

    private void setTextString(String text) {
        this.mTitleText.setText(text);
    }

    @Override
    public void cancelAnimation() {
        this.mTitleText.setAlpha(0.0f);
        removeCallbacks(this.mRunEndAnimation);
        this.mFadeAnimator.cancel();
        super.cancelAnimation();
    }

    @Override
    public void disableAnimator(boolean disable) {
        this.mDisableAnimator = disable;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!this.mDispatchOnDraw || this.mOnDrawListener == null) {
            return;
        }
        this.mDispatchOnDraw = false;
        this.mOnDrawListener.onFirstDraw();
    }

    @Override
    public boolean isAnimatorRunning() {
        return this.mRunning;
    }

    @Override
    public void loadAnimation(String path) {
        super.loadAnimation(path);
        getConfigFromJson();
        setTextColor(this.mColor);
        setTextSize(this.mTextSize);
    }

    @Override
    protected void onAttachedToWindow() {
        this.mDispatchOnDraw = true;
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mTitleText.setAlpha(0.0f);
        this.mDispatchOnDraw = false;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!this.mDisableAnimator) {
            drawBackgroundColor(canvas);
            canvas.translate(this.mTranslate, 0.0f);
            clipCirclePath(canvas);
        }
        if (this.mDisableAnimator) {
            return;
        }
        drawCoverCircle(canvas);
    }

    @Override
    protected void onLottieAnimationEnd(Animator animator) {
        postDelayed(this.mRunEndAnimation, BG_SHOW_TIME);
    }

    @Override
    public void playAnimation() {
        this.mClipRadius = DEFAULT_RADIUS;
        this.mTranslate = 0;
        this.mAlpha = 0;
        this.mCanRunAnimator = false;
        this.mFadeAnimator.cancel();
        this.mRunning = true;
        super.playAnimation();
    }

    @Override
    public void requestHideAnimation() {
        this.mCanRunAnimator = true;
        if (this.mRunning) {
            return;
        }
        if (this.mDisableAnimator) {
            super.onLottieAnimationEnd(null);
        } else {
            this.mFadeAnimator.start();
        }
    }

    @Override
    public void setAnimation() {
        JSONObject animation = getAnimation(true);
        if (animation != null) {
            this.animationView.setAnimation(animation);
        }
    }

    @Override
    public void setOnDrawListener(OnDrawListener listener) {
        this.mOnDrawListener = listener;
    }

    @Override
    public void setTitle(String title) {
        if (title == null || title.length() == 0) {
            Log.w(TAG, "title can not be null");
        } else {
            if (title.equals(this.mTitle)) {
                return;
            }
            this.mTitle = title;
            setTextString(title);
        }
    }
}