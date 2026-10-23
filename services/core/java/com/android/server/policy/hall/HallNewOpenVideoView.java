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
import android.graphics.Canvas;
import android.util.AttributeSet;

/**
 * A simpler open video view that removes itself only after a hide request.
 *
 * @hide
 */
public class HallNewOpenVideoView extends HallVideoView {
    private boolean mCanRemove;
    private boolean mDispatchOnDraw;
    private OnDrawListener mOnDrawListener;

    public HallNewOpenVideoView(Context context) {
        this(context, null);
    }

    public HallNewOpenVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HallNewOpenVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void disableAnimator(boolean disable) {
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
    public void loadAnimation(String path) {
        super.loadAnimation(path);
    }

    @Override
    protected void onAnimationEnd(Animator animator) {
        if (this.mCanRemove) {
            super.onAnimationEnd(animator);
            this.mCanRemove = false;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        this.mDispatchOnDraw = true;
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mDispatchOnDraw = false;
        super.onDetachedFromWindow();
    }

    @Override
    public void playAnimation() {
        this.mCanRemove = false;
        super.playAnimation();
    }

    @Override
    public void requestHideAnimation() {
        if (isAnimatorRunning()) {
            this.mCanRemove = true;
        } else {
            super.onAnimationEnd(null);
        }
    }

    @Override
    public void setAnimation() {
        setVideoData(true);
    }

    @Override
    public void setOnDrawListener(OnDrawListener listener) {
        this.mOnDrawListener = listener;
    }

    @Override
    public void setTitle(String title) {
    }
}